package com.wanderwildwood.ibasho.ui.access

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.EditText
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import com.wanderwildwood.ibasho.ui.common.EInkAlertDialogBuilder
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.data.AccessRepository
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.database.NotificationPassword
import com.wanderwildwood.ibasho.database.PhoneNumber
import com.wanderwildwood.ibasho.database.SmsPassword
import com.wanderwildwood.ibasho.ui.FmdActivity
import com.wanderwildwood.ibasho.ui.common.closeOnlyWhen
import com.wanderwildwood.ibasho.ui.common.passwordProblem
import com.wanderwildwood.ibasho.ui.common.showDialogError
import com.wanderwildwood.ibasho.utils.normalizeNumberForStorage
import kotlinx.coroutines.launch

class AccessControlActivity : FmdActivity(), AccessControlFuns {

    companion object {
        const val EXTRA_NOTIF = "EXTRA_NOTIF"
    }

    private lateinit var accessRepo: AccessRepository
    private lateinit var settings: SettingsRepository

    private val viewModel: AccessControlViewModel by viewModels { AccessControlViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accessRepo = AccessRepository.getInstance(this)
        settings = SettingsRepository.getInstance(this)

        val initialPage = if (intent.extras?.getInt(EXTRA_NOTIF) != null) 2 else 0

        setContent {
            AccessControlTabsScreen(
                onBackClicked = { finish() },
                accessFuns = this,
                initialPage,
            )
        }
    }

    /* ------- Phone numbers ------- */

    // XXX: Migrate these adding dialogs to Compose??
    override fun onAddPhoneNumberClicked() {
        val context = this
        val layout = layoutInflater.inflate(R.layout.dialog_phone_number, null)
        val nameInput = layout.findViewById<EditText>(R.id.editTextName)
        val phoneNumberInput = layout.findViewById<EditText>(R.id.editTextPhoneNumber)

        val dialog = EInkAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.allowlist_add_phone_number))
            .setView(layout)
            .setPositiveButton(getString(R.string.add), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        // A number that cannot be right keeps the dialog open, with the reason in it.
        dialog.closeOnlyWhen {
            val name = nameInput.getText().toString()
            val number = phoneNumberInput.getText().toString()
            val valid = normalizeNumberForStorage(context, number) != null
            layout.showDialogError(if (valid) null else R.string.allowlist_invalid_number)
            if (valid) {
                lifecycleScope.launch {
                    addContactToAllowList(number, name)
                }
            }
            valid
        }
        dialog.show()
    }

    override fun onAddContactClicked() {
        var intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        try {
            pickContactLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            try {
                pickContactLauncher.launch(intent)
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(this, R.string.WhiteList_no_contact_picker, Toast.LENGTH_LONG).show()
            }
        }
    }

    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            return@registerForActivityResult
        }
        val data = result.data ?: return@registerForActivityResult

        // Multiple items selected
        val clipData = data.clipData
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i).uri?.let { addContactFromUri(it) }
            }
        }

        // Single item selected
        data.data?.let { addContactFromUri(it) }
    }

    private fun addContactFromUri(uri: Uri) {
        val projection = arrayOf<String>(
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cursor = managedQuery(uri, projection, null, null, null)

        if (!cursor.moveToFirst()) {
            // cursor is empty
            return
        }
        do {
            val nameIdx = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)
            var cName = ""
            if (nameIdx >= 0) {
                cName = cursor.getString(nameIdx)
            }

            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            var cNumber = ""
            if (numIdx >= 0) {
                cNumber = cursor.getString(numIdx)
            }

            lifecycleScope.launch {
                addContactToAllowList(cNumber, cName)
            }
        } while (cursor.moveToNext())
    }

    private suspend fun addContactToAllowList(rawNumber: String, name: String) {
        val normNumber = normalizeNumberForStorage(this, rawNumber)
        if (normNumber == null) {
            Toast.makeText(this, R.string.allowlist_invalid_number, Toast.LENGTH_LONG).show()
            return
        }
        if (accessRepo.getPhoneNumber(normNumber) != null) {
            Toast.makeText(this, R.string.Toast_Duplicate_contact, Toast.LENGTH_LONG).show()
            return
        }

        val newNumber = PhoneNumber(0, name, normNumber)
        accessRepo.insertPhoneNumber(newNumber)

        if (!(settings.get(Settings.SET_FIRST_TIME_CONTACT_ADDED) as Boolean)) {
            val keyword = settings.get(Settings.SET_FMD_COMMAND) as String
            val message =
                getString(R.string.tip_first_contact_added, keyword, keyword, keyword)
            EInkAlertDialogBuilder(this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    settings.set<Boolean>(Settings.SET_FIRST_TIME_CONTACT_ADDED, true)
                }
                .show()
        }
    }

    /* ------- SMS Passwords ------- */

    private fun showAddPasswordDialog(
        @StringRes title: Int,
        onSaveClicked: suspend (label: String, passwordHash: String) -> Unit,
    ) {
        val context = this
        val layout = layoutInflater.inflate(R.layout.dialog_password_labelled, null)
        val labelInput = layout.findViewById<EditText>(R.id.editTextLabel)
        val passwordInput = layout.findViewById<EditText>(R.id.editTextPassword)

        val dialog = EInkAlertDialogBuilder(context)
            .setTitle(title)
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.add), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        // A wrong password keeps the dialog open, with the label and password still typed in.
        dialog.closeOnlyWhen {
            val label = labelInput.getText().toString()
            val password = passwordInput.getText().toString()
            val problem = passwordProblem(context, password, forceMinLength = true, allowEmpty = false)
            layout.showDialogError(problem)
            if (problem == null) {
                viewModel.setLoading(true)
                lifecycleScope.launch {
                    val passwordHash = settings.hashLocalPassword(password)
                    onSaveClicked(label, passwordHash)
                    viewModel.setLoading(false)
                }
            }
            problem == null
        }
        dialog.show()
    }

    override fun onAddSmsPasswordClicked() {
        showAddPasswordDialog(R.string.access_sms_password_add, { label, passwordHash ->
            onSubmitSmsPassword(label, passwordHash)
        })
    }

    private suspend fun onSubmitSmsPassword(label: String, passwordHash: String) {
        val old = accessRepo.getSmsPassword(passwordHash)
        if (old != null) {
            val msg = getString(R.string.access_password_exists, old.label)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            return
        }

        val entry = SmsPassword(0, label, passwordHash)
        accessRepo.insertSmsPassword(entry)
    }

    /* ------- Notification Passwords ------- */

    override fun onAddNotificationPasswordClicked() {
        showAddPasswordDialog(R.string.access_notification_password_add, { label, passwordHash ->
            onSubmitNotificationPassword(label, passwordHash)
        })
    }

    private suspend fun onSubmitNotificationPassword(label: String, passwordHash: String) {
        val old = accessRepo.getNotificationPassword(passwordHash)
        if (old != null) {
            val msg = getString(R.string.access_password_exists, old.label)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            return
        }

        val entry = NotificationPassword(0, label, passwordHash)
        accessRepo.insertNotificationPassword(entry)
    }

}
