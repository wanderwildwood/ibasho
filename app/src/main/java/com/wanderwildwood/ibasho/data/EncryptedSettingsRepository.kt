package com.wanderwildwood.ibasho.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wanderwildwood.ibasho.utils.SingletonHolder


/**
 * Storage for sensitive values that benefit from an additional layer of encryption.
 *
 * Because encryption is device-bound, these settings cannot (and should not) be backed up.
 */
class EncryptedSettingsRepository private constructor(context: Context) {

    companion object :
        SingletonHolder<EncryptedSettingsRepository, Context>(::EncryptedSettingsRepository) {

        val TAG = EncryptedSettingsRepository::class.simpleName

        // This file should be EXCLUDED from backups
        private const val FILENAME = "fmd_encrypted_settings"

        private const val KEY_SERVER_CACHED_ACCESS_TOKEN = "KEY_SERVER_CACHED_ACCESS_TOKEN"
        const val KEY_FMDSERVER_V2_PASSWORD_KEY = "KEY_SERVER_V2_PASSWORD_KEY"
        const val KEY_FMDSERVER_V2_MASTER_KEY = "KEY_SERVER_V2_MASTER_KEY"

        private const val KEY_FMD_PIN = "KEY_FMD_PIN"
        // The wipe command's password, kept as typed and later as a hash. The command is gone;
        // both are removed by the settings migration (see SettingsRepository.migrateSettings).
        private const val KEY_DELETE_PASSWORD = "KEY_DELETE_PASSWORD"
        private const val KEY_DELETE_PASSWORD_HASH = "KEY_DELETE_PASSWORD_HASH"
    }

    val sharedPrefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPrefs = EncryptedSharedPreferences.create(
            context,
            FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getString(key: String): String {
        return sharedPrefs.getString(key, "") ?: ""
    }

    fun putString(key: String, value: String?) {
        if (value.isNullOrBlank()) {
            sharedPrefs.edit().remove(key).apply()
        } else {
            sharedPrefs.edit().putString(key, value).apply()
        }
    }

    fun getCachedAccessToken(): String = getString(KEY_SERVER_CACHED_ACCESS_TOKEN)


    fun setCachedAccessToken(newToken: String) = putString(KEY_SERVER_CACHED_ACCESS_TOKEN, newToken)

    fun getFmdPin(): String = getString(KEY_FMD_PIN)

    /** Upstream's old PIN, which [SettingsRepository] moves into the access database. */
    fun removeFmdPin() = putString(KEY_FMD_PIN, null)

    /** Removes the password of the wipe command, which this app no longer has. */
    fun removeDeletePassword() {
        sharedPrefs.edit().remove(KEY_DELETE_PASSWORD).remove(KEY_DELETE_PASSWORD_HASH).commit()
    }
}
