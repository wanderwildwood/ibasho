package com.wanderwildwood.ibasho.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.commands.Command
import com.wanderwildwood.ibasho.commands.availableCommands
import com.wanderwildwood.ibasho.utils.CypherUtils

class PasswordSetDialog @JvmOverloads constructor(
    val context: Context,
    val onSuccess: (String) -> Unit,
    @StringRes val title: Int = R.string.password_enter,
    val message: String? = null,
    val forceMinLength: Boolean = true,
    val allowEmpty: Boolean = false,
    val onCancel: () -> Unit = {},
) {

    var dialog: AlertDialog

    init {
        val passwordLayout: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_password_set, null)
        val editTextPassword = passwordLayout.findViewById<EditText>(R.id.editTextPassword)

        if (!message.isNullOrBlank()) {
            val textView = passwordLayout.findViewById<TextView>(R.id.messagePassword)
            textView.text = message
        }

        dialog = EInkAlertDialogBuilder(context)
            .setTitle(title)
            .setView(passwordLayout)
            .setPositiveButton(R.string.Ok, null)
            .setNegativeButton(context.getString(R.string.cancel), { _, _ -> onCancel() })
            .setCancelable(false)
            .create()

        // A wrong entry keeps the dialog open, with what was typed and the reason in the dialog.
        dialog.closeOnlyWhen {
            val password = editTextPassword.getText().toString()
            val problem = passwordProblem(context, password, forceMinLength, allowEmpty)
            passwordLayout.showDialogError(problem)
            if (problem == null) onSuccess(password)
            problem == null
        }
    }

    fun show() {
        dialog.show();
    }
}

/** Why [password] cannot be used (a string resource), or null when it can. */
@StringRes
fun passwordProblem(
    context: Context,
    password: String,
    forceMinLength: Boolean,
    allowEmpty: Boolean,
): Int? {
    return if (password.isBlank()) {
        if (allowEmpty) null else R.string.pw_change_empty
    } else if (availableCommands(context).stream()
            .anyMatch { cmd: Command? -> cmd!!.keyword == password }
    ) {
        R.string.password_match_command_keyword
    } else if (forceMinLength && password.length < CypherUtils.MIN_PASSWORD_LENGTH) {
        R.string.password_error_min_length
    } else {
        null
    }
}
