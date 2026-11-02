package com.wanderwildwood.ibasho.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.ibasho.R

/** The address of the app's own source code. */
const val SOURCE_URL = "https://github.com/wanderwildwood/ibasho"

/** What the APK carries that is not this app's own code, and the licence each project declares. */
val LICENCE_COMPONENTS: List<LicenceComponent> = listOf(
    LicenceComponent("FindMyDevice, the app this is forked from", "Nulide", "GPL-3.0-or-later", "https://gitlab.com/Nulide/findmydevice"),
    LicenceComponent("AndroidX (core, activity, lifecycle, room, work, preference and the rest) and Jetpack Compose", "The Android Open Source Project", "Apache-2.0", "https://developer.android.com/jetpack"),
    LicenceComponent("Material Components for Android", "Google", "Apache-2.0", "https://github.com/material-components/material-components-android"),
    LicenceComponent("Compose Multiplatform (animation, foundation, material3, runtime, ui)", "JetBrains", "Apache-2.0", "https://github.com/JetBrains/compose-multiplatform"),
    LicenceComponent("MMD, Mudita Mindful Design", "Mudita", "Apache-2.0", "https://github.com/mudita/MMD"),
    LicenceComponent("Lato typeface, inside MMD", "tyPoland Lukasz Dziedzic", "OFL-1.1", "https://www.latofonts.com/"),
    LicenceComponent("Kotlin standard library and coroutines", "JetBrains", "Apache-2.0", "https://kotlinlang.org"),
    LicenceComponent("Gson", "Google", "Apache-2.0", "https://github.com/google/gson"),
    LicenceComponent("Volley", "The Volley Team", "Apache-2.0", "https://github.com/google/volley"),
    LicenceComponent("OkHttp and Okio", "Square, Inc.", "Apache-2.0", "https://github.com/square/okhttp"),
    LicenceComponent("Retrofit and its Gson converter", "Square, Inc.", "Apache-2.0", "https://github.com/square/retrofit"),
    LicenceComponent("Public Suffix List, bundled with OkHttp", "Mozilla Foundation and contributors", "MPL-2.0", "https://publicsuffix.org/"),
    LicenceComponent("UnifiedPush connector", "S1m", "Apache-2.0", "https://unifiedpush.org/developers/android/"),
    LicenceComponent("Process Phoenix", "Jake Wharton", "Apache-2.0", "https://github.com/JakeWharton/ProcessPhoenix"),
    LicenceComponent("AboutLibraries", "Mike Penz", "Apache-2.0", "https://github.com/mikepenz/AboutLibraries"),
    LicenceComponent("Zip4j", "Srikanth Reddy Lingala", "Apache-2.0", "https://github.com/srikanth-lingala/zip4j"),
    LicenceComponent("Apache Maven Artifact and Plexus Utils, for version numbers", "The Apache Software Foundation", "Apache-2.0", "https://maven.apache.org"),
    LicenceComponent("Bouncy Castle Provider", "The Legion of the Bouncy Castle", "MIT", "https://www.bouncycastle.org/download/bouncy-castle-java/"),
    LicenceComponent("Shizuku API", "Rikka", "MIT", "https://github.com/RikkaApps/Shizuku-API"),
    LicenceComponent("Tink Cryptography API for Android", "Google", "Apache-2.0", "https://github.com/tink-crypto/tink-java"),
    LicenceComponent("Guava ListenableFuture, JSpecify, error-prone and JSR 305 annotations", "Google and contributors", "Apache-2.0", "https://github.com/google/guava"),
    LicenceComponent("SpotBugs Annotations", "The SpotBugs authors", "LGPL-2.1", "https://spotbugs.github.io/"),
)

/** Each distinct licence text once: family id and the raw resource. */
val LICENCE_RAW: List<Pair<String, Int>> = listOf(
    "gpl" to R.raw.licence_gpl3,
    "lgpl" to R.raw.licence_lgpl21,
    "apache" to R.raw.licence_apache2,
    "mpl" to R.raw.licence_mpl2,
    "mit" to R.raw.licence_mit,
    "ofl" to R.raw.licence_ofl,
)

@Composable
fun rememberLicencesData(): LicencesData = LicencesData(
    appName = "Whereabouts",
    sourceUrl = SOURCE_URL,
    licenceLine = stringResource(R.string.licences_licence),
    components = LICENCE_COMPONENTS,
    texts = LICENCE_RAW.map { (id, raw) ->
        LicenceText(id, stringResource(when (id) {
            "gpl" -> R.string.licences_full_gpl
            "lgpl" -> R.string.licences_full_lgpl
            "apache" -> R.string.licences_full_apache
            "mpl" -> R.string.licences_full_mpl
            "mit" -> R.string.licences_full_mit
            "ofl" -> R.string.licences_full_ofl
            else -> R.string.licences_full_head
        }), raw)
    },
)

@Composable
fun rememberLicencesLabels(): LicencesLabels {
    val none = stringResource(R.string.about_no_browser_address, "@@")
    val noText = stringResource(R.string.licences_no_text, "@@")
    return LicencesLabels(
        title = stringResource(R.string.licences_title),
        back = stringResource(R.string.licences_back),
        sourceHeading = stringResource(R.string.licences_source_head),
        noText = { licence -> noText.replace("@@", licence) },
        noBrowser = { address -> none.replace("@@", address) },
    )
}
