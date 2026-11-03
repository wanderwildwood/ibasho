package com.wanderwildwood.ibasho.ui.family

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.family.FamilyLogic
import com.wanderwildwood.ibasho.family.FamilyLogic.Seen
import com.wanderwildwood.ibasho.family.FamilyRepository
import com.wanderwildwood.ibasho.ui.FmdActivity
import com.wanderwildwood.ibasho.ui.PagedColumn
import com.wanderwildwood.ibasho.ui.common.FmdTopAppBar
import com.wanderwildwood.ibasho.ui.settings.AddAccountActivity
import com.wanderwildwood.ibasho.ui.settings.FMDServerActivity
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

/**
 * Family: whether this phone shares its last place, with whom, and the last place of each person
 * who shares with this phone. Loads when opened and when pulled down; nothing runs in the
 * background from here. See docs/PLAN-family.md.
 */
class FamilyActivity : FmdActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Scaffold(topBar = { FmdTopAppBar(R.string.family_title) { finish() } }) { pad ->
                    Box(modifier = Modifier.padding(pad)) { FamilyScreen() }
                }
            }
        }
    }
}

private sealed interface Load {
    data object Looking : Load
    data class Done(val look: FamilyRepository.Look) : Load
    data object Failed : Load
}

@Composable
private fun FamilyScreen() {
    val context = LocalContext.current
    val repo = remember { FamilyRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    if (!repo.isAvailable()) {
        NoAccount()
        return
    }

    var load by remember { mutableStateOf<Load>(Load.Looking) }
    var sharing by remember { mutableStateOf(repo.isSharing) }
    var busy by remember { mutableStateOf(false) }
    var failedChange by remember { mutableStateOf(false) }
    var reloads by remember { mutableStateOf(0) }

    LaunchedEffect(reloads) {
        load = Load.Looking
        load = try {
            Load.Done(repo.look()).also { sharing = repo.isSharing }
        } catch (e: Exception) {
            Load.Failed
        }
    }

    /** Runs a change, then looks again so the screen shows what the server now holds. */
    fun change(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        failedChange = false
        scope.launch {
            try {
                action()
            } catch (e: Exception) {
                failedChange = true
            }
            sharing = repo.isSharing
            busy = false
            reloads++
        }
    }

    PagedColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        onPullAtTop = { reloads++ },
    ) {
        item {
            CheckRow(
                text = stringResource(R.string.family_share_my_place),
                checked = sharing,
                enabled = !busy,
                onChange = { on -> change { repo.setSharing(on) } },
            )
        }
        item {
            TextMMD(
                stringResource(
                    if (sharing) R.string.family_share_on_text else R.string.family_share_off_text
                )
            )
        }
        if (busy) item { TextMMD(stringResource(R.string.family_saving)) }
        if (failedChange) item { TextMMD(stringResource(R.string.family_change_failed)) }

        when (val l = load) {
            Load.Looking -> item { TextMMD(stringResource(R.string.family_looking)) }
            Load.Failed -> item { TextMMD(stringResource(R.string.family_load_failed)) }
            is Load.Done -> {
                if (l.look.rows.isEmpty()) {
                    item { TextMMD(stringResource(R.string.family_empty)) }
                }
                for (row in l.look.rows) {
                    item(key = row.person.name) {
                        PersonRow(
                            row = row,
                            sharing = sharing,
                            busy = busy,
                            fingerprint = repo.fingerprintOf(row.person),
                            onChoose = { on -> change { repo.setChosen(row.person.name, on) } },
                            onTrust = { change { repo.acceptNewKey(row.person.name) } },
                        )
                    }
                }
                item { TextMMD(stringResource(R.string.family_pull_hint)) }
            }
        }
    }
}

@Composable
private fun NoAccount() {
    val context = LocalContext.current
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextMMD(stringResource(R.string.family_needs_account))
        OutlinedButtonMMD(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val settings = SettingsRepository.getInstance(context)
                val target = if (settings.serverAccountExists()) FMDServerActivity::class.java
                else AddAccountActivity::class.java
                context.startActivity(Intent(context, target))
            },
        ) { TextMMD(stringResource(R.string.family_server_settings)) }
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled) { onChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckboxMMD(
            checked,
            onCheckedChange = if (enabled) onChange else null,
            modifier = Modifier.padding(end = 12.dp),
        )
        TextMMD(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PersonRow(
    row: FamilyRepository.Row,
    sharing: Boolean,
    busy: Boolean,
    fingerprint: String?,
    onChoose: (Boolean) -> Unit,
    onTrust: () -> Unit,
) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextMMD(row.person.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        TextMMD(seenText(row.seen))
        val seen = row.seen
        if (seen is Seen.Place) {
            OutlinedButtonMMD(
                modifier = Modifier.fillMaxWidth(),
                onClick = { openInMap(context, row.person.name, seen) },
            ) { TextMMD(stringResource(R.string.family_open_in_map)) }
        }
        // Whom to share with is asked only once sharing is on: a box that cannot be ticked
        // looks the same as one that can on a black-and-white screen.
        if (sharing) {
            CheckRow(
                text = stringResource(R.string.family_share_with_them),
                checked = row.chosen,
                enabled = !busy,
                onChange = onChoose,
            )
        }
        if (row.keyChanged || row.seen == Seen.KeyChanged) {
            TextMMD(stringResource(R.string.family_key_changed_text))
            ArmedTrustButton(enabled = !busy, onTrust = onTrust)
        }
        if (fingerprint != null) {
            TextMMD(stringResource(R.string.family_fingerprint, fingerprint))
        }
    }
}

/** Trusting a new key is deliberate: the first tap changes the words, the second within 6 s acts. */
@Composable
private fun ArmedTrustButton(enabled: Boolean, onTrust: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(6_000)
            armed = false
        }
    }
    OutlinedButtonMMD(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        onClick = {
            if (armed) {
                armed = false
                onTrust()
            } else {
                armed = true
            }
        },
    ) {
        TextMMD(stringResource(if (armed) R.string.family_trust_armed else R.string.family_trust))
    }
}

@Composable
private fun seenText(seen: Seen): String = when (seen) {
    is Seen.Place -> {
        val age = when (val a = FamilyLogic.age(System.currentTimeMillis(), seen.unixMillis)) {
            FamilyLogic.Age.JustNow -> stringResource(R.string.family_age_just_now)
            is FamilyLogic.Age.Minutes -> stringResource(R.string.family_age_minutes, a.n)
            is FamilyLogic.Age.Hours -> stringResource(R.string.family_age_hours, a.n)
            is FamilyLogic.Age.Date -> {
                val ctx = LocalContext.current
                DateFormat.getMediumDateFormat(ctx).format(Date(a.millis)) + " " +
                    DateFormat.getTimeFormat(ctx).format(Date(a.millis))
            }
        }
        val battery = seen.battery?.takeIf { it in 1..100 }
        if (battery != null) stringResource(R.string.family_seen_place_battery, age, battery)
        else stringResource(R.string.family_seen_place, age)
    }
    Seen.NotSharing -> stringResource(R.string.family_seen_not_sharing)
    Seen.Paused -> stringResource(R.string.family_seen_paused)
    Seen.NotSetUp -> stringResource(R.string.family_seen_not_set_up)
    Seen.KeyChanged -> stringResource(R.string.family_seen_key_changed)
    Seen.Unreadable -> stringResource(R.string.family_seen_unreadable)
}

/** Hands the place to whatever map app the phone has. Nothing else learns of it from here. */
private fun openInMap(context: android.content.Context, name: String, place: Seen.Place) {
    val lat = String.format(Locale.US, "%.6f", place.lat)
    val lon = String.format(Locale.US, "%.6f", place.lon)
    val label = Uri.encode(name)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon($label)"))
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        android.widget.Toast.makeText(context, R.string.family_no_map_app, android.widget.Toast.LENGTH_LONG).show()
    }
}
