package com.wanderwildwood.ibasho.ui.access

import androidx.annotation.StringRes
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import kotlinx.coroutines.delay
import com.wanderwildwood.ibasho.ui.theme.EInkDialog
import com.wanderwildwood.ibasho.ui.PagedColumn
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.database.AccessItem
import com.wanderwildwood.ibasho.database.PhoneNumber
import com.wanderwildwood.ibasho.database.SmsPassword
import com.wanderwildwood.ibasho.database.SmsPasswordWithTempPhoneNumbers
import com.wanderwildwood.ibasho.database.TempPhoneNumber
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import com.wanderwildwood.ibasho.utils.Utils
import com.wanderwildwood.ibasho.utils.normalizeNumberForDisplay

// Help class so that we can both:
// 1. Pass a ViewModel to create a tab to keep the upper code simple.
// 2. Have a variant with explicit functions that we can write Previews for.
@Composable
fun <T : AccessItem> AccessControlTab(
    accessType: AccessType<T>,
    accessItems: List<T>,
    onAddClicked: () -> Unit,
    onAddSecondaryClicked: (() -> Unit)?,
    viewModel: AccessControlViewModel = viewModel(factory = AccessControlViewModel.Factory),
) {
    AccessControlTab(
        accessType,
        accessItems,
        onAddClicked,
        onAddSecondaryClicked,
        onDeleteClicked = { viewModel.deleteItem(it) },
        onSavePermissionClicked = { item, newPerm ->
            viewModel.updatePermissionForItem(item, newPerm)
        },
        commandKeyword = viewModel.getCommandKeyword(),
    )
}

@Composable
fun <T : AccessItem> AccessControlTab(
    accessType: AccessType<T>,
    accessItems: List<T>,
    onAddClicked: () -> Unit = {},
    onAddSecondaryClicked: (() -> Unit)? = null,
    onDeleteClicked: (T) -> Unit = {},
    onSavePermissionClicked: (T, Long) -> Unit = { _, _ -> },
    commandKeyword: String = "fmd",
) {
    var permissionToEdit: T? by rememberSaveable { mutableStateOf(null) }
    var itemToDelete: T? by rememberSaveable { mutableStateOf(null) }
    val dismissDialog = { permissionToEdit = null; itemToDelete = null }

    // Main body
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // One paging list whether or not there is anything in it. The empty case used to lay
        // the two hints out flat and give the "nothing here" line whatever height was left,
        // which at 480x800 was not enough: it was cut off behind the buttons.
        PagedColumn(
            modifier = Modifier.weight(1f),
        ) {
            item {
                TextMMD(stringResource(accessType.hintText, commandKeyword))
                Spacer(Modifier.height(8.dp))
                TextMMD(stringResource(R.string.access_permission_hint))
                Spacer(Modifier.height(32.dp))
            }
            if (accessItems.isEmpty()) {
                item {
                    TextMMD(
                        text = stringResource(accessType.emptyText),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(accessItems) { item ->
                ItemElement(
                    item,
                    onEditPermissionsClicked = { permissionToEdit = item },
                    onDeleteClicked = { itemToDelete = item })
            }
        }

        // Bottom buttons
        ButtonMMD(
            onClick = onAddClicked,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextMMD(stringResource(accessType.addText))
        }

        if (onAddSecondaryClicked != null && accessType.addSecondaryText != null) {
            ButtonMMD(
                onClick = onAddSecondaryClicked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextMMD(stringResource(accessType.addSecondaryText))
            }
        }
    }

    // Dialogs
    permissionToEdit?.let { item ->
        FmdPermissionDialog(item.getItemPermission(), dismissDialog) { newPerm ->
            onSavePermissionClicked(item, newPerm)
            dismissDialog()
        }
    }
    itemToDelete?.let { toDelete ->
        DeleteDialog(toDelete, dismissDialog, onDeleteClicked, accessType.deleteTitle)
    }
}

@Composable
private fun <T : AccessItem> ItemElement(
    item: T,
    onEditPermissionsClicked: (T) -> Unit,
    onDeleteClicked: (T) -> Unit,
) {
    if (item is SmsPasswordWithTempPhoneNumbers) {
        Column {
            ItemRow(item, onEditPermissionsClicked, onDeleteClicked)

            // Header
            if (item.tempPhoneNumbers.isNotEmpty()) {
                // Bold, not italic: on sixteen greys a sloped stroke loses its thin end
                // and reads as a smudge rather than as emphasis.
                val style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
                ) {
                    TextMMD(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.access_sms_password_used_by),
                        style = style,
                    )
                    TextMMD(
                        text = stringResource(R.string.access_sms_password_expiry),
                        style = style,
                    )
                }
            }

            // Table items
            val context = LocalContext.current
            for (num in item.tempPhoneNumbers) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextMMD(
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { Utils.copyToClipboard(context, "", num.number) },
                            )
                            // Make click area larger
                            .padding(vertical = 6.dp),
                        text = num.number,
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextMMD(num.expiryPretty(), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    } else {
        ItemRow(item, onEditPermissionsClicked, onDeleteClicked)
    }
}

@Composable
private fun <T : AccessItem> ItemRow(
    item: T,
    onEditPermissionsClicked: (T) -> Unit,
    onDeleteClicked: (T) -> Unit,
) {
    val context = LocalContext.current
    // The number is stored as E.164 now, so it is shown the way the phone writes a number
    // locally; a nameless entry is just the number, as upstream words it.
    val label = when (item) {
        is PhoneNumber -> {
            val shown = normalizeNumberForDisplay(context, item.number) ?: item.number
            if (item.name.isBlank()) shown else stringResource(R.string.allowlist_entry, item.name, shown)
        }
        else -> item.toDisplayLabel(context)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        TextMMD(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { Utils.copyToClipboard(context, "", label) },
                )
                // Make click area larger
                .padding(vertical = 8.dp),
            text = label,
        )
        Spacer(Modifier.width(8.dp))
        OutlinedIconButton({ onEditPermissionsClicked(item) }, modifier = Modifier.size(48.dp)) {
            Icon(
                painterResource(R.drawable.ic_shield),
                contentDescription = stringResource(R.string.fmd_permission_allowed_commands_title),
            )
        }
        Spacer(Modifier.width(8.dp))
        OutlinedIconButton({ onDeleteClicked(item) }, modifier = Modifier.size(48.dp)) {
            Icon(
                painterResource(R.drawable.ic_delete_outline),
                contentDescription = stringResource(R.string.Delete),
            )
        }
    }
}

@Composable
private fun <T> DeleteDialog(
    toDelete: T,
    dismissDialog: () -> Unit,
    onDeleteClicked: (T) -> Unit,
    @StringRes deleteTitle: Int,
) {
    // Deleting asks in its own face: the first press arms the row, the second deletes. It
    // disarms after six seconds.
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(ARMED_MS)
            armed = false
        }
    }
    EInkDialog(onDismiss = dismissDialog) {
        TextMMD(stringResource(deleteTitle), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        TextMMD(stringResource(R.string.allowlist_delete_message))
        Spacer(Modifier.height(16.dp))
        OutlinedButtonMMD(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            onClick = {
                if (armed) {
                    dismissDialog()
                    onDeleteClicked(toDelete)
                } else {
                    armed = true
                }
            },
        ) {
            TextMMD(
                stringResource(if (armed) R.string.Delete_armed else R.string.Delete),
                fontWeight = if (armed) FontWeight.Bold else null,
            )
        }
        OutlinedButtonMMD(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            onClick = dismissDialog,
        ) {
            TextMMD(stringResource(R.string.cancel))
        }
    }
}

private const val ARMED_MS = 6_000L

@Preview
@Composable
private fun TabPreviewEmpty() {
    AppTheme {
        Surface {
            AccessControlTab(
                accessType = ACCESS_PHONE_NUMBER,
                accessItems = emptyList(),
            )
        }
    }
}

@Preview
@Composable
private fun TabPreviewNumber() {
    AppTheme {
        Surface {
            AccessControlTab(
                accessType = ACCESS_PHONE_NUMBER,
                accessItems = listOf(
                    PhoneNumber(0, "John Doe", "+1 234 567 89"),
                    PhoneNumber(0, "Max Muster", "+49 79 123 456 78"),
                )
            )
        }
    }
}

@Preview
@Composable
private fun TabPreviewPassword() {
    val now = System.currentTimeMillis()
    AppTheme {
        Surface {
            AccessControlTab(
                accessType = ACCESS_SMS_PASS,
                accessItems = listOf(
                    SmsPasswordWithTempPhoneNumbers(
                        smsPassword = SmsPassword(0, "Label 1", "Password 1"),
                        tempPhoneNumbers = listOf(
                            TempPhoneNumber(0, "+1 111 222 33", 42, now - 24 * 60 * 1000L, 0),
                            TempPhoneNumber(0, "+1 444 555 66", 42, now - 7 * 60 * 1000L, 0),
                        ),
                    ),
                    SmsPasswordWithTempPhoneNumbers(
                        smsPassword = SmsPassword(0, "Label 2", "Password 2"),
                        tempPhoneNumbers = emptyList(),
                    ),
                )
            )
        }
    }
}
