package com.wanderwildwood.ibasho.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.wanderwildwood.ibasho.utils.SingletonHolder
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.BadPaddingException


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

        // A keystore that is busy (say just after boot) is tried again before anything is
        // judged lost: three tries, 0.3 s and then 0.6 s apart.
        private const val OPEN_TRIES = 3
        private const val RETRY_DELAY_MILLIS = 300L

        // A failure that does not say the key is gone leaves the store shut for this start
        // only. Only when it happens on this many starts in a row is the store started afresh.
        private const val FAILED_STARTS_BEFORE_RESET = 3
        private const val FAILURES_FILE = "fmd_encrypted_settings_failures"
        private const val KEY_FAILED_STARTS = "failed_starts"
        private const val REOPEN_GAP_MILLIS = 10_000L

        /** Whether [e] says the key or the stored keyset can no longer decrypt: gone for good. */
        private fun meansKeyLost(e: Throwable): Boolean {
            var cause: Throwable? = e
            while (cause != null) {
                // BadPaddingException covers AEADBadTagException; InvalidKeyException covers
                // KeyPermanentlyInvalidatedException.
                if (cause is BadPaddingException || cause is InvalidKeyException ||
                    cause is UnrecoverableKeyException
                ) return true
                // Tink's word for a keyset that its key no longer opens.
                if (cause is GeneralSecurityException &&
                    cause.message?.contains("decrypt", ignoreCase = true) == true
                ) return true
                val next = cause.cause
                cause = if (next === cause) null else next
            }
            return false
        }
    }

    /**
     * The store, or null when it could not be opened on this start (and was kept for the next),
     * or not even after starting it afresh. Then nothing is kept (and nothing is read back):
     * never a plain-text fallback.
     */
    @Volatile
    private var sharedPrefs: SharedPreferences?

    private val appContext = context.applicationContext

    /**
     * True while the store is shut for this start but kept (see init): what it holds is not
     * gone, only out of reach for now. It is tried again, at most every [REOPEN_GAP_MILLIS].
     */
    @Volatile
    var isShut = false
        private set

    @Volatile
    private var lastReopenMillis = 0L

    /**
     * True when the store could not be opened on this start (say the phone's keystore lost its
     * key) and was started afresh, empty. A server login kept in it is gone: log in again.
     */
    var wasReset = false
        private set

    init {
        var opened: SharedPreferences? = null
        var lastError: Exception? = null
        for (attempt in 1..OPEN_TRIES) {
            try {
                opened = open(context)
                break
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Encrypted settings could not be opened (try $attempt of $OPEN_TRIES)", e)
                if (attempt < OPEN_TRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MILLIS * attempt)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                }
            }
        }

        val failures = context.getSharedPreferences(FAILURES_FILE, Context.MODE_PRIVATE)
        val failedStarts = failures.getInt(KEY_FAILED_STARTS, 0)
        sharedPrefs = if (opened != null) {
            if (failedStarts != 0) failures.edit().remove(KEY_FAILED_STARTS).commit()
            opened
        } else if (lastError != null && !meansKeyLost(lastError) &&
            failedStarts + 1 < FAILED_STARTS_BEFORE_RESET
        ) {
            // Not a lost key, as far as can be told: keep the store (and the server login in
            // it) as it is, shut for this start, and try again on the next.
            Log.e(TAG, "Encrypted settings could not be opened; kept for the next start", lastError)
            failures.edit().putInt(KEY_FAILED_STARTS, failedStarts + 1).commit()
            isShut = true
            lastReopenMillis = System.currentTimeMillis()
            null
        } else {
            Log.e(TAG, "Encrypted settings could not be opened; starting them afresh", lastError)
            failures.edit().remove(KEY_FAILED_STARTS).commit()
            startAfresh(context)
        }
    }

    /** Removes the store and its master key and opens a new, empty one; null if even that fails. */
    private fun startAfresh(context: Context): SharedPreferences? {
        wasReset = true
        context.deleteSharedPreferences(FILENAME)
        try {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                .deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        } catch (e2: Exception) {
            Log.e(TAG, "Could not remove the old master key", e2)
        }
        return try {
            open(context)
        } catch (e3: Exception) {
            Log.e(TAG, "Encrypted settings could not be started afresh either", e3)
            null
        }
    }

    /** The store; while it [isShut], one more try to open it, now and then. */
    private fun prefs(): SharedPreferences? {
        sharedPrefs?.let { return it }
        if (!isShut) return null
        synchronized(this) {
            sharedPrefs?.let { return it }
            val now = System.currentTimeMillis()
            if (now - lastReopenMillis < REOPEN_GAP_MILLIS) return null
            lastReopenMillis = now
            return try {
                open(appContext).also {
                    sharedPrefs = it
                    isShut = false
                    appContext.getSharedPreferences(FAILURES_FILE, Context.MODE_PRIVATE)
                        .edit().remove(KEY_FAILED_STARTS).apply()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Encrypted settings still could not be opened", e)
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
            prefs()?.getString(key, "") ?: ""
        } catch (e: Exception) {
            // A value that cannot be decrypted reads as not set.
            Log.e(TAG, "Could not read an encrypted setting", e)
            ""
        }
    }

    /** Written at once (commit), so a value is safe here before its plain copy is removed. */
    fun putString(key: String, value: String?): Boolean {
        val prefs = prefs() ?: return false
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
