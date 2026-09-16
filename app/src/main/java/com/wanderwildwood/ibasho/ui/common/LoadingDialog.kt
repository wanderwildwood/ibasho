package com.wanderwildwood.ibasho.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import com.wanderwildwood.ibasho.ui.theme.EInkDialog

@Composable
fun LoadingDialog() {
    // The ring that used to turn beside this word is gone. On E Ink it is a redraw a
    // frame to say what the word says once, and the panel takes a quarter-second to draw
    // anything at all. The dialog is the shared one, so it looks like every other.
    EInkDialog(onDismiss = {}) {
        TextMMD(stringResource(R.string.loading))
    }
}

@Preview
@Composable
fun LoadingDialogPreview() {
    AppTheme {
        LoadingDialog()
    }
}
