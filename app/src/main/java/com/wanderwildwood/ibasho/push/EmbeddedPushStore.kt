package com.wanderwildwood.ibasho.push

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.wanderwildwood.ibasho.BuildConfig
import java.util.UUID

/**
 * What the built-in push distributor remembers: the id Mozilla's push server knows this
 * phone by, and one channel per registration the connector has made with it.
 *
 * A registration is keyed by the connector's token. Each gets a channel id of its own,
 * which is what the push server knows it by, and the endpoint the server gave it once
 * the server has answered.
 */
class EmbeddedPushStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Channel(val token: String, val channelId: String, val vapid: String?, val endpoint: String?)

    /** The push server's id for this phone. When it changes, every endpoint is void. */
    var uaid: String?
        get() = prefs.getString(KEY_UAID, null)
        set(value) = prefs.edit { if (value == null) remove(KEY_UAID) else putString(KEY_UAID, value) }

    /**
     * Stands in for a server account in a debug build, so the push path can be tested
     * without one. A release build never honours it, even left over from a debug install.
     */
    var debugWithoutAccount: Boolean
        get() = BuildConfig.DEBUG && prefs.getBoolean(KEY_DEBUG_NO_ACCOUNT, false)
        set(value) = prefs.edit { putBoolean(KEY_DEBUG_NO_ACCOUNT, value) }

    fun channels(): List<Channel> = prefs.all.keys
        .filter { it.startsWith(PREFIX_CHANNEL) }
        .mapNotNull { key -> channelForToken(key.removePrefix(PREFIX_CHANNEL)) }

    fun hasChannels(): Boolean = prefs.all.keys.any { it.startsWith(PREFIX_CHANNEL) }

    fun channelForToken(token: String): Channel? {
        val channelId = prefs.getString(PREFIX_CHANNEL + token, null) ?: return null
        return Channel(
            token = token,
            channelId = channelId,
            vapid = prefs.getString(PREFIX_VAPID + channelId, null),
            endpoint = prefs.getString(PREFIX_ENDPOINT + channelId, null),
        )
    }

    fun channelForId(channelId: String): Channel? =
        channels().firstOrNull { it.channelId == channelId }

    /**
     * The channel for [token], made if there is none. A different VAPID key from the one
     * it was registered with voids its endpoint, because the server binds the two.
     */
    fun channelFor(token: String, vapid: String?): Channel {
        val existing = channelForToken(token)
        if (existing != null && existing.vapid == vapid) return existing
        val channelId = existing?.channelId ?: UUID.randomUUID().toString()
        prefs.edit {
            putString(PREFIX_CHANNEL + token, channelId)
            if (vapid == null) remove(PREFIX_VAPID + channelId) else putString(PREFIX_VAPID + channelId, vapid)
            remove(PREFIX_ENDPOINT + channelId)
        }
        return channelForToken(token)!!
    }

    fun setEndpoint(channelId: String, endpoint: String) =
        prefs.edit { putString(PREFIX_ENDPOINT + channelId, endpoint) }

    /** A new uaid means the server has forgotten every channel: all of them re-register. */
    fun forgetEndpoints() = prefs.edit {
        prefs.all.keys.filter { it.startsWith(PREFIX_ENDPOINT) }.forEach { remove(it) }
    }

    fun remove(token: String): Channel? {
        val channel = channelForToken(token) ?: return null
        prefs.edit {
            remove(PREFIX_CHANNEL + token)
            remove(PREFIX_VAPID + channel.channelId)
            remove(PREFIX_ENDPOINT + channel.channelId)
        }
        return channel
    }

    companion object {
        private const val PREFS = "embedded_push"
        private const val KEY_UAID = "uaid"
        private const val KEY_DEBUG_NO_ACCOUNT = "debug_without_account"
        private const val PREFIX_CHANNEL = "channel."
        private const val PREFIX_VAPID = "vapid."
        private const val PREFIX_ENDPOINT = "endpoint."
    }
}
