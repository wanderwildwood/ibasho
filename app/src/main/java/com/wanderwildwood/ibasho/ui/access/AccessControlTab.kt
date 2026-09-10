package com.wanderwildwood.ibasho.ui.access

import androidx.annotation.StringRes
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.database.AccessItem
import com.wanderwildwood.ibasho.database.PhoneNumber
import com.wanderwildwood.ibasho.database.SmsPassword
import com.wanderwildwood.ibasho.database.SmsPasswordWithTempPhoneNumbers
import com.wanderwildwood.ibasho.database.TempPhoneNumber
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import com.wanderwildwood.ibasho.utils.Utils

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
        if (accessItems.isEmpty()) {
            Text(stringResource(accessType.hintText, commandKeyword))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.access_permission_hint))
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(accessType.emptyText),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                item {
                    Text(stringResource(accessType.hintText, commandKeyword))
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.access_permission_hint))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }
                items(accessItems) { item ->
                    ItemElement(
                        item,
                        onEditPermissionsClicked = { permissionToEdit = item },
                        onDeleteClicked = { itemToDelete = item })
                }
            }
        }

        // Bottom buttons
        Button(
            onClick = onAddClicked,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(accessType.addText))
        }

        if (onAddSecondaryClicked != null && accessType.addSecondaryText != null) {
            Button(
                onClick = onAddSecondaryClicked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(accessType.addSecondaryText))
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
                val style = MaterialTheme.typography.bodySmall.copy(
                    fontStyle = FontStyle.Italic,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp)
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.access_sms_password_used_by),
                        style = style,
                    )
                    Text(
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
                    Text(
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
                    Text(num.expiryPretty(), style = MaterialTheme.typography.bodySmall)
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { Utils.copyToClipboard(context, "", item.toDisplayLabel()) },
                )
                // Make click area larger
                .padding(vertical = 8.dp),
            text = item.toDisplayLabel(),
        )
        Spacer(Modifier.width(8.dp))
        OutlinedIconButton({ onEditPermissionsClicked(item) }) {
            Icon(
                painterResource(R.drawable.ic_shield),
                contentDescription = stringResource(R.string.fmd_permission_allowed_commands_title),
            )
        }
        Spacer(Modifier.width(8.dp))
        OutlinedIconButton({ onDeleteClicked(item) }) {
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
    AlertDialog(
        onDismissRequest = dismissDialog,
        confirmButton = {
            TextButton(
                onClick = {
                    dismissDialog()
                    onDeleteClicked(toDelete)
                }
            ) {
                Text(stringResource(R.string.Delete))
            }
        },
        dismissButton = {
            TextButton(onClick = dismissDialog) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(deleteTitle)) },
        text = { Text(stringResource(R.string.allowlist_delete_message)) },
    )
}

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
