package de.nulide.findmydevice.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
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
import de.nulide.findmydevice.R
import de.nulide.findmydevice.utils.Utils

// Modified version of TraditonalHeader to support multiline description
// See https://github.com/mikepenz/AboutLibraries/blob/develop/aboutlibraries-compose/src/commonMain/kotlin/com/mikepenz/aboutlibraries/ui/compose/variant/traditional/TraditionalHeader.kt
@Composable
@Preview
fun AboutHeader(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val packageName = context.packageName
    val packageVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: ""

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
                Box(
                    modifier = Modifier
                        .size(style.dimensions.headerIconSize)
                        .then(Modifier.clickable {
                            Utils.openUrl(
                                context,
                                "https://gitlab.com/fmd-foss/fmd-android"
                            )
                        }),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painterResource(id = R.drawable.fmd_logo),
                        contentDescription = stringResource(R.string.img_desc_app_icon)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    BasicText(
                        text = stringResource(R.string.app_name),
                        style = style.textStyles.headerTitleTextStyle.copy(color = onBg),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    BasicText(
                        text = stringResource(R.string.app_description),
                        style = style.textStyles.headerTaglineTextStyle.copy(color = subtle),
                        overflow = TextOverflow.Visible
                    )
                }
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
