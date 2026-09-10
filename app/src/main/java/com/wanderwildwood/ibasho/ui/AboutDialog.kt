package com.wanderwildwood.ibasho.ui

import android.content.Context
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wanderwildwood.ibasho.BuildConfig
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.settings.AboutActivity

// TODO: this repository is not published yet. The link must resolve before release.
// No scheme: it has to fit one line at 480px, and a reader can type it.
const val SOURCE_URL = "github.com/wanderwildwood/ibasho"

/**
 * About is a dialog reached by the "i" in the top right, not a settings row:
 * it is not a setting, and it is the one thing a stranger looks for before
 * trusting an app.
 *
 * The order is fixed — name and version, what it sends, the licence named in
 * full, what is bundled and whose it is, then the source.
 */
fun showAboutDialog(context: Context) {
    val body = buildString {
        append(context.getString(R.string.about_sends))
        append("\n\n")
        append(context.getString(R.string.about_sends_network))
        append("\n\n")
        // The last three are short facts, not paragraphs: consecutive lines
        // keep the licence, the attribution and the source all above the fold.
        append(context.getString(R.string.about_licence))
        append("\n")
        append(context.getString(R.string.about_built_on))
        append("\n")
        append(context.getString(R.string.about_source, SOURCE_URL))
    }

    MaterialAlertDialogBuilder(context)
        .setTitle("${context.getString(R.string.app_name)} ${BuildConfig.VERSION_NAME}")
        .setMessage(body)
        // The full dependency list is too long for a dialog, but the licences
        // have to be reachable, so it keeps its own screen.
        .setNeutralButton(R.string.about_libraries) { _, _ ->
            context.startActivity(Intent(context, AboutActivity::class.java))
        }
        .setPositiveButton(android.R.string.ok, null)
        .show()
}
