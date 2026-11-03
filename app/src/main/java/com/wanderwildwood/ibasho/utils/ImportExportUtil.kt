package com.wanderwildwood.ibasho.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.os.Build
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonWriter
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.data.ALLOWLIST_FILENAME
import com.wanderwildwood.ibasho.data.AccessRepository
import com.wanderwildwood.ibasho.data.AllowlistRepository
import com.wanderwildwood.ibasho.data.SETTINGS_FILENAME
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.database.ACCESS_DB_FILENAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


private const val TAG = "ImportExportUtil"


class SettingsImportExporter(
    private val context: Context,
) {
    /** Set by [importData] when the backup is encrypted and the password given was not its. */
    var lastFailureWasWrongPassword = false
        private set

    companion object {
        fun filenameForExport(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                "fmd-settings-$date.zip"
            } else {
                "fmd-settings.zip"
            }
        }
    }

    suspend fun exportData(uri: Uri, password: String) {
        writeToUri(context, uri) { outputStream ->
            // Never an unencrypted backup: it holds the server password hash and the keys.
            require(password.isNotBlank()) { "A backup needs a password" }
            AccessRepository.getInstance(context).forceWriteToDisk()

            // https://github.com/srikanth-lingala/zip4j#working-with-streams
            val zipParas = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = CompressionLevel.NORMAL

                isEncryptFiles = true
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
            }
            val zipOutputStream = ZipOutputStream(outputStream, password.toCharArray())

            val filesToAdd = listOf(
                File(context.filesDir, SETTINGS_FILENAME),
                context.getDatabasePath(ACCESS_DB_FILENAME),
            )

            for (file in filesToAdd) {
                if (!file.exists()) {
                    // For example, the access.db may not exist if the user never configured it.
                    context.log().i(TAG, "Skipping ${file.name} because it doesn't exist.")
                    continue
                }
                zipParas.fileNameInZip = file.name
                zipOutputStream.putNextEntry(zipParas)
                file.inputStream().copyTo(zipOutputStream)
                zipOutputStream.closeEntry()
            }

            zipOutputStream.close()
        }
    }

    @Throws(JsonIOException::class, JsonSyntaxException::class)
    suspend fun importData(
        uri: Uri,
        password: String?,
    ): Boolean = withContext(Dispatchers.IO) {
        // We cannot use the file ending (.zip/.json) to detect the file,
        // since the URI path may not have those (e.g. when the file comes from Nextcloud as a content provider).
        // Thus we just need to try one format, and if it fails, try the other.

        lastFailureWasWrongPassword = false
        var success = importV1SettingsJson(uri)
        if (success) {
            return@withContext true
        }

        success = importV2Zip(uri, password)
        if (success) {
            return@withContext true
        }

        return@withContext false
    }

    // Old "settings.json"
    private fun importV1SettingsJson(uri: Uri): Boolean {
        var inputStream: InputStream? = null
        try {
            context.log().i(TAG, "Trying to import as JSON")
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                context.log().i(TAG, "Failed to open InputStream from URI")
                return false
            }

            // Read and checked first: a file that is JSON but not settings ("{}", a list,
            // another app's file) would otherwise reset every setting to its default.
            val bytes = inputStream.readBytes()
            if (!BackupZip.isSettings(bytes)) {
                context.log().i(TAG, "Not a settings.json")
                return false
            }
            SettingsRepository.getInstance(context).importFromStream(ByteArrayInputStream(bytes))

            // Apparently the import was successful
            context.log().i(TAG, "JSON import successful")
            return true
        } catch (e: Exception) {
            // continue
        } finally {
            inputStream?.close()
        }
        return false
    }

    // We need to open a new stream (or would need to reset the old stream).
    // Otherwise the ZIP reader starts reading wherever the JSON reader stopped.

    // New format, "fmd-export.zip" (contains settings.json and other data).
    //
    // Read and checked whole first (see [BackupZip]): a wrong password, a cut-off or damaged
    // file, or a zip that is not this app's backup changes nothing on the phone. Only then is
    // the access database swapped in, as a whole file renamed over the old one, and the settings
    // read in after it, so their migration runs on the restored database.
    private suspend fun importV2Zip(
        uri: Uri,
        password: String?,
    ): Boolean {
        context.log().i(TAG, "Trying to import ZIP")
        val entries = try {
            val input = context.contentResolver.openInputStream(uri)
            if (input == null) {
                context.log().i(TAG, "Failed to open InputStream from URI")
                return false
            }
            input.use {
                BackupZip.readAll(it, password?.toCharArray(), setOf(SETTINGS_FILENAME, ALLOWLIST_FILENAME, ACCESS_DB_FILENAME))
            }.also { BackupZip.check(it, SETTINGS_FILENAME, ACCESS_DB_FILENAME, ALLOWLIST_FILENAME) }
        } catch (e: BackupZip.Refused) {
            context.log().w(TAG, "ZIP refused before anything was changed: ${e.reason}")
            if (e.reason == BackupZip.Reason.WRONG_PASSWORD) {
                lastFailureWasWrongPassword = true
            }
            return false
        } catch (e: Exception) {
            context.log().e(TAG, "ZIP could not be read:\n${e.stackTraceToString()}")
            return false
        }

        val accessRepo = AccessRepository.getInstance(context)
        val target = context.getDatabasePath(ACCESS_DB_FILENAME)
        val staged = File(target.parentFile, "$ACCESS_DB_FILENAME.restoring")
        var dbClosed = false
        try {
            entries[ACCESS_DB_FILENAME]?.let { bytes ->
                staged.writeBytes(bytes)
                // Closed first: otherwise Room writes the database it holds in memory back over
                // the restored file.
                accessRepo.closeDb()
                dbClosed = true
                if (!staged.renameTo(target)) throw IOException("Could not put the restored database in place")
                // The old database's journal must not be replayed into the new one.
                File(target.path + "-wal").delete()
                File(target.path + "-shm").delete()
                File(target.path + "-journal").delete()
                accessRepo.openDb()
                dbClosed = false
            }
            SettingsRepository.getInstance(context).importFromStream(ByteArrayInputStream(entries.getValue(SETTINGS_FILENAME)))
            entries[ALLOWLIST_FILENAME]?.let {
                AllowlistRepository.getInstance(context).importFromStreamToDb(ByteArrayInputStream(it))
            }
            context.log().i(TAG, "ZIP import successful")
            return true
        } catch (e: Exception) {
            context.log().e(TAG, "ZIP import failed:\n${e.stackTraceToString()}")
            return false
        } finally {
            staged.delete()
            // A failed import must not leave the database closed.
            if (dbClosed) {
                accessRepo.openDb()
            }
        }
    }
}

fun writeAsJson(
    outputStreamWriter: OutputStreamWriter,
    // Receive the Gson as parameter, to ensure it is configured with the settings that src needs
    gson: Gson,
    src: Any,
) {
    val type = src.javaClass
    val writer = JsonWriter(outputStreamWriter)
    gson.toJson(src, type, writer)

    // Don't close the JsonWriter, as this also closes all of the underlying writers.
    // This would close the entire ZIP file during settings export.
    // Callers of this function should close the outputStreamWriter themselves.
}

// Coroutines: read/write must happen on the IO thread.
// Because when the URI is backed by a remote location (e.g. Nextcloud) we otherwise get a NetworkOnMainThreadException.
// Switch back to the main thread for UI-related tasks (showing a Toast).

suspend fun writeToUri(
    context: Context,
    uri: Uri,
    write: (OutputStream) -> Unit,
) = withContext(Dispatchers.IO) {
    var outputStream: OutputStream? = null
    try {
        outputStream = context.contentResolver.openOutputStream(uri) ?: return@withContext
        write(outputStream)
        context.log().i(TAG, "Export successful")
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.export_success, Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        context.log().e(TAG, "Export failed:\n${e.stackTraceToString()}")
        // Don't leave a broken or empty file behind.
        deleteDocumentQuietly(context, uri)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
        }
    } finally {
        outputStream?.close()
    }
}

/** Removes a document the picker created for an export that did not happen. */
fun deleteDocumentQuietly(context: Context, uri: Uri) {
    try {
        DocumentsContract.deleteDocument(context.contentResolver, uri)
    } catch (e: Exception) {
        context.log().w(TAG, "Could not remove the unused export file:\n${e.message}")
    }
}
