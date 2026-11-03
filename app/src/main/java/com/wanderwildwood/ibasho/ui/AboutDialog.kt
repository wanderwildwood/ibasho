package com.wanderwildwood.ibasho.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.wanderwildwood.ibasho.BuildConfig
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.theme.AppTheme

/**
 * This app's About: only the words and the address. The dialog, its one Close button, the
 * Licenses row, the links and the llama are the shared HouseAbout.kt, the same in every app.
 * It is reached by the "i" in the top right, not a settings row.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val data = AboutData(
        title = stringResource(R.string.about_title, BuildConfig.VERSION_NAME),
        sections = listOf(
            AboutSection(stringResource(R.string.about_privacy_title), stringResource(R.string.about_sends)),
            AboutSection(null, stringResource(R.string.about_sends_network)),
            AboutSection(null, stringResource(R.string.about_privacy_permissions)),
            AboutSection(null, stringResource(R.string.about_privacy_close)),
            AboutSection(
                stringResource(R.string.about_terms_title),
                stringResource(R.string.about_terms_free) + " " + stringResource(R.string.about_terms_warranty),
            ),
            AboutSection(null, stringResource(R.string.about_built_on)),
        ),
        sourceUrl = SOURCE_URL,
    )
    val labels = AboutLabels(
        close = stringResource(R.string.about_close),
        licences = stringResource(R.string.licences_title),
        feedTheLlamas = stringResource(R.string.about_feed_the_llamas),
        noBrowser = { address -> context.getString(R.string.about_no_browser_address, address) },
    )
    HouseAboutDialog(data, labels, R.drawable.llama, onDismiss) { modifier, back ->
        LicencesScreen(rememberLicencesData(), rememberLicencesLabels(), onBack = back, modifier = modifier)
    }
}

/** Opens About over a View-based screen (the main screen is not Compose). */
fun showAboutDialog(activity: FragmentActivity) {
    if (activity.supportFragmentManager.findFragmentByTag(AboutDialogFragment.TAG) != null) return
    AboutDialogFragment().show(activity.supportFragmentManager, AboutDialogFragment.TAG)
}

/** An empty, transparent shell; the dialog itself is the Compose one above. */
class AboutDialogFragment : DialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { AppTheme { AboutDialog(onDismiss = { dismissAllowingStateLoss() }) } }
        }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setLayout(1, 1)
        }
    }

    companion object {
        const val TAG = "about"
    }
}
