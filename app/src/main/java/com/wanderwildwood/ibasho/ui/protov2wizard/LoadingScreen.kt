package com.wanderwildwood.ibasho.ui.protov2wizard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.theme.AppTheme

// A word where upstream has a turning ring, as LoadingDialog does: on E Ink the ring is a
// redraw a frame to say what the word says once.
@Composable
fun LoadingScreen(
    @StringRes text: Int = R.string.loading,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextMMD(stringResource(text))
    }
}

@Preview
@Composable
private fun Preview() {
    AppTheme {
        Surface {
            LoadingScreen()
        }
    }
}
