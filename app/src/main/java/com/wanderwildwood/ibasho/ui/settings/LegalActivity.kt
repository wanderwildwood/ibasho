package com.wanderwildwood.ibasho.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.FmdActivity
import com.wanderwildwood.ibasho.ui.LicencesScreen
import com.wanderwildwood.ibasho.ui.PagedColumn
import com.wanderwildwood.ibasho.ui.rememberLicencesData
import com.wanderwildwood.ibasho.ui.rememberLicencesLabels
import com.wanderwildwood.ibasho.ui.common.FmdTopAppBar
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import com.wanderwildwood.ibasho.utils.Utils

/**
 * Privacy, Terms and Licences, the three pages About leads to. All three are plain text in a
 * list that turns by whole pages; none draws a rule between its parts.
 */
class LegalActivity : FmdActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = intent.getStringExtra(EXTRA_PAGE) ?: PAGE_LICENCES
        setContent {
            AppTheme {
                @StringRes val title = when (page) {
                    PAGE_PRIVACY -> R.string.about_privacy_title
                    PAGE_TERMS -> R.string.about_terms_title
                    else -> R.string.about_licences
                }
                Scaffold(topBar = { FmdTopAppBar(title) { finish() } }) { inner ->
                    when (page) {
                        PAGE_PRIVACY -> Words(
                            listOf(
                                stringResource(R.string.about_sends),
                                stringResource(R.string.about_sends_network),
                                stringResource(R.string.about_privacy_permissions),
                                stringResource(R.string.about_privacy_close),
                            ),
                            Modifier.padding(inner),
                        )
                        PAGE_TERMS -> Words(
                            listOf(
                                stringResource(R.string.about_terms_free) + " " +
                                    stringResource(R.string.about_terms_warranty),
                                stringResource(R.string.about_built_on),
                            ),
                            Modifier.padding(inner),
                        )
                        else -> Licences(Modifier.padding(inner))
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_PAGE = "page"
        const val PAGE_PRIVACY = "privacy"
        const val PAGE_TERMS = "terms"
        const val PAGE_LICENCES = "licences"

        fun intent(context: Context, page: String): Intent =
            Intent(context, LegalActivity::class.java).putExtra(EXTRA_PAGE, page)
    }
}

@Composable
private fun Words(paragraphs: List<String>, modifier: Modifier) {
    PagedColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 12.dp)) {
        items(paragraphs) { Body(it) }
    }
}

@Composable
private fun Body(text: String, bold: Boolean = false) {
    TextMMD(
        text,
        fontSize = 16.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = Color.Black,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    )
}

@Composable
private fun Licences(modifier: Modifier) {
    // The shared Licences page; the top bar above it already says what it is and goes back.
    LicencesScreen(
        rememberLicencesData(),
        rememberLicencesLabels(),
        onBack = {},
        modifier = modifier.fillMaxSize().padding(start = 20.dp),
        showBackButton = false,
        showTitle = false,
    )
}
