package com.wanderwildwood.ibasho.ui.paging

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.ScrollView
import kotlin.math.abs

/**
 * A screen that turns pages instead of scrolling.
 *
 * Taken from kotozute, where it was written for the same panel and for the same reason MMD's
 * own lists page: a screen that redraws in full renders inertia as a smear, and spends the
 * battery drawing it. These screens are a `ScrollView` holding one long column of rows, so
 * there is no adapter to ask how tall anything is. Instead: intercept the drag before the
 * ScrollView starts scrolling with it, move exactly one screen, and swallow whatever is left
 * of the gesture. One swipe is one page however far the finger keeps going.
 *
 * Taps are untouched. A drag is only claimed once it passes the touch slop *and* is plainly
 * more vertical than horizontal, so a row's own click still fires.
 */
class PagingScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private var downX = 0f
    private var downY = 0f
    private var turned = false

    /**
     * Blank space past the end of the column, so the last page can begin on a row edge.
     *
     * Without it the last page is the one place a page cannot land on a row edge: the scroll
     * stops where the content runs out, which can leave a row cut through the middle under the
     * toolbar. It must be at least the tallest row, and well short of a page.
     *
     * Two differences from kotozute's. It sits on the column rather than on this view's own
     * padding, because every screen here hands this view to the edge-to-edge helper, which
     * sets the bottom padding to the system bar's height and would wipe the slack out. And it
     * is only added where this view fills the screen: a dialog sizes itself to what it holds,
     * and slack there would simply be a dialog 144dp too tall.
     */
    private var slack = 0

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (slack == 0 && layoutParams?.height == ViewGroup.LayoutParams.MATCH_PARENT) {
            val column = getChildAt(0) ?: return
            slack = (SLACK_DP * resources.displayMetrics.density).toInt()
            column.setPadding(
                column.paddingLeft, column.paddingTop, column.paddingRight,
                column.paddingBottom + slack
            )
        }
    }

    /** Letting go stops the page rather than throwing the column across several screens. */
    override fun fling(velocityY: Int) = Unit

    /**
     * The gesture, wherever it arrives: over a row it reaches [onInterceptTouchEvent], over a
     * gap between rows it goes straight to [onTouchEvent]. Both have to turn the page, or a
     * swipe over a row pages and a swipe two pixels lower scrolls.
     *
     * @return true once the page has turned and the rest of the drag should be swallowed.
     */
    private fun track(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                turned = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (turned) return true
                val dy = ev.y - downY
                val dx = ev.x - downX
                if (abs(dy) > slop && abs(dy) > abs(dx)) {
                    turned = true
                    turnPage(forward = dy < 0)
                    return true
                }
            }
        }
        return turned
    }

    // Never intercepts the DOWN, so a row still gets its tap.
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = track(ev)

    // Never passed to super: the base class is what scrolls continuously.
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        track(ev)
        return true
    }

    /** One screen on, or one back, landing on a row's edge. */
    private fun turnPage(forward: Boolean) {
        val page = height - paddingTop - paddingBottom
        val column = getChildAt(0) ?: return
        val (container, offset) = rowContainer() ?: return

        val rows = (0 until container.childCount)
            .map { container.getChildAt(it) }
            .filter { it.visibility != GONE }
            .map { PageTurn.Row(top = offset + it.top, height = it.height) }

        val target = PageTurn.target(
            current = scrollY,
            page = page,
            maxScroll = (column.height - page).coerceAtLeast(0),
            contentHeight = column.height - slack,
            rows = rows,
            forward = forward
        ) ?: return

        scrollTo(0, target)
    }

    /**
     * The view whose children are the rows, and its top in this view's coordinates: descend
     * while there is exactly one visible child that could hold rows, so a screen wrapped in
     * an extra layout still pages by its rows and not by the wrapper.
     */
    private fun rowContainer(): Pair<ViewGroup, Int>? {
        var container = getChildAt(0) as? ViewGroup ?: return null
        var offset = container.top
        while (true) {
            val visible = (0 until container.childCount)
                .map { container.getChildAt(it) }
                .filter { it.visibility != GONE }
            val only = visible.singleOrNull() as? ViewGroup ?: return container to offset
            if (only.childCount == 0) return container to offset
            offset += only.top
            container = only
        }
    }

    private companion object {
        /** See [slack]: must clear the tallest row, and stay well short of a page. */
        const val SLACK_DP = 144f
    }
}
