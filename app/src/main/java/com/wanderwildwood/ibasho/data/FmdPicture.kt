package com.wanderwildwood.ibasho.data

import com.wanderwildwood.ibasho.utils.encodeBase64
import org.json.JSONException
import org.json.JSONObject

const val MIME_JPEG = "image/jpeg"

data class FmdPicture(
    val raw: ByteArray,
    val mimeType: String,
) {
    fun encodeToJson(): String {
        val obj = JSONObject()
        try {
            obj.put("raw64", this.raw.encodeBase64())
            obj.put("mimeType", this.mimeType)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return obj.toString()
    }
}
