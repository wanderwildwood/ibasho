package com.wanderwildwood.ibasho.net.model

import androidx.annotation.Keep

@Keep
data class LoginResponse(
    val accessToken: String,
    val encMasterKey64: String,
)
