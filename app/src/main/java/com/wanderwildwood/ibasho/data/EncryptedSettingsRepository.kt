package com.wanderwildwood.ibasho.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wanderwildwood.ibasho.utils.CypherUtils
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
        // Earlier versions kept the wipe password here as it was typed. It is now kept only as
        // an Argon2id hash (KEY_DELETE_PASSWORD_HASH); a plain one left from before is hashed
        // and removed the first time it is looked at (see migrateDeletePasswordToHash).
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

    /** Whether a wipe password is set. Cheap: no hashing, safe on the main thread. */
    fun hasDeletePassword(): Boolean =
        getString(KEY_DELETE_PASSWORD_HASH).isNotBlank() || getString(KEY_DELETE_PASSWORD).isNotBlank()

    /**
     * Sets (or, with null or blank, removes) the wipe password. Only its hash is kept.
     * Hashing takes a moment: call this off the main thread.
     */
    @Synchronized
    fun setDeletePassword(new: String?) {
        if (new.isNullOrBlank()) {
            sharedPrefs.edit().remove(KEY_DELETE_PASSWORD).remove(KEY_DELETE_PASSWORD_HASH).apply()
            return
        }
        val hash = CypherUtils.hashPasswordForLocalAccess(new, CypherUtils.generateArgon2SaltB64())
        sharedPrefs.edit()
            .putString(KEY_DELETE_PASSWORD_HASH, hash)
            .remove(KEY_DELETE_PASSWORD)
            .commit()
    }

    /**
     * Whether [candidate] is the wipe password. Constant-time on the hash. Off the main thread.
     * False when no password is set.
     */
    fun checkDeletePassword(candidate: String): Boolean {
        migrateDeletePasswordToHash()
        val hash = getString(KEY_DELETE_PASSWORD_HASH)
        if (hash.isBlank() || candidate.isEmpty()) return false
        return CypherUtils.checkPasswordForFmdPin(hash, candidate)
    }

    /** Replaces a wipe password kept as typed (earlier versions) by its hash. Off the main thread. */
    @Synchronized
    fun migrateDeletePasswordToHash() {
        val plain = getString(KEY_DELETE_PASSWORD)
        if (plain.isBlank()) return
        setDeletePassword(plain)
    }
}
