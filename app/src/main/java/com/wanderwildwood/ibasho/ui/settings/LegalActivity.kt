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
import com.wanderwildwood.ibasho.ui.PagedColumn
import com.wanderwildwood.ibasho.ui.common.FmdTopAppBar
import com.wanderwildwood.ibasho.ui.theme.AppTheme
import com.wanderwildwood.ibasho.utils.Utils

const val SOURCE_URL = "https://github.com/wanderwildwood/ibasho"

/** One licence the app is shipped under or carries: its name and its full text. */
data class LicenceText(val name: String, val text: String)

/** A bundled component for the "Built with" list: name, version, licence names, address. */
data class Component(val name: String, val version: String, val licences: String, val link: String?)

/**
 * Turn hard-wrapped licence text into paragraphs: a blank line ends one, a single line break
 * inside one is a space. The reader then wraps to the screen's own width.
 */
fun licenceParagraphs(text: String): List<String> =
    text.replace("\r", "").split(Regex("\n\\s*\n"))
        .map { p -> p.lines().joinToString(" ") { it.trim() }.trim() }
        .filter { it.isNotEmpty() }

/** The distinct licences of the libraries, each once, longest-named last, by name. */
fun distinctLicences(libraries: List<Library>): List<LicenceText> =
    libraries.flatMap { it.licenses }
        .mapNotNull { l -> l.licenseContent?.takeIf { it.isNotBlank() }?.let { LicenceText(l.name, it) } }
        .distinctBy { it.name }
        .sortedBy { it.name }

fun componentsOf(libraries: List<Library>): List<Component> =
    libraries.map { lib ->
        Component(
            name = lib.name,
            version = lib.artifactVersion.orEmpty(),
            licences = lib.licenses.joinToString(", ") { it.name }.ifEmpty { "-" },
            link = lib.website ?: lib.scm?.url,
        )
    }.sortedBy { it.name.lowercase() }

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
                                stringResource(R.string.about_licence) + " " +
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
    PagedColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
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
    val context = LocalContext.current
    val libraries by produceLibraries(R.raw.aboutlibraries)
    val gpl = remember { licenceParagraphs(context.resources.openRawResource(R.raw.gpl_3_0).bufferedReader().readText()) }
    val parts = libraries?.libraries.orEmpty()
    val components = remember(parts) { componentsOf(parts) }
    val texts = remember(parts) { distinctLicences(parts) }

    PagedColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
    ) {
        item { Body(stringResource(R.string.licences_source_title), bold = true) }
        item {
            // A clickable address; with no browser the press says so instead of closing the app.
            TextMMD(
                SOURCE_URL,
                fontSize = 16.sp,
                color = Color.Black,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { Utils.openUrl(context, SOURCE_URL) }
                    .padding(vertical = 12.dp),
            )
        }
        item { Body(stringResource(R.string.licences_licence_title), bold = true) }
        item { Body(stringResource(R.string.licences_licence_holder)) }
        item { Body(stringResource(R.string.about_built_on)) }
        items(gpl) { Body(it) }
        item { Body(stringResource(R.string.licences_built_with_title), bold = true) }
        items(components) { c ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(vertical = 6.dp)
                    .let { m -> if (c.link != null) m.clickable { Utils.openUrl(context, c.link) } else m },
            ) {
                TextMMD(
                    (c.name + " " + c.version).trim(),
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black,
                )
                TextMMD(c.licences, fontSize = 14.sp, color = Color.Black)
                if (c.link != null) {
                    TextMMD(c.link, fontSize = 14.sp, color = Color.Black, textDecoration = TextDecoration.Underline)
                }
            }
        }
        for (t in texts) {
            item { Body(t.name, bold = true) }
            items(licenceParagraphs(t.text)) { Body(it) }
        }
    }
}
