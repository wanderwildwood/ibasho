package com.wanderwildwood.ibasho.utils

import com.google.gson.JsonParser
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.io.inputstream.ZipInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Reads an exported backup zip whole and checks it, before anything on the phone is touched.
 *
 * Every entry is read to its end, which is where zip4j checks each one (the CRC of a plain entry,
 * the authentication code of an AES one), so a cut-off, damaged or tampered file is refused here
 * rather than half-way through replacing the database. Only a backup that passes is handed back,
 * as bytes, for the caller to put in place.
 */
object BackupZip {

    enum class Reason { WRONG_PASSWORD, DAMAGED, INCOMPLETE, TOO_BIG }

    class Refused(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause)

    /** The first sixteen bytes of every SQLite database file. */
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    /**
     * The entries of the zip in [input] that this app knows ([known]), or a [Refused].
     * Unknown entries are read (so they are checked too) and dropped. [maxBytes] caps the whole.
     */
    fun readAll(input: InputStream, password: CharArray?, known: Set<String>, maxBytes: Long = 64L * 1024 * 1024): Map<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        var total = 0L
        try {
            val zip = ZipInputStream(input, password)
            val buf = ByteArray(8192)
            while (true) {
                val entry = zip.nextEntry ?: break
                val out = ByteArrayOutputStream()
                while (true) {
                    val n = zip.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > maxBytes) throw Refused(Reason.TOO_BIG)
                    out.write(buf, 0, n)
                }
                if (entry.fileName in known) {
                    if (entry.fileName in entries) throw Refused(Reason.DAMAGED)
                    entries[entry.fileName] = out.toByteArray()
                }
            }
        } catch (e: Refused) {
            throw e
        } catch (e: ZipException) {
            throw Refused(if (e.type == ZipException.Type.WRONG_PASSWORD) Reason.WRONG_PASSWORD else Reason.DAMAGED, e)
        } catch (e: Exception) {
            throw Refused(Reason.DAMAGED, e)
        }
        return entries
    }

    /**
     * Checks what [readAll] gave back: the settings are there and are settings, an access
     * database (if there is one) is an SQLite file, an allowlist (if there is one, from older
     * versions) is a JSON list, and
     * there is at least one of the two. Throws a [Refused] otherwise.
     */
    fun check(entries: Map<String, ByteArray>, settingsName: String, accessDbName: String, allowlistName: String) {
        val settings = entries[settingsName] ?: throw Refused(Reason.INCOMPLETE)
        if (!isSettings(settings)) throw Refused(Reason.DAMAGED)
        val db = entries[accessDbName]
        val allowlist = entries[allowlistName]
        if (db == null && allowlist == null) throw Refused(Reason.INCOMPLETE)
        if (db != null && !isSqlite(db)) throw Refused(Reason.DAMAGED)
        if (allowlist != null && !isJsonArray(allowlist)) throw Refused(Reason.DAMAGED)
    }

    fun isSqlite(bytes: ByteArray): Boolean =
        bytes.size >= 512 && SQLITE_HEADER.indices.all { bytes[it] == SQLITE_HEADER[it] }

    fun isJsonArray(bytes: ByteArray): Boolean = try {
        JsonParser.parseString(String(bytes, Charsets.UTF_8)).isJsonArray
    } catch (e: Exception) {
        false
    }

    /**
     * A settings file is a JSON object keyed by setting numbers, with something in it: "{}" or
     * "null" would be read as "every setting at its default", and a key that is not a number
     * would fail half-way through the import.
     */
    fun isSettings(bytes: ByteArray): Boolean = try {
        val element = JsonParser.parseString(String(bytes, Charsets.UTF_8))
        element.isJsonObject && element.asJsonObject.size() > 0 &&
            element.asJsonObject.keySet().all { it.toIntOrNull() != null }
    } catch (e: Exception) {
        false
    }
}
