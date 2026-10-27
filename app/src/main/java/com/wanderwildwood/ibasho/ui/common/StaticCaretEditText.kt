package com.wanderwildwood.ibasho.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import com.google.android.material.theme.MaterialComponentsViewInflater

/**
 * A text field whose caret does not blink.
 *
 * The platform blinks the caret from a timer, twice a second, for as long as a field has
 * focus, and nothing but hiding it stops that. On an e-ink panel each blink is a repaint.
 * So the platform's caret is switched off and the same caret is drawn here, as a steady
 * ink bar at the cursor.
 */
class StaticCaretEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle,
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private val caret = Paint().apply { color = Color.BLACK }
    private val caretWidth = 2f * resources.displayMetrics.density

    init {
        isCursorVisible = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = layout ?: return
        if (!isFocused || selectionStart < 0 || selectionStart != selectionEnd) return
        val offset = selectionStart
        val line = layout.getLineForOffset(offset)
        val x = totalPaddingLeft + layout.getPrimaryHorizontal(offset)
        val top = extendedPaddingTop + layout.getLineTop(line)
        val bottom = extendedPaddingTop + layout.getLineBottom(line)
        canvas.drawRect(x, top.toFloat(), x + caretWidth, bottom.toFloat(), caret)
    }

    // Hand the platform's caret control back to nothing: leaving it hidden is the point.
    override fun setCursorVisible(visible: Boolean) {
        super.setCursorVisible(false)
    }
}

/**
 * Inflates every plain EditText of the XML layouts as a [StaticCaretEditText], so no layout
 * has to be edited one by one.
 */
class IbashoViewInflater : MaterialComponentsViewInflater() {
    override fun createEditText(context: Context, attrs: AttributeSet?): AppCompatEditText {
        return StaticCaretEditText(context, attrs)
    }
}
