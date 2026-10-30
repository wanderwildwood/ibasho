package com.wanderwildwood.ibasho.ui.access

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.checkbox.CheckboxMMD
import com.wanderwildwood.ibasho.ui.PagedColumn
import com.wanderwildwood.ibasho.ui.theme.DialogButtons
import com.wanderwildwood.ibasho.ui.theme.EInkDialog
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.commands.Command
import com.wanderwildwood.ibasho.commands.FmdPermission
import com.wanderwildwood.ibasho.commands.availableCommandsWithoutHelp
import com.wanderwildwood.ibasho.commands.hasPermission
import com.wanderwildwood.ibasho.commands.setPermission

@Composable
fun FmdPermissionDialog(
    initialPermission: Long,
    onCancelClicked: () -> Unit = {},
    onSaveClicked: (Long) -> Unit = {},
) {
    var permission by rememberSaveable { mutableLongStateOf(initialPermission) }
    val commands = availableCommandsWithoutHelp(LocalContext.current)

    EInkDialog(onCancelClicked) {
        TextMMD(
            text = stringResource(R.string.fmd_permission_allowed_commands_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        // Column of commands/permissions
        // Weighted so the buttons below keep their room when the list runs past the dialog.
        PagedColumn(
            modifier = Modifier.weight(1f, fill = false).padding(top = 16.dp, bottom = 16.dp)
        ) {
            items(commands) { command ->
                CommandRow(
                    command,
                    checked = permission.hasPermission(command.permission),
                    onCheckedChange = { checked ->
                        permission = permission.setPermission(command.permission, checked)
                    },
                )
            }
        }

        // Button row
        DialogButtons(
            cancel = stringResource(R.string.cancel),
            confirm = stringResource(R.string.save),
            onCancel = onCancelClicked,
            onConfirm = { onSaveClicked(permission) },
        )
    }
}

@Composable
private fun CommandRow(
    command: Command,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    // The word comes first and is never broken ("bluetoothscan" used to wrap mid-word beside
    // an icon): no icon, little padding, one line. The whole row is the touch target.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = { onCheckedChange(!checked) }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckboxMMD(
            checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(end = 12.dp),
        )
        TextMMD(
            modifier = Modifier.weight(1f),
            text = command.keyword,
            style = MaterialTheme.typography.bodyLarge,
            softWrap = false,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun FmdPermissionDialogPreview() {
    Surface {
        FmdPermissionDialog(FmdPermission.ALL)
    }
}
