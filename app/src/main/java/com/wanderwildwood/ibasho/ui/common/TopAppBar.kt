package com.wanderwildwood.ibasho.ui.common

import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.ibasho.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FmdTopAppBar(
    @StringRes title: Int,
    onBackClicked: () -> Unit,
) {
    TopAppBarMMD(
        title = { TextMMD(stringResource(title)) },
        navigationIcon = {
            IconButton(onClick = onBackClicked) {
                Icon(
                    painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.back)
                )
            }
        }
    )
}