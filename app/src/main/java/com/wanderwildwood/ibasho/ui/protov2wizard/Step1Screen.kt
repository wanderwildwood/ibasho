package com.wanderwildwood.ibasho.ui.protov2wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import com.wanderwildwood.ibasho.utils.Utils

// Upstream's layout and behaviour on MMD's components: the list pages rather than scrolls,
// the warnings are bold rather than red, and the icon beside the risk is gone.
@Composable
fun Step1Screen(
    username: String,
    serverUrl: String,
    onNextStepClicked: (String) -> Unit,
) {
    val context = LocalContext.current
    var password by rememberSaveable { mutableStateOf("") }

    LazyColumnMMD(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextMMD(stringResource(R.string.proto_v2_start_text))
        }
        item {
            OutlinedButtonMMD(
                modifier = Modifier.fillMaxWidth(),
                onClick = { Utils.openUrl(context, "https://fmd-foss.org/blog") },
            ) {
                TextMMD(stringResource(R.string.proto_v2_wizard_step1_button_more_info))
            }
        }
        item {
            TextMMD(
                stringResource(R.string.proto_v2_wizard_step1_text_risk),
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            OutlinedButtonMMD(
                modifier = Modifier.fillMaxWidth(),
                onClick = { Utils.openUrl(context, serverUrl) },
            ) {
                TextMMD(stringResource(R.string.proto_v2_wizard_step1_button_open_web, serverUrl))
            }
        }
        item {
            TextMMD(stringResource(R.string.proto_v2_wizard_step1_text_password))
        }
        item {
            TextMMD(stringResource(R.string.Settings_FMD_Server_User_ID) + " " + username)
        }
        item {
            TextFieldMMD(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = { password = it },
                label = { TextMMD(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
        }
        item {
            TextMMD(
                stringResource(R.string.proto_v2_wizard_step1_text_keep_app_open),
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            ButtonMMD(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNextStepClicked(password) },
            ) {
                TextMMD(stringResource(R.string.proto_v2_start_button))
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Surface {
            Step1Screen("alice", "https://server.fmd-foss.org", {})
        }
    }
}
