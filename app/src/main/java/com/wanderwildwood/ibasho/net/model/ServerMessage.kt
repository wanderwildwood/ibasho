package com.wanderwildwood.ibasho.net.model

import androidx.annotation.Keep

@Keep
data class ServerMessage(
    val uuid: String,
    val unixMillis: Long,
    val code: Int,
    val text: String,
)

const val MSG_OTHER = 1
const val MSG_ACCOUNT_LOCKED = 2
