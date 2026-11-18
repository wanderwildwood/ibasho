package com.wanderwildwood.ibasho.data

import android.content.Context
import androidx.core.net.toUri
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.ToNumberPolicy
import com.google.gson.stream.JsonReader
import com.wanderwildwood.ibasho.BuildConfig
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.commands.FmdPermission
import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V2_MASTER_KEY
import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V2_PASSWORD_KEY
import com.wanderwildwood.ibasho.database.NotificationPassword
import com.wanderwildwood.ibasho.database.SmsPassword
import com.wanderwildwood.ibasho.utils.CypherUtils
import com.wanderwildwood.ibasho.utils.SingletonHolder
import com.wanderwildwood.ibasho.utils.Utils
import com.wanderwildwood.ibasho.utils.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.EncodedKeySpec
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec


const val SETTINGS_FILENAME = "settings.json"

/**
 * Settings should be accessed through this repository.
 * This is to only have a single Settings instance,
 * thus preventing race conditions.
 */
class SettingsRepository private constructor(private val context: Context) {

    companion object :
        SingletonHolder<SettingsRepository, Context>(::SettingsRepository) {

        val TAG = SettingsRepository::class.simpleName
    }

    private val passwordToHashCache: MutableMap<String, String> = mutableMapOf()

    private val gson = GsonBuilder()
        // Force Gson to parse numbers as either Long or Double.
        // When needed, we can cast Longs down to Ints.
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .serializeSpecialFloatingPointValues() // to allow NaN
        .create()

    // Should only be accessed via the getters/setters in this repository
    private var settings: Settings

    /** One lock for reading, changing and writing the map, whichever map [settings] holds now. */
    private val lock = Any()

    /** Writes the last known location, in order, off the location callbacks' main thread. */
    private val lastLocationWriter = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var encSettings: EncryptedSettingsRepository

    init {
        settings = loadNoSet()
        encSettings = EncryptedSettingsRepository.getInstance(context)
        sealSecrets()
    }

    fun load() {
        settings = loadNoSet()
        encSettings = EncryptedSettingsRepository.getInstance(context)
        sealSecrets()
    }

    /**
     * Moves the v1 login hash and private key (see [SealedSettings]) out of the plain settings
     * into the encrypted store, written there first and then removed from the file. Runs on
     * every load and import, so a plain copy never stays, whatever version it came from. If the
     * encrypted store cannot take them, they are dropped all the same, never kept in plain: the
     * server then needs logging in again.
     */
    private fun sealSecrets() {
        if (SealedSettings.KEYS.keys.none { settings.containsKey(it) }) return
        val values = synchronized(lock) { SealedSettings.takeOut(settings) }
        for ((storeKey, value) in values) {
            if (!encSettings.putString(storeKey, value)) {
                context.log().e(TAG, "Could not keep $storeKey in the encrypted store; log in again")
            }
        }
        context.log().i(TAG, "Moved ${values.size} server login values into the encrypted store")
        saveSettings()
    }

    private fun loadNoSet(): Settings {
        val file = File(context.filesDir, SETTINGS_FILENAME)
        if (!file.exists()) {
            file.createNewFile()
        }

        // Better crash with a JsonSyntaxException than silently resetting the settings (they are important!).
        // If a user is affected by a crash due to an invalid settings JSON, they can manually fix this
        // by clearing the entire app storage.
        FileReader(file).use { reader ->
            return gson.fromJson(reader, Settings::class.java) ?: Settings()
        }
    }

    // Synchronized so two threads never write the file at the same time.
    // Whereabouts: written beside the file, synced and renamed over it, so a kill mid-write keeps
    // the settings as they were rather than leaving half a file that stops the app starting.
    private fun saveSettings() = synchronized(lock) {
        val file = File(context.filesDir, SETTINGS_FILENAME)
        val tmp = File(context.filesDir, "$SETTINGS_FILENAME.tmp")
        java.io.FileOutputStream(tmp).use { out ->
            val writer = java.io.OutputStreamWriter(out, Charsets.UTF_8)
            gson.toJson(settings, Settings::class.java, writer)
            writer.flush()
            out.fd.sync()
        }
        if (!tmp.renameTo(file)) {
            tmp.delete()
            throw java.io.IOException("settings could not be saved")
        }
    }

    fun <T> set(key: Int, value: T) {
        SealedSettings.KEYS[key]?.let { storeKey ->
            encSettings.putString(storeKey, value as String?)
            return
        }
        synchronized(lock) {
            settings.set(key, value)
            saveSettings()
        }
    }

    /** Sets several plain (not sealed) settings at once, with one save. */
    private fun setAll(values: List<Pair<Int, Any?>>) {
        synchronized(lock) {
            for ((key, value) in values) {
                check(!SealedSettings.isSealed(key)) { "sealed setting $key" }
                settings.set(key, value)
            }
            saveSettings()
        }
    }

    fun get(key: Int): Any {
        SealedSettings.KEYS[key]?.let { storeKey -> return encSettings.getString(storeKey) }
        return synchronized(lock) { settings.get(key) }
    }

    fun remove(key: Int) {
        SealedSettings.KEYS[key]?.let { storeKey ->
            encSettings.putString(storeKey, null)
            return
        }
        synchronized(lock) {
            settings.remove(key)
            saveSettings()
        }
    }

    /**
     * The settings JSON for an export: the plain settings, with the v1 login hash and private
     * key put back in, so a v1 account still works after the backup is imported on a new phone.
     * The export is a password-protected zip; on import they go straight back into the
     * encrypted store.
     */
    fun exportJsonBytes(): ByteArray {
        val copy = synchronized(lock) { Settings().also { it.putAll(settings) } }
        SealedSettings.putBack(copy) { encSettings.getString(it) }
        return gson.toJson(copy, Settings::class.java).toByteArray(Charsets.UTF_8)
    }

    fun writeAsJson(outputStreamWriter: OutputStreamWriter) {
        com.wanderwildwood.ibasho.utils.writeAsJson(outputStreamWriter, gson, settings)
    }

    @Throws(JsonIOException::class, JsonSyntaxException::class)
    fun importFromStream(inputStream: InputStream) {
        val reader = JsonReader(InputStreamReader(inputStream))
        settings = gson.fromJson(reader, Settings::class.java) ?: Settings()

        // The imported settings replace this phone's, the v1 login with them: whatever the
        // encrypted store held for v1 goes, and what the backup carries (if anything) goes in.
        for (storeKey in SealedSettings.KEYS.values) encSettings.putString(storeKey, null)
        sealSecrets()

        // Migrate after import to bring them to the latest structure
        runBlocking { migrateSettings() }

        saveSettings()
    }

    suspend fun migrateSettings() {
        val currentVersion = (get(Settings.SET_SET_VERSION) as Number).toInt()

        migrateBackgroundLocationType()
        if (currentVersion < 4) {
            migrateFmdPinToDb()
        }
        if (currentVersion < 5) {
            AccessRepository.getInstance(context).migrateNumbersToE164(context)
        }
        if (currentVersion < 6) {
            // The wipe command is gone: its switch and its password go with it. Upstream's old
            // PIN, already copied into the access database above as a hash, goes too.
            remove(Settings.SET_WIPE_ENABLED)
            encSettings.removeDeletePassword()
            encSettings.removeFmdPin()

            // Lock (it can put any text on the screen) is now off by default for the server.
            // A phone set up before keeps what it had: if it never chose, that was everything,
            // lock included, so that is written down for it. A new install (version 0) is left
            // to the new default.
            if (currentVersion > 0 && !settings.containsKey(Settings.SET_FMDSERVER_PERMISSIONS)) {
                set(Settings.SET_FMDSERVER_PERMISSIONS, FmdPermission.ALL)
            }
        }

        set(Settings.SET_SET_VERSION, Settings.SETTINGS_VERSION)
    }


    private fun migrateBackgroundLocationType() {
        val oldType = (get(Settings.SET_FMDSERVER_LOCATION_TYPE) as Number).toInt()
        if (oldType < BackgroundLocationType.BASE) {
            val newType = BackgroundLocationType.fromOldEncoding(oldType)
            set(Settings.SET_FMDSERVER_LOCATION_TYPE, newType.encode())
        }
    }

    suspend fun migrateFmdPinToDb() {
        val pin = encSettings.getFmdPin()
        if (pin.isNotBlank()) {
            context.log().i(TAG, "Migrating FMD PIN to database")
            val hash = hashLocalPassword(pin)

            val accessRepo = AccessRepository.getInstance(context)
            accessRepo.insertSmsPassword(
                SmsPassword(
                    0,
                    label = "FMD PIN",
                    passwordHash = hash,
                    permission = FmdPermission.ALL
                )
            )
            accessRepo.insertNotificationPassword(
                NotificationPassword(
                    0,
                    label = "FMD PIN",
                    passwordHash = hash,
                    permission = FmdPermission.ALL
                )
            )
        }
    }

    // ---------- Convenience helpers ----------

    // Run this on the compute-dispatcher (to avoid blocking the main thread)
    suspend fun hashLocalPassword(password: String): String = withContext(Dispatchers.Default) {
        // Cache password hashes to avoid expensive re-computations (e.g., during repeated notification-based access).
        val cachedHash = passwordToHashCache[password]
        if (cachedHash != null) {
            return@withContext cachedHash
        }

        // Use the same salt for all local passwords.
        // This is necessary so that we can hash once and then compare against all passwords in the database.
        // This (in turn) is necessary because a priori we don't know which password entry the user intended to use.
        // Security: hashes are stored in the local database and don't leave the device (exception: ZIP export).
        var saltBase64 = get(Settings.SET_LOCAL_PASSWORD_SALT_B64) as String
        if (saltBase64.isBlank()) {
            saltBase64 = CypherUtils.generateArgon2SaltB64()
            set(Settings.SET_LOCAL_PASSWORD_SALT_B64, saltBase64)
        }

        val hash = CypherUtils.hashPasswordForLocalAccess(password, saltBase64)
        passwordToHashCache[password] = hash
        return@withContext hash
    }

    fun serverAccountExists(): Boolean {
        // The SET_FMDSERVER_ID is remembered during logout.
        // Therefore, check both (to be sure).
        val id = get(Settings.SET_FMDSERVER_ID) as String
        val pwV1 = get(Settings.SET_FMD_CRYPT_HPW) as String
        val pwV2 = encSettings.getString(KEY_FMDSERVER_V2_PASSWORD_KEY)
        return id.isNotEmpty() && (pwV1.isNotEmpty() || pwV2.isNotEmpty())
    }

    fun setKeys(keys: FmdKeyPair) {
        set(Settings.SET_FMD_CRYPT_PRIVKEY, keys.encryptedPrivateKey)
        set(Settings.SET_FMD_CRYPT_PUBKEY, CypherUtils.encodeBase64(keys.publicKey.encoded))
    }

    fun getKeysV1(): FmdKeyPair? {
        if (get(Settings.SET_FMD_CRYPT_PUBKEY) == "") {
            return null
        }

        val pubKeySpec: EncodedKeySpec = X509EncodedKeySpec(
            CypherUtils.decodeBase64(get(Settings.SET_FMD_CRYPT_PUBKEY) as String)
        )
        var publicKey: PublicKey? = null
        try {
            val keyFactory = KeyFactory.getInstance("RSA")
            publicKey = keyFactory.generatePublic(pubKeySpec)
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        } catch (e: InvalidKeySpecException) {
            e.printStackTrace()
        }

        return if (publicKey != null) {
            FmdKeyPair(publicKey, get(Settings.SET_FMD_CRYPT_PRIVKEY) as String)
        } else {
            null
        }
    }

    fun removeServerAccount(full: Boolean = true) {
        // If not full removal, remember the user ID for autofill convenience.
        if (full) {
            set(Settings.SET_FMDSERVER_ID, "")
        }

        // Proto v1
        set(Settings.SET_FMD_CRYPT_HPW, "")
        set(Settings.SET_FMD_CRYPT_PRIVKEY, "")
        set(Settings.SET_FMD_CRYPT_PUBKEY, "")

        // Proto v2
        encSettings.putString(KEY_FMDSERVER_V2_PASSWORD_KEY, "")
        encSettings.putString(KEY_FMDSERVER_V2_MASTER_KEY, "")

        encSettings.setCachedAccessToken("")

        // Family sharing belongs to the account: whom it shared with and the keys it pinned.
        set(Settings.SET_FAMILY_SHARING, false)
        set(Settings.SET_FAMILY_AUDIENCE, "")
        set(Settings.SET_FAMILY_PINS, "")
        set(Settings.SET_FAMILY_KEY_CHANGED, "")

        // The next account's uploads are judged from its own first run.
        set(Settings.SET_FMDSERVER_FIRST_UPLOAD_JOB_MILLIS, 0L)
    }

    /**
     * Caches [loc] as the last known location. All seven values go in under one lock and one
     * save, so the cache never pairs one fix's latitude with another's longitude, and the save
     * runs on a background thread: the callers are location callbacks on the main thread.
     */
    fun storeLastKnownLocation(loc: FmdLocation) {
        lastLocationWriter.execute {
            try {
                setAll(
                    listOf(
                        // historically stored as String
                        Settings.SET_LAST_KNOWN_LOCATION_LAT to loc.lat.toString(),
                        Settings.SET_LAST_KNOWN_LOCATION_LON to loc.lon.toString(),
                        Settings.SET_LAST_KNOWN_LOCATION_ACCURACY to (loc.accuracy ?: Float.NaN),
                        Settings.SET_LAST_KNOWN_LOCATION_ALTITUDE to (loc.altitude ?: Double.NaN),
                        Settings.SET_LAST_KNOWN_LOCATION_BEARING to (loc.bearing ?: Float.NaN),
                        Settings.SET_LAST_KNOWN_LOCATION_SPEED to (loc.speed ?: Float.NaN),
                        Settings.SET_LAST_KNOWN_LOCATION_TIME to loc.timeMillis,
                    )
                )
            } catch (e: Exception) {
                context.log().e(TAG, "Could not cache the last known location: $e")
            }
        }
    }

    /**
     * Return the last known location as cached by the settings.
     */
    fun getLastKnownLocation(): FmdLocation? {
        return try {
            // Under the lock, so all seven values come from the same save.
            synchronized(lock) { getLastKnownLocationInt() }
        } catch (e: NumberFormatException) {
            null
        } catch (e: ClassCastException) {
            // https://gitlab.com/fmd-foss/fmd-android/-/issues/379
            // java.lang.ClassCastException: java.lang.String cannot be cast to java.lang.Number
            null
        }
    }

    private fun getLastKnownLocationInt(): FmdLocation {
        var acc: Float? = (get(Settings.SET_LAST_KNOWN_LOCATION_ACCURACY) as Number).toFloat()
        if (acc!!.isNaN()) {
            acc = null
        }
        var alt: Double? = (get(Settings.SET_LAST_KNOWN_LOCATION_ALTITUDE) as Number).toDouble()
        if (alt!!.isNaN()) {
            alt = null
        }
        var bear: Float? = (get(Settings.SET_LAST_KNOWN_LOCATION_BEARING) as Number).toFloat()
        if (bear!!.isNaN()) {
            bear = null
        }
        var speed: Float? = (get(Settings.SET_LAST_KNOWN_LOCATION_SPEED) as Number).toFloat()
        if (speed!!.isNaN()) {
            speed = null
        }

        return FmdLocation(
            lat = (get(Settings.SET_LAST_KNOWN_LOCATION_LAT) as String).toDouble(),
            lon = (get(Settings.SET_LAST_KNOWN_LOCATION_LON) as String).toDouble(),
            accuracy = acc,
            altitude = alt,
            bearing = bear,
            speed = speed,
            provider = context.getString(R.string.cmd_locate_last_known_location_text),
            batteryLevel = Utils.getBatteryLevel(context),
            timeMillis = (get(Settings.SET_LAST_KNOWN_LOCATION_TIME) as Number).toLong(),
        )
    }
}
