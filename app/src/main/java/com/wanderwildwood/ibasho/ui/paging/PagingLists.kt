package com.wanderwildwood.ibasho.ui.paging

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ListView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/**
 * A swipe moves the list one screen, and then stops.
 *
 * Taken from kotozute. Mudita's own lists do not scroll continuously on this panel: MMD takes
 * the scrolling away from the list and steps it, because a screen that redraws in full renders
 * inertia as a smear. These lists are RecyclerViews and a ListView that MMD's component cannot
 * replace, so they take the behaviour instead.
 *
 * **The last line of the old page becomes the first line of the new one.** After the jump,
 * whichever row the page landed part-way through is pulled fully into view: a top edge that is
 * always a row's edge, and one line of overlap to read on from.
 *
 * Only plainly vertical gestures are claimed. Once a page has turned the rest of the drag is
 * swallowed: one swipe is one page, however far the finger keeps travelling.
 */
fun RecyclerView.turnsAPageOnSwipe() {
    // Letting go stops the list rather than throwing it across several screens.
    onFlingListener = object : RecyclerView.OnFlingListener() {
        override fun onFling(velocityX: Int, velocityY: Int) = true
    }

    val slop = ViewConfiguration.get(context).scaledTouchSlop
    addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var turned = false

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.x
                    downY = e.y
                    turned = false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (turned) return true
                    val dy = e.y - downY
                    val dx = e.x - downX
                    if (abs(dy) > slop && abs(dy) > abs(dx)) {
                        turned = true
                        rv.turnPage(forward = dy < 0)
                        return true
                    }
                }
            }
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) = Unit
        override fun onRequestDisallowInterceptTouchEvent(disallow: Boolean) = Unit
    })
}

private fun RecyclerView.turnPage(forward: Boolean) {
    val page = height - paddingTop - paddingBottom
    if (page <= 0) return
    scrollBy(0, if (forward) page else -page)
    alignToRow(
        canGoFurther = canScrollVertically(if (forward) 1 else -1),
        firstTop = getChildAt(0)?.top,
        firstHeight = getChildAt(0)?.height,
        page = page,
        edge = if (clipToPadding) paddingTop else 0,
    ) { dy -> scrollBy(0, dy) }
}

/**
 * The same for a ListView, which has no item touch listener to hang it off: a touch listener
 * sees each event before the list does, and a consumed MOVE is one the list never scrolls by.
 * The DOWN always passes through, so a row's press and click are untouched.
 */
@SuppressLint("ClickableViewAccessibility")
fun ListView.turnsAPageOnSwipe() {
    val slop = ViewConfiguration.get(context).scaledTouchSlop
    var downX = 0f
    var downY = 0f
    var turned = false

    setOnTouchListener { _, e ->
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                downY = e.y
                turned = false
                false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!turned) {
                    val dy = e.y - downY
                    val dx = e.x - downX
                    if (abs(dy) > slop && abs(dy) > abs(dx)) {
                        turned = true
                        turnPage(forward = dy < 0)
                    }
                }
                // Every move is the list's own scroll otherwise, turned or not yet.
                true
            }
            // After a turn, the lift must not also count as a click on whatever row is under
            // the finger now.
            MotionEvent.ACTION_UP -> turned
            else -> false
        }
    }
}

private fun ListView.turnPage(forward: Boolean) {
    val page = height - paddingTop - paddingBottom
    if (page <= 0) return
    scrollListBy(if (forward) page else -page)
    alignToRow(
        canGoFurther = canScrollList(if (forward) 1 else -1),
        firstTop = getChildAt(0)?.top,
        firstHeight = getChildAt(0)?.height,
        page = page,
        edge = if (clipToPadding) paddingTop else 0,
    ) { dy -> scrollListBy(dy) }
}

/**
 * Pull the row the page landed part-way through fully into view, except where that would cost
 * more than it buys: at the end of the list, where the pull would push the last rows out of
 * reach, and on a row taller than the page, which is the one that takes two pages.
 */
private inline fun alignToRow(
    canGoFurther: Boolean,
    firstTop: Int?,
    firstHeight: Int?,
    page: Int,
    edge: Int,
    scroll: (Int) -> Unit,
) {
    if (!canGoFurther) return
    if (firstTop == null || firstHeight == null) return
    if (firstHeight > page) return
    scroll(firstTop - edge)
}
