package com.wanderwildwood.ibasho.ui.setup

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.data.AccessRepository
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.database.PhoneNumber
import com.wanderwildwood.ibasho.permissions.DoNotDisturbAccessPermission
import com.wanderwildwood.ibasho.permissions.LocationPermission
import com.wanderwildwood.ibasho.permissions.OverlayPermission
import com.wanderwildwood.ibasho.permissions.Permission
import com.wanderwildwood.ibasho.permissions.SmsPermission
import com.wanderwildwood.ibasho.ui.FmdActivity
import com.wanderwildwood.ibasho.ui.common.FmdTopAppBar
import com.wanderwildwood.ibasho.ui.settings.AddAccountActivity
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import com.wanderwildwood.ibasho.utils.normalizeNumberForStorage
import com.wanderwildwood.ibasho.warnings.isBackgroundRestricted
import com.wanderwildwood.ibasho.warnings.isMuditaKompakt
import com.wanderwildwood.ibasho.warnings.openKompaktHelp
import kotlinx.coroutines.launch

/**
 * The first thing a new install shows: one question, then only the steps its answer needs.
 *
 * Texts only is the whole app for most people who lose a phone: a number they trust texts
 * it, and it answers. It needs no server, no computer and no account, so that path never
 * mentions them, and afterwards the app keeps the server out of sight until one is added,
 * the way Messaging stays a plain texting app for anyone who never sets up Signal.
 *
 * Texts and a server adds the web page, and on a Kompakt the one step a phone cannot do by
 * itself. Either way the guide can be left at any point and opened again from Settings.
 */
class SetupGuideActivity : FmdActivity() {

    // Bumped each time the screen comes back, so every "Allowed" is read afresh after the
    // phone's own permission screens.
    private var resumed by mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        resumed++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Scaffold(topBar = { FmdTopAppBar(R.string.setup_title) { finish() } }) { pad ->
                    Box(modifier = Modifier.padding(pad)) { Guide(resumed) }
                }
            }
        }
    }

    fun finishGuide(mode: String) {
        val settings = SettingsRepository.getInstance(this)
        settings.set(Settings.SET_SETUP_MODE, mode)
        settings.set(Settings.SET_SETUP_GUIDE_DONE, true)
        finish()
    }
}

private enum class Step { CHOOSE, ALLOW, NUMBER, RING, SERVER, KOMPAKT, DONE }

@Composable
private fun Guide(resumed: Int) {
    val activity = LocalContext.current as SetupGuideActivity
    var mode by rememberSaveable { mutableStateOf(Settings.VAL_SETUP_TEXTS) }
    var step by rememberSaveable { mutableStateOf(Step.CHOOSE) }

    // The server path adds its own steps after the ones every phone needs.
    val order = buildList {
        add(Step.CHOOSE); add(Step.ALLOW); add(Step.NUMBER); add(Step.RING)
        if (mode == Settings.VAL_SETUP_SERVER) {
            add(Step.SERVER)
            if (isMuditaKompakt()) add(Step.KOMPAKT)
        }
        add(Step.DONE)
    }
    val next = { step = order[order.indexOf(step) + 1] }
    BackHandler(enabled = step != Step.CHOOSE) { step = order[order.indexOf(step) - 1] }

    when (step) {
        Step.CHOOSE -> ChooseStep(
            onTexts = { mode = Settings.VAL_SETUP_TEXTS; next() },
            onServer = { mode = Settings.VAL_SETUP_SERVER; next() },
            onLater = { activity.finish() },
        )
        Step.ALLOW -> PermissionStep(
            R.string.setup_allow_title, R.string.setup_allow_text,
            listOf(SmsPermission(), LocationPermission()), resumed, next,
        )
        Step.NUMBER -> NumberStep(next)
        Step.RING -> PermissionStep(
            R.string.setup_ring_title, R.string.setup_ring_text,
            listOf(OverlayPermission(), DoNotDisturbAccessPermission()), resumed, next,
            optional = true,
        )
        Step.SERVER -> ServerStep(resumed, next)
        Step.KOMPAKT -> KompaktStep(resumed, next)
        Step.DONE -> DoneStep(mode) { activity.finishGuide(mode) }
    }
}

@Composable
private fun Page(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumnMMD(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun Heading(@StringRes text: Int) =
    TextMMD(stringResource(text), fontWeight = FontWeight.Bold)

@Composable
private fun ChooseStep(onTexts: () -> Unit, onServer: () -> Unit, onLater: () -> Unit) = Page {
    item { TextMMD(stringResource(R.string.setup_intro)) }
    item {
        ButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = onTexts) {
            TextMMD(stringResource(R.string.setup_choose_texts))
        }
    }
    item { TextMMD(stringResource(R.string.setup_choose_texts_text)) }
    item {
        OutlinedButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = onServer) {
            TextMMD(stringResource(R.string.setup_choose_server))
        }
    }
    item { TextMMD(stringResource(R.string.setup_choose_server_text)) }
    item {
        OutlinedButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = onLater) {
            TextMMD(stringResource(R.string.setup_later))
        }
    }
}

/** A row per permission: its name, and either "Allowed" or the button that asks for it. */
@Composable
private fun PermissionStep(
    @StringRes title: Int,
    @StringRes text: Int,
    permissions: List<Permission>,
    resumed: Int,
    onNext: () -> Unit,
    optional: Boolean = false,
) {
    val activity = LocalContext.current as Activity
    // Read on every return to this screen; `resumed` is only here to make that happen.
    val granted = permissions.map { resumed.let { _ -> it.isGranted(activity) } }
    Page {
        item { Heading(title) }
        item { TextMMD(stringResource(text)) }
        permissions.forEachIndexed { i, p ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextMMD(stringResource(p.name), modifier = Modifier.weight(1f))
                    if (granted[i]) {
                        TextMMD(stringResource(R.string.setup_allowed), fontWeight = FontWeight.Bold)
                    } else {
                        OutlinedButtonMMD(onClick = { p.request(activity) }) {
                            TextMMD(stringResource(R.string.setup_allow))
                        }
                    }
                }
            }
        }
        item {
            ButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = onNext) {
                TextMMD(
                    stringResource(
                        if (granted.all { it } || !optional) R.string.setup_next else R.string.setup_skip
                    )
                )
            }
        }
    }
}

@Composable
private fun NumberStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val repo = AccessRepository.getInstance(context)
    val scope = rememberCoroutineScope()
    val numbers by repo.getPhoneNumbers().collectAsState(initial = emptyList())
    var name by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }

    fun add(rawNumber: String, rawName: String) {
        val norm = normalizeNumberForStorage(context, rawNumber)
        if (norm == null) {
            Toast.makeText(context, R.string.allowlist_invalid_number, Toast.LENGTH_LONG).show()
            return
        }
        scope.launch {
            if (repo.getPhoneNumber(norm) == null) {
                repo.insertPhoneNumber(PhoneNumber(0, rawName, norm))
            }
            SettingsRepository.getInstance(context).set(Settings.SET_FIRST_TIME_CONTACT_ADDED, true)
        }
        name = ""
        number = ""
    }

    val pickContact = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri: Uri = result.data?.data ?: return@rememberLauncherForActivityResult
        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null, null, null,
        )?.use { c ->
            if (c.moveToFirst()) {
                val n = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val p = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (p >= 0) add(c.getString(p), if (n >= 0) c.getString(n) ?: "" else "")
            }
        }
    }

    Page {
        item { Heading(R.string.setup_number_title) }
        item { TextMMD(stringResource(R.string.setup_number_text)) }
        numbers.forEach { n ->
            item {
                TextMMD(if (n.name.isBlank()) n.number else "${n.name}, ${n.number}", fontWeight = FontWeight.Bold)
            }
        }
        item {
            TextFieldMMD(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = { name = it },
                label = { TextMMD(stringResource(R.string.setup_number_name)) },
                singleLine = true,
            )
        }
        item {
            TextFieldMMD(
                modifier = Modifier.fillMaxWidth(),
                value = number,
                onValueChange = { number = it },
                label = { TextMMD(stringResource(R.string.setup_number_number)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
        }
        item {
            OutlinedButtonMMD(
                modifier = Modifier.fillMaxWidth(),
                enabled = number.isNotBlank(),
                onClick = { add(number, name) },
            ) { TextMMD(stringResource(R.string.setup_number_add)) }
        }
        item {
            OutlinedButtonMMD(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    try {
                        pickContact.launch(
                            Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        )
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(context, R.string.WhiteList_no_contact_picker, Toast.LENGTH_LONG).show()
                    }
                },
            ) { TextMMD(stringResource(R.string.setup_number_contacts)) }
        }
        item {
            ButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = onNext) {
                TextMMD(stringResource(if (numbers.isEmpty()) R.string.setup_skip else R.string.setup_next))
            }
        }
    }
}

@Composable
private fun ServerStep(resumed: Int, onNext: () -> Unit) {
    val context = LocalContext.current
    val settings = SettingsRepository.getInstance(context)
    val loggedIn = resumed.let { settings.serverAccountExists() }
    Page {
        item { Heading(R.string.setup_server_title) }
        item { TextMMD(stringResource(R.string.setup_server_text)) }
        item {
            if (loggedIn) {
                TextMMD(
                    stringResource(R.string.setup_server_done, settings.get(Settings.SET_FMDSERVER_ID) as String),
                    fontWeight = FontWeight.Bold,
                )
            } else {
                OutlinedButtonMMD(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { context.startActivity(Intent(context, AddAccountActivity::class.java)) },
                ) { TextMMD(stringResource(R.string.setup_server_login)) }
            }
        }
        item {
            ButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = onNext) {
                TextMMD(stringResource(if (loggedIn) R.string.setup_next else R.string.setup_skip))
            }
        }
    }
}

@Composable
private fun KompaktStep(resumed: Int, onNext: () -> Unit) {
    val context = LocalContext.current
    val restricted = resumed.let { isBackgroundRestricted(context) }
    Page {
        item { Heading(R.string.setup_kompakt_title) }
        item { TextMMD(stringResource(R.string.setup_kompakt_text)) }
        item {
            TextMMD(
                stringResource(if (restricted) R.string.setup_kompakt_restricted else R.string.setup_kompakt_clear),
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            OutlinedButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = { openKompaktHelp(context) }) {
                TextMMD(stringResource(R.string.kompakt_help_button))
            }
        }
        item {
            ButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = onNext) {
                TextMMD(stringResource(R.string.setup_next))
            }
        }
    }
}

@Composable
private fun DoneStep(mode: String, onFinish: () -> Unit) {
    val context = LocalContext.current
    val keyword = SettingsRepository.getInstance(context).get(Settings.SET_FMD_COMMAND) as String
    Page {
        item { Heading(R.string.setup_done_title) }
        item { TextMMD(stringResource(R.string.setup_done_text, keyword, keyword, keyword)) }
        if (mode == Settings.VAL_SETUP_SERVER) {
            item { TextMMD(stringResource(R.string.setup_done_server)) }
        }
        if (mode == Settings.VAL_SETUP_TEXTS && isMuditaKompakt()) {
            item { TextMMD(stringResource(R.string.setup_done_kompakt)) }
        }
        item { TextMMD(stringResource(R.string.setup_done_later)) }
        item {
            ButtonMMD(modifier = Modifier.fillMaxWidth(), onClick = onFinish) {
                TextMMD(stringResource(R.string.setup_finish))
            }
        }
    }
}
