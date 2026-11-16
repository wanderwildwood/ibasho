package com.wanderwildwood.ibasho.data

import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V1_HASHED_PASSWORD
import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V1_PRIVATE_KEY

/**
 * Settings that live in the encrypted store, not in the plain settings file: the server
 * protocol v1 login hash and the v1 private key (wrapped with the password).
 *
 * [SettingsRepository] reads and writes them there under their old settings keys, moves any
 * plain copy out of the settings file (an older install, or an imported backup), and puts them
 * back into the settings JSON only for an export, which is a password-protected zip and is how
 * a v1 account moves to a new phone.
 */
object SealedSettings {

    /** Settings key → key in the encrypted store. */
    val KEYS: Map<Int, String> = mapOf(
        Settings.SET_FMD_CRYPT_HPW to KEY_FMDSERVER_V1_HASHED_PASSWORD,
        Settings.SET_FMD_CRYPT_PRIVKEY to KEY_FMDSERVER_V1_PRIVATE_KEY,
    )

    fun isSealed(key: Int): Boolean = key in KEYS

    /**
     * Removes every sealed key from [settings] and returns the values that were set (not blank),
     * by their key in the encrypted store.
     */
    fun takeOut(settings: MutableMap<Int, Any?>): Map<String, String> {
        val found = mutableMapOf<String, String>()
        for ((settingsKey, storeKey) in KEYS) {
            if (!settings.containsKey(settingsKey)) continue
            val value = settings.remove(settingsKey) as? String
            if (!value.isNullOrBlank()) found[storeKey] = value
        }
        return found
    }

    /** [settings] with the sealed values put back in (only those that are set), for an export. */
    fun putBack(settings: MutableMap<Int, Any?>, read: (storeKey: String) -> String) {
        for ((settingsKey, storeKey) in KEYS) {
            val value = read(storeKey)
            if (value.isNotBlank()) settings[settingsKey] = value
        }
    }
}
