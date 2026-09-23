package com.wanderwildwood.ibasho.net.model

data class EncryptedItem(
    val clientItemIdHex: String,
    val unixMillis: Long,
    val ciphertext64: String,
)
