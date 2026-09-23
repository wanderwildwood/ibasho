package com.wanderwildwood.ibasho.net.model

import androidx.annotation.Keep

@Keep
data class EncryptedItem(
    val clientItemIdHex: String,
    val unixMillis: Long,
    val ciphertext64: String,
)
