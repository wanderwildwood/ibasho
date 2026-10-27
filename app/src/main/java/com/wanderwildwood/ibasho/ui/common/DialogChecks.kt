package com.wanderwildwood.ibasho.ui.common

import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.wanderwildwood.ibasho.R

/**
 * Makes the positive button run [check] and close the dialog only when it returns true.
 * A failed check leaves the dialog open with what was typed still in it; [check] says why with
 * [showDialogError]. (A plain positive-button listener always dismisses, so the typing was lost
 * and the reason went by in a Toast that was gone before it could be read.)
 */
fun AlertDialog.closeOnlyWhen(check: () -> Boolean) {
    setOnShowListener {
        getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (check()) dismiss()
        }
    }
}

/** Writes [message] in the dialog's error line (R.id.dialogError), or hides it when null. */
fun View.showDialogError(@StringRes message: Int?) {
    val line = findViewById<TextView>(R.id.dialogError) ?: return
    if (message == null) {
        line.visibility = View.GONE
    } else {
        line.setText(message)
        line.visibility = View.VISIBLE
    }
}
