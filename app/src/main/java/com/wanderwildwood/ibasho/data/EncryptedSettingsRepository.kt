package com.wanderwildwood.ibasho.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wanderwildwood.ibasho.utils.SingletonHolder
import java.security.KeyStore


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

        // Server protocol v1: the login hash and the private key (wrapped with the password).
        // Kept in the plain settings file until settings version 6; see SettingsRepository.
        const val KEY_FMDSERVER_V1_HASHED_PASSWORD = "KEY_SERVER_V1_HASHED_PASSWORD"
        const val KEY_FMDSERVER_V1_PRIVATE_KEY = "KEY_SERVER_V1_PRIVATE_KEY"

        private const val KEY_FMD_PIN = "KEY_FMD_PIN"
        // The wipe command's password, kept as typed and later as a hash. The command is gone;
        // both are removed by the settings migration (see SettingsRepository.migrateSettings).
        private const val KEY_DELETE_PASSWORD = "KEY_DELETE_PASSWORD"
        private const val KEY_DELETE_PASSWORD_HASH = "KEY_DELETE_PASSWORD_HASH"
    }

    /**
     * The store, or null when it could not be opened even after starting it afresh. Then nothing
     * is kept (and nothing is read back): never a plain-text fallback.
     */
    private val sharedPrefs: SharedPreferences?

    /**
     * True when the store could not be opened on this start (say the phone's keystore lost its
     * key) and was started afresh, empty. A server login kept in it is gone: log in again.
     */
    var wasReset = false
        private set

    init {
        sharedPrefs = try {
            open(context)
        } catch (e: Exception) {
            Log.e(TAG, "Encrypted settings could not be opened; starting them afresh", e)
            wasReset = true
            context.deleteSharedPreferences(FILENAME)
            try {
                KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                    .deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            } catch (e2: Exception) {
                Log.e(TAG, "Could not remove the old master key", e2)
            }
            try {
                open(context)
            } catch (e3: Exception) {
                Log.e(TAG, "Encrypted settings could not be started afresh either", e3)
                null
            }
        }
    }

    private fun open(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getString(key: String): String {
        return try {
            sharedPrefs?.getString(key, "") ?: ""
        } catch (e: Exception) {
            // A value that cannot be decrypted reads as not set.
            Log.e(TAG, "Could not read an encrypted setting", e)
            ""
        }
    }

    /** Written at once (commit), so a value is safe here before its plain copy is removed. */
    fun putString(key: String, value: String?): Boolean {
        val prefs = sharedPrefs ?: return false
        return if (value.isNullOrBlank()) {
            prefs.edit().remove(key).commit()
        } else {
            prefs.edit().putString(key, value).commit()
        }
    }

    fun getCachedAccessToken(): String = getString(KEY_SERVER_CACHED_ACCESS_TOKEN)


    fun setCachedAccessToken(newToken: String) = putString(KEY_SERVER_CACHED_ACCESS_TOKEN, newToken)

    fun getFmdPin(): String = getString(KEY_FMD_PIN)

    /** Upstream's old PIN, which [SettingsRepository] moves into the access database. */
    fun removeFmdPin() = putString(KEY_FMD_PIN, null)

    /** Removes the password of the wipe command, which this app no longer has. */
    fun removeDeletePassword() {
        putString(KEY_DELETE_PASSWORD, null)
        putString(KEY_DELETE_PASSWORD_HASH, null)
    }
}
