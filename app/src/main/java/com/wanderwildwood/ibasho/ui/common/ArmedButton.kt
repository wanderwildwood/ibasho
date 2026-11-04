package com.wanderwildwood.ibasho.ui.common

import android.content.Context
import android.content.ContextWrapper
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes

/**
 * A destructive action asks in its own face rather than stacking a dialog on
 * top of the screen that already holds it. The first tap arms the button and
 * changes what it says; a second tap does the thing.
 *
 * It stays armed, with no timer, until the person does something else: a tap or
 * a drag anywhere else, Back (which only disarms), or leaving the screen. Arming
 * one button disarms any other. The rule lives in [ArmedWatch].
 */
fun armThenRun(
    button: Button,
    @StringRes idleText: Int,
    @StringRes armedText: Int,
    action: Runnable,
) {
    val watch = ArmedWatch.of(button.context.componentActivity())
    val disarmRow: () -> Unit = { button.setText(idleText) }

    button.setText(idleText)
    button.setOnClickListener {
        if (watch.isArmed(disarmRow)) {
            watch.done(disarmRow)
            button.setText(idleText)
            action.run()
        } else {
            button.setText(armedText)
            watch.arm(disarmRow)
        }
    }
}

private tailrec fun Context.componentActivity(): ComponentActivity = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.componentActivity()
    else -> error("armThenRun needs a button inside a ComponentActivity")
}
