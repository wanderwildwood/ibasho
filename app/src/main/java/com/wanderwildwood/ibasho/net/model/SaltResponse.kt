package com.wanderwildwood.ibasho.net.model

import androidx.annotation.Keep

@Keep
data class SaltResponse(
    val salt64: String,
    val protoVersion: Int,
)
