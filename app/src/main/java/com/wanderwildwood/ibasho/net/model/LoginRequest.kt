package com.wanderwildwood.ibasho.net.model

data class LoginRequest (
    val userName: String,
    val passwordHash64: String,
    val sessionDurationSeconds: Int,
)
