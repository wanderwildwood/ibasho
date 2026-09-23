package com.wanderwildwood.ibasho.net.model

import androidx.annotation.Keep

@Keep
data class RegisterRequest(
    val username: String,
    val salt64: String,
    val passwordHash64: String,
    val protoVersion: Int,
    val encMasterKey64: String,
    val registrationToken: String,
)
