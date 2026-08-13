package de.nulide.findmydevice.ui.settings

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.m3.style.m3LibrariesStyle
import com.mikepenz.aboutlibraries.ui.compose.style.DefaultLibraryStrings
import com.mikepenz.aboutlibraries.ui.compose.variant.LibrariesVariant
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryBadges
import com.mikepenz.aboutlibraries.ui.compose.variant.traditional.TraditionalHeader
import de.nulide.findmydevice.R
import de.nulide.findmydevice.ui.common.FmdTopAppBar
import de.nulide.findmydevice.ui.theme.AppTheme
import de.nulide.findmydevice.utils.Utils

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Scaffold(
                    topBar = { FmdTopAppBar(R.string.Settings_About) { finish() } }
                ) { innerPadding ->
                    val libraries by produceLibraries(R.raw.aboutlibraries)
                    LibrariesContainer(
                        libraries = libraries,
                        badges = LibraryBadges(
                            author = true,
                            version = true,
                            license = true,
                            funding = false,
                            description = false,
                        ),
                        variant = LibrariesVariant.Traditional,
                        header = {
                            item {
                                val context = LocalContext.current
                                TraditionalHeader(
                                    title = getString(R.string.app_name),
                                    tagline = getString(R.string.app_description),
                                    versionLabel = packageManager.getPackageInfo(
                                        packageName,
                                        0
                                    ).versionName,
                                    strings = DefaultLibraryStrings,
                                    appIcon = {
                                        val drawable =
                                            packageManager.getApplicationIcon(applicationInfo.packageName)
                                        Image(
                                            drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
                                                .asImageBitmap(),
                                            ""
                                        )
                                    },
                                    showSearch = false,
                                    style = LibraryDefaults.m3LibrariesStyle(),
                                    onIconClick = {
                                        Utils.openUrl(
                                            context,
                                            "https://gitlab.com/fmd-foss/fmd-android"
                                        )
                                    },
                                    modifier = Modifier.padding(innerPadding),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
