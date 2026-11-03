package com.wanderwildwood.ibasho.utils

import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class BackupZipTest {

    private val settingsName = "settings.json"
    private val dbName = "access.db"
    private val allowlistName = "whitelist.json"
    private val known = setOf(settingsName, dbName, allowlistName)

    private val settings = """{"8":5,"0":"https://fmd.example.org"}""".toByteArray()
    // Random past the header, so the stored entry is as long as the file and a cut or a changed
    // byte near the end of the zip lands inside it.
    private val db = kotlin.random.Random(7).nextBytes(4096).also { "SQLite format 3\u0000".toByteArray().copyInto(it) }

    private fun zip(password: String?, vararg entries: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out, password?.toCharArray()).use { z ->
            for ((name, bytes) in entries) {
                z.putNextEntry(ZipParameters().apply {
                    fileNameInZip = name
                    compressionMethod = CompressionMethod.DEFLATE
                    if (password != null) {
                        isEncryptFiles = true
                        encryptionMethod = EncryptionMethod.AES
                        aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                    }
                })
                z.write(bytes)
                z.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun readAndCheck(bytes: ByteArray, password: String?): Map<String, ByteArray> =
        BackupZip.readAll(ByteArrayInputStream(bytes), password?.toCharArray(), known)
            .also { BackupZip.check(it, settingsName, dbName, allowlistName) }

    private fun refused(reason: BackupZip.Reason, block: () -> Unit) {
        try {
            block()
            fail("expected $reason")
        } catch (e: BackupZip.Refused) {
            assertEquals(reason, e.reason)
        }
    }

    @Test
    fun aGoodEncryptedBackupComesBackWhole() {
        val entries = readAndCheck(zip("correct horse", settingsName to settings, dbName to db), "correct horse")
        assertArrayEquals(settings, entries[settingsName])
        assertArrayEquals(db, entries[dbName])
    }

    @Test
    fun theWrongPasswordIsSaidAsSuch() {
        refused(BackupZip.Reason.WRONG_PASSWORD) { readAndCheck(zip("correct horse", settingsName to settings, dbName to db), "battery staple") }
    }

    @Test
    fun aCutOffFileIsRefusedBeforeTheDatabaseIsReached() {
        val whole = zip("correct horse", settingsName to settings, dbName to db)
        // Cut inside the database entry: the settings before it read fine, and still nothing is handed back.
        val cut = whole.copyOf(whole.size - 400)
        refused(BackupZip.Reason.DAMAGED) { readAndCheck(cut, "correct horse") }
    }

    @Test
    fun aChangedByteIsRefused() {
        val whole = zip(null, settingsName to settings, dbName to db)
        // The same zip with one byte of the stored database changed after it was written.
        val tampered = whole.copyOf().also { bytes ->
            val at = bytes.size - 300
            bytes[at] = (bytes[at].toInt() xor 0x55).toByte()
        }
        assertTrue(readAndCheck(whole, null).isNotEmpty())
        refused(BackupZip.Reason.DAMAGED) { readAndCheck(tampered, null) }
    }

    @Test
    fun aZipWithoutSettingsOrWithoutDataIsIncomplete() {
        refused(BackupZip.Reason.INCOMPLETE) { readAndCheck(zip("pw123456", dbName to db), "pw123456") }
        refused(BackupZip.Reason.INCOMPLETE) { readAndCheck(zip("pw123456", settingsName to settings), "pw123456") }
        try {
            readAndCheck("not a zip at all".toByteArray(), null)
            fail("a file that is not a zip was accepted")
        } catch (e: BackupZip.Refused) {
            // Either reason will do; what matters is that it is refused.
        }
    }

    @Test
    fun aDatabaseThatIsNotSqliteIsRefused() {
        refused(BackupZip.Reason.DAMAGED) {
            readAndCheck(zip("pw123456", settingsName to settings, dbName to ByteArray(4096) { 1 }), "pw123456")
        }
    }

    @Test
    fun emptyOrForeignSettingsAreNotSettings() {
        assertFalse(BackupZip.isSettings("{}".toByteArray()))
        assertFalse(BackupZip.isSettings("null".toByteArray()))
        assertFalse(BackupZip.isSettings("""{"name":"Ada Whitlock"}""".toByteArray()))
        assertFalse(BackupZip.isSettings("[1,2]".toByteArray()))
        assertTrue(BackupZip.isSettings(settings))
    }

    @Test
    fun anOlderBackupWithAnAllowlistIsAccepted() {
        val allowlist = """[{"name":"Tomas Reyes","number":"+15550100002"}]""".toByteArray()
        val entries = readAndCheck(zip("pw123456", settingsName to settings, allowlistName to allowlist), "pw123456")
        assertArrayEquals(allowlist, entries[allowlistName])
    }
}
