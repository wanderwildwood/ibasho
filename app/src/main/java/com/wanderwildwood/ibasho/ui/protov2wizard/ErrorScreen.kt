package com.wanderwildwood.ibasho.ui.protov2wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import com.wanderwildwood.ibasho.utils.Utils

// A list, because the message can be a whole stack trace. Upstream's red warning icon is
// gone and the heading is bold instead.
@Composable
fun ErrorScreen(
    message: String,
    onCloseClicked: () -> Unit,
) {
    val context = LocalContext.current

    LazyColumnMMD(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextMMD(stringResource(R.string.proto_v2_wizard_error), fontWeight = FontWeight.Bold)
        }
        item {
            TextMMD(message)
        }
        item {
            OutlinedButtonMMD(
                modifier = Modifier.fillMaxWidth(),
                onClick = { Utils.copyToClipboard(context, "", message) },
            ) {
                TextMMD(stringResource(R.string.crashed_copy_log))
            }
        }
        item {
            ButtonMMD(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCloseClicked() },
            ) {
                TextMMD(stringResource(R.string.proto_v2_wizard_step3_button_close))
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Surface {
            ErrorScreen("host not found", {})
        }
    }
}
