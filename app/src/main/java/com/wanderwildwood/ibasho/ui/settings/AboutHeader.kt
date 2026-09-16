package com.wanderwildwood.ibasho.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.style.m3LibrariesStyle
import com.mudita.mmd.components.chips.AssistChipMMD
import com.mudita.mmd.components.text.TextMMD
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.utils.Utils

// Modified version of TraditionalHeader to support multiline description
// See https://github.com/mikepenz/AboutLibraries/blob/develop/aboutlibraries-compose/src/commonMain/kotlin/com/mikepenz/aboutlibraries/ui/compose/variant/traditional/TraditionalHeader.kt
@Composable
@Preview
fun AboutHeader(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val packageVersion = "${packageInfo.versionName ?: "1.33.7"} (${packageInfo.versionCode})"

    val style = LibraryDefaults.m3LibrariesStyle()

    val colors = style.colors
    val bg = colors.headerBackground
    val onBg = colors.headerOnBackground
    val subtle = colors.headerSubtleContent
    val versionChipBg = colors.tabIdleBackground

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(style.padding.headerPadding),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.about_app_icon_description),
                    modifier = Modifier.size(style.dimensions.headerIconSize),
                )
                Spacer(Modifier.width(16.dp))
                BasicText(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.app_name),
                    style = style.textStyles.headerTitleTextStyle.copy(color = onBg),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(versionChipBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    BasicText(
                        text = packageVersion,
                        style = TextStyle(
                            color = subtle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            BasicText(
                text = stringResource(R.string.app_description),
                style = style.textStyles.headerTaglineTextStyle.copy(color = subtle),
                overflow = TextOverflow.Visible
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AssistChipMMD(
                    onClick = { Utils.openUrl(context, "https://fmd-foss.org/docs/overview") },
                    label = { TextMMD(stringResource(R.string.about_documentation)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.ic_help),
                            contentDescription = null,
                        )
                    }
                )
                AssistChipMMD(
                    onClick = { Utils.openUrl(context, "https://fmd-foss.org/donate") },
                    label = { TextMMD(stringResource(R.string.about_donate)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.ic_volunteer_activism),
                            contentDescription = null,
                        )
                    }
                )
                AssistChipMMD(
                    onClick = { Utils.openUrl(context, "https://github.com/wanderwildwood/ibasho") },
                    label = { TextMMD(stringResource(R.string.about_source_code)) },
                    leadingIcon = {
                        Icon(
                            painterResource(R.drawable.ic_code),
                            contentDescription = null,
                        )
                    }
                )
            }
        }
        // Bottom divider matching design's `borderBottom: 1px solid outlineVariant`
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.headerDivider),
        )
    }
}
