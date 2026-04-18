package de.nulide.findmydevice.data

const val MIME_JPEG = "image/jpeg"

data class FmdPicture(
    val raw: ByteArray,
    val mimeType: String,
)
