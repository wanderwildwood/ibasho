package com.wanderwildwood.ibasho.net.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

// Family sharing on the server (ibasho-server backend/family.go). See docs/PLAN-family.md.

@Keep
data class IdentityBody(
    @SerializedName("publicKey64") val publicKey64: String,
    @SerializedName("encPrivateKey64") val encPrivateKey64: String,
)

@Keep
data class FamilyMe(
    @SerializedName("username") val username: String,
    @SerializedName("paused") val paused: Boolean,
)

@Keep
data class FamilyPersonBody(
    @SerializedName("username") val username: String,
    @SerializedName("publicKey64") val publicKey64: String?,
    @SerializedName("paused") val paused: Boolean,
)

@Keep
data class FamilyResponse(
    @SerializedName("me") val me: FamilyMe,
    @SerializedName("people") val people: List<FamilyPersonBody>?,
)

@Keep
data class PauseBody(
    @SerializedName("paused") val paused: Boolean,
)

@Keep
data class SealedPut(
    @SerializedName("viewer") val viewer: String,
    @SerializedName("unixMillis") val unixMillis: Long,
    @SerializedName("ciphertext64") val ciphertext64: String,
)

@Keep
data class SealedPutRequest(
    @SerializedName("items") val items: List<SealedPut>,
    /** Everyone this phone shares with now; the server drops copies for anyone else. */
    @SerializedName("audience") val audience: List<String>,
)

@Keep
data class SealedGet(
    @SerializedName("owner") val owner: String,
    @SerializedName("unixMillis") val unixMillis: Long,
    @SerializedName("ciphertext64") val ciphertext64: String,
)

@Keep
data class SealedGetResponse(
    @SerializedName("items") val items: List<SealedGet>?,
)
