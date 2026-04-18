package de.nulide.findmydevice.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.nulide.findmydevice.utils.SingletonHolder


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
        private const val KEY_FMD_PIN = "KEY_FMD_PIN"
        private const val KEY_DELETE_PASSWORD = "KEY_DELETE_PASSWORD"
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

    fun getDeletePassword(): String = getString(KEY_DELETE_PASSWORD)

    fun setDeletePassword(new: String?) = putString(KEY_DELETE_PASSWORD, new)
}
