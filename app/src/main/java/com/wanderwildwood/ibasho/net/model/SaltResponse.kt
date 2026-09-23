package com.wanderwildwood.ibasho.net.model

data class SaltResponse(
    val salt64: String,
    val protoVersion: Int,
)
