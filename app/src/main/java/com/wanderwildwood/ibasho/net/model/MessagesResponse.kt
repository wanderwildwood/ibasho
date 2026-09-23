package com.wanderwildwood.ibasho.net.model

import androidx.annotation.Keep

@Keep
data class MessagesResponse(
    val messages: List<ServerMessage>,
)
