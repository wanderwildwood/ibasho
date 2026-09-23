package com.wanderwildwood.ibasho.crypto

sealed class DataBlobType(val label: String) {
    object Command : DataBlobType("command")
    object Location : DataBlobType("location")
    object Picture : DataBlobType("picture")

    companion object {
        fun fromLabel(label: String): DataBlobType? {
            return when (label) {
                "command" -> Command
                "location" -> Location
                "picture" -> Picture
                else -> null
            }
        }

        fun all(): List<DataBlobType> {
            return listOf(Location, Picture, Command)
        }
    }
}

data class EncryptedDataBlob(
    val uniqueId: ByteArray,
    val unixMillis: Long,
    val type: String,
    val ciphertext: ByteArray,
)
