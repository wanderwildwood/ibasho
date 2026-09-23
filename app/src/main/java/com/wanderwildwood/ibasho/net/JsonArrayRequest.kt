package com.wanderwildwood.ibasho.net

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Response
import com.android.volley.Response.ErrorListener
import com.android.volley.Response.Listener
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.JsonRequest
import org.json.JSONArray
import org.json.JSONObject


/**
 * Same as a [JsonObjectRequest], but produces a JSONArray as a response.
 */
class JsonArrayRequest(
    method: Int,
    url: String,
    jsonRequest: JSONObject,
    listener: Listener<JSONArray>,
    errorListener: ErrorListener,
) : JsonRequest<JSONArray>(
    method, url, jsonRequest.toString(), listener, errorListener
) {

    override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONArray> {
        return try {
            val bytes = response?.data ?: ByteArray(0)
            val charsetName = HttpHeaderParser.parseCharset(response?.headers, PROTOCOL_CHARSET)
            val jsonString = String(bytes, charset(charsetName))
            val parsed = JSONArray(jsonString)
            return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: Exception) {
            Response.error(ParseError(e))
        }
    }

    override fun getHeaders(): Map<String, String> {
        return mapOf<String, String>(
            Pair(HEADER_USER_AGENT, FMD_USER_AGENT)
        )
    }
}
