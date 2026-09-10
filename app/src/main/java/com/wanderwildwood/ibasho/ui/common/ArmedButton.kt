package com.wanderwildwood.ibasho.ui.common

import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.annotation.StringRes

/** How long an armed control stays live before it forgets it was tapped. */
private const val DISARM_AFTER_MS = 4_000L

/**
 * A destructive action asks in its own face rather than stacking a dialog on
 * top of the screen that already holds it. The first tap arms the button and
 * changes what it says; a second tap does the thing.
 *
 * It disarms itself after four seconds, so a stray tap does not leave a live
 * trigger for whoever picks the phone up next.
 */
fun armThenRun(
    button: Button,
    @StringRes idleText: Int,
    @StringRes armedText: Int,
    action: Runnable,
) {
    val handler = Handler(Looper.getMainLooper())
    var armed = false

    val disarm = Runnable {
        armed = false
        button.setText(idleText)
    }

    button.setText(idleText)
    button.setOnClickListener {
        if (armed) {
            handler.removeCallbacks(disarm)
            armed = false
            button.setText(idleText)
            action.run()
        } else {
            armed = true
            button.setText(armedText)
            handler.postDelayed(disarm, DISARM_AFTER_MS)
        }
    }
}
