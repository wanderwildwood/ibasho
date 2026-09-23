package com.wanderwildwood.ibasho.net.model

import androidx.annotation.Keep

@Keep
data class DataRequestResponse(
    val items: List<EncryptedItem>,
)
