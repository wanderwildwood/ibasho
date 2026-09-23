package com.wanderwildwood.ibasho.ui.protov2wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import kotlinx.coroutines.delay

// Upstream's three icon-led paragraphs lose their icons: on sixteen greys a small glyph is a
// smudge, and the paragraphs say what the icons did. Deleting the history cannot be undone,
// so that button asks in its own face and disarms itself after four seconds.
@Composable
fun Step2Screen(
    numLocations: Int,
    numPictures: Int,
    onDeleteDataClicked: () -> Unit,
    onMigrateDataClicked: () -> Unit,
) {
    var deleteArmed by remember { mutableStateOf(false) }
    LaunchedEffect(deleteArmed) {
        if (deleteArmed) {
            delay(4000)
            deleteArmed = false
        }
    }

    LazyColumnMMD(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextMMD(stringResource(R.string.proto_v2_wizard_step2_text_intro))
        }
        item {
            TextMMD(stringResource(R.string.proto_v2_wizard_step2_text_data))
        }
        item {
            TextMMD(stringResource(R.string.proto_v2_wizard_step2_text_data_migration))
        }
        item {
            TextMMD(
                stringResource(R.string.proto_v2_wizard_step2_text_num_locations, numLocations)
                        + "\n" +
                        stringResource(R.string.proto_v2_wizard_step2_text_num_pictures, numPictures)
            )
        }
        item {
            OutlinedButtonMMD(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (deleteArmed) {
                        deleteArmed = false
                        onDeleteDataClicked()
                    } else {
                        deleteArmed = true
                    }
                },
            ) {
                TextMMD(
                    stringResource(
                        if (deleteArmed) R.string.proto_v2_wizard_step2_button_delete_data_armed
                        else R.string.proto_v2_wizard_step2_button_delete_data
                    )
                )
            }
        }
        item {
            ButtonMMD(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onMigrateDataClicked() },
            ) {
                TextMMD(stringResource(R.string.proto_v2_wizard_step2_button_migrate_data))
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Surface {
            Step2Screen(50, 2, { }, {})
        }
    }
}
