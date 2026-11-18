package com.wanderwildwood.ibasho.transports

import android.content.Context
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.commands.PasswordPause
import com.wanderwildwood.ibasho.commands.WrongPasswordRules
import com.wanderwildwood.ibasho.utils.Notifications

/** Tells the owner when wrong passwords have just paused one sender, or a whole channel. */
fun notifyPauseStarted(context: Context, started: PasswordPause, channelName: String) {
    val (title, text) = when (started) {
        PasswordPause.NONE -> return
        PasswordPause.SENDER -> Pair(
            context.getString(R.string.wrong_passwords_sender_paused_title),
            context.getString(
                R.string.wrong_passwords_sender_paused_text,
                WrongPasswordRules.MAX_WRONG,
                channelName,
            ),
        )
        PasswordPause.CHANNEL -> Pair(
            context.getString(R.string.wrong_passwords_paused_title),
            context.getString(
                R.string.wrong_passwords_paused_text,
                WrongPasswordRules.MAX_WRONG_PER_HOUR,
                channelName,
            ),
        )
    }
    Notifications.notify(context, title, text, Notifications.CHANNEL_SECURITY)
}
