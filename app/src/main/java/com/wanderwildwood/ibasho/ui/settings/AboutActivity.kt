package com.wanderwildwood.ibasho.ui.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.variant.LibrariesVariant
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryBadges
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.FmdActivity
import com.wanderwildwood.ibasho.ui.common.FmdTopAppBar
import com.wanderwildwood.ibasho.ui.theme.AppTheme

class AboutActivity : FmdActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                AboutHeader(
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
