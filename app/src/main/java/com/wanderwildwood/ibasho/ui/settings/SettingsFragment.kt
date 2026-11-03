package com.wanderwildwood.ibasho.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.ibasho.ui.PagedColumn
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import android.app.Activity
import com.wanderwildwood.ibasho.ui.family.FamilyActivity
import com.wanderwildwood.ibasho.ui.setup.SetupGuideActivity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.wanderwildwood.ibasho.ui.common.EInkAlertDialogBuilder
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.ui.TaggedFragment
import com.wanderwildwood.ibasho.ui.access.AccessControlActivity
import com.wanderwildwood.ibasho.ui.common.PasswordSetDialog
import com.wanderwildwood.ibasho.ui.helper.SettingsEntry
import com.wanderwildwood.ibasho.utils.deleteDocumentQuietly
import com.wanderwildwood.ibasho.utils.SettingsImportExporter
import com.wanderwildwood.ibasho.utils.SettingsImportExporter.Companion.filenameForExport
import com.wanderwildwood.ibasho.utils.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.io.File

class SettingsFragment : TaggedFragment() {

    companion object {
        private const val EXPORT_REQ_CODE = 30
        private const val IMPORT_REQ_CODE = 40
        private const val KEYGUARD_REQ_CODE = 50

        private const val TEMP_ZIP_NAME = "tmp_backup_import.zip"

        val TAG = SettingsFragment::class.simpleName
    }

    private lateinit var settings: SettingsRepository

    override fun getStaticTag(): String = "SettingsFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository.Companion.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settingsEntries = SettingsEntry.getSettingsEntries(view.context)
        view.findViewById<ComposeView>(R.id.listSettings).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { AppTheme { SettingsList(settingsEntries) { open(it) } } }
        }
    }

    private fun open(entry: SettingsEntry) {
        val context = requireContext()
        var settingIntent: Intent? = null
        when (entry.action) {
            SettingsEntry.Action.SERVER -> settingIntent = if (!settings.serverAccountExists()) {
                Intent(context, AddAccountActivity::class.java)
            } else {
                Intent(context, FMDServerActivity::class.java)
            }
            SettingsEntry.Action.FAMILY -> settingIntent = Intent(context, FamilyActivity::class.java)
            SettingsEntry.Action.COMMANDS -> settingIntent = Intent(context, FMDConfigActivity::class.java)
            SettingsEntry.Action.ACCESS_CONTROL -> settingIntent = Intent(context, AccessControlActivity::class.java)
            SettingsEntry.Action.LOCATION_SOURCES -> settingIntent = Intent(context, OpenCellIdActivity::class.java)
            SettingsEntry.Action.EXPORT -> authenticateForBackup(context)
            SettingsEntry.Action.IMPORT -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.setType("*/*")
                startActivityForResult(intent, IMPORT_REQ_CODE)
            }
            SettingsEntry.Action.LANGUAGE -> settingIntent = Intent(context, AppearanceActivity::class.java)
            SettingsEntry.Action.SETUP_GUIDE -> settingIntent = Intent(context, SetupGuideActivity::class.java)
            SettingsEntry.Action.LOGS -> settingIntent = Intent(context, LogViewActivity::class.java)
            // About is not a row here; it is the "i" on the main screen.
            SettingsEntry.Action.TROUBLESHOOTING -> settingIntent = Intent(context, DebuggingActivity::class.java)
        }

        if (settingIntent != null) {
            startActivity(settingIntent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val context = activity ?: return
        if (requestCode == IMPORT_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                if (uri != null) {
                    handleImport(context, uri)
                } else {
                    context.log().w(TAG, "Cannot import: URI is null!")
                }
            }
        } else if (requestCode == EXPORT_REQ_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                if (uri != null) {
                    showExportPasswordDialog(context, uri)
                } else {
                    context.log().w(TAG, "Cannot export: URI is null!")
                }
            }
        } else if (requestCode == KEYGUARD_REQ_CODE && resultCode == Activity.RESULT_OK) {
            exportBackup()
        }
    }

    private fun showExportPasswordDialog(context: Context, uri: Uri) {
        // A password is required: an unencrypted backup would carry the server password hash
        // and the keys in the clear.
        val message = context.getString(R.string.export_password_message) +
                "\n\n" + context.getString(R.string.export_password_rule)

        PasswordSetDialog(
            context,
            { password ->
                lifecycleScope.launch {
                    SettingsImportExporter(context).exportData(uri, password)
                }
            },
            R.string.export_password_enter_title,
            message,
            // The picker has already made the file; no backup means no empty file left behind.
            onCancel = { deleteDocumentQuietly(context, uri) },
        ).show()
    }

    private fun handleImport(context: Context, uri: Uri) = lifecycleScope.launch {
        // Copy to temporary file
        val tempFile = File(context.cacheDir, TEMP_ZIP_NAME)

        // Reading from the input stream can make a network request (if the URI comes from a cloud storage)
        withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)!!.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        var isZipEncrypted = false
        var zipFile: ZipFile? = null
        try {
            zipFile = ZipFile(tempFile)
            isZipEncrypted = zipFile.isEncrypted
            context.log().d(TAG, "isZipEncrypted=$isZipEncrypted")
        } catch (e: ZipException) {
            context.log().w(TAG, "Failed to check if ZIP is encrypted:\n${e.message}")
        } finally {
            zipFile?.close()
        }

        if (isZipEncrypted) {
            showImportPasswordDialog(context)
        } else {
            executeImport(context, null)
        }
    }

    private fun showImportPasswordDialog(context: Context, wrongPassword: Boolean = false) {
        val message = if (wrongPassword) {
            context.getString(R.string.import_password_wrong) + "\n\n" +
                    context.getString(R.string.import_password_message)
        } else {
            context.getString(R.string.import_password_message)
        }

        PasswordSetDialog(
            context,
            { password -> executeImport(context, password) },
            R.string.export_password_enter_title,
            message,
            forceMinLength = false,
            // Cancelling leaves nothing behind in the cache.
            onCancel = { File(context.cacheDir, TEMP_ZIP_NAME).delete() },
        ).show()
    }

    private fun executeImport(context: Context, password: String?) = lifecycleScope.launch {
        val tempFile = File(context.cacheDir, TEMP_ZIP_NAME)
        val importer = SettingsImportExporter(context)
        val isSuccessful = importer.importData(tempFile.toUri(), password)
        if (!isSuccessful && importer.lastFailureWasWrongPassword) {
            // Ask again; the copy stays until the user gives up or it works.
            showImportPasswordDialog(context, wrongPassword = true)
            return@launch
        }
        if (isSuccessful) {
            showImportSuccessDialog(context)
        } else {
            showImportFailedDialog(context)
        }

        // Delete after finishing the import
        tempFile.delete()
    }

    private fun showImportSuccessDialog(context: Context) {
        // TODO: The message_warning is temporary. We should include these in the backup again.
        val message =
            getString(R.string.import_success_message) + "\n\n" + getString(R.string.import_success_message_warning)
        EInkAlertDialogBuilder(context)
            .setTitle(getString(R.string.Settings_Import_Success))
            .setMessage(message)
            .setPositiveButton(getString(R.string.Ok)) { _, _ -> }
            .setCancelable(false)
            .show()
    }

    private fun showImportFailedDialog(context: Context) {
        EInkAlertDialogBuilder(context)
            .setTitle(getString(R.string.Settings_Import_Failed))
            .setMessage(R.string.import_failed_message)
            .setPositiveButton(getString(R.string.Ok)) { _, _ -> }
            .setCancelable(false)
            .show()
    }

    private fun authenticateForBackup(context: Context) {
        val keyGuard = context.getSystemService(KeyguardManager::class.java)
        if (!keyGuard.isDeviceSecure) {
            // No secure lockscreen, proceed immediately
            exportBackup()
            return
        }

        val title = context.getString(R.string.export_authenticate_title)

        // setAllowedAuthenticators() does not work on SDK <= 29
        // https://developer.android.com/reference/androidx/biometric/BiometricPrompt.PromptInfo.Builder#setAllowedAuthenticators(int)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            val intent = keyGuard.createConfirmDeviceCredentialIntent(title, "")
            startActivityForResult(intent, KEYGUARD_REQ_CODE)
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .setTitle(title)
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                exportBackup()
            }
        }
        val biometricPrompt = BiometricPrompt(this, callback)
        biometricPrompt.authenticate(promptInfo)
    }

    private fun exportBackup() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.putExtra(Intent.EXTRA_TITLE, filenameForExport())
        intent.setType("*/*")
        startActivityForResult(intent, EXPORT_REQ_CODE)
    }

}

/**
 * The Settings rows on the house list: whole rows per page, with its scrollbar, no rules between
 * them; a group begins with space above its first row.
 */
@Composable
private fun SettingsList(entries: List<SettingsEntry>, onOpen: (SettingsEntry) -> Unit) {
    PagedColumn(modifier = Modifier.fillMaxSize()) {
        items(entries) { entry ->
            TextMMD(
                text = entry.label(),
                fontSize = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (entry.startsGroup()) 16.dp else 0.dp)
                    .clickable { onOpen(entry) }
                    .padding(16.dp),
            )
        }
    }
}
