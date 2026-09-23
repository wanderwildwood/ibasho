package com.wanderwildwood.ibasho.ui.protov2wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.theme.AppTheme

// Two things on the screen, so no list is needed. Upstream's large tick is gone; the sentence
// says it.
@Composable
fun Step3Screen(
    onCloseClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextMMD(stringResource(R.string.proto_v2_wizard_step2_text_intro))

        ButtonMMD(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onCloseClicked() },
        ) {
            TextMMD(stringResource(R.string.proto_v2_wizard_step3_button_close))
        }
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Surface {
            Step3Screen({})
        }
    }
}
