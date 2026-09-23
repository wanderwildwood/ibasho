package com.wanderwildwood.ibasho.net.model

data class LoginResponse(
    val accessToken: String,
    val encMasterKey64: String,
)
