package com.wanderwildwood.ibasho.ui

import android.content.Context
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wanderwildwood.ibasho.BuildConfig
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.settings.AboutActivity
import android.net.Uri
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

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

    // A llama under the message, which opens the page a donation goes to. The dialog is a
    // builder rather than Compose, so the row is put together by hand rather than inflated:
    // it is a drawing and three words, and a layout file for that would be a file to keep.
    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val pad = (24 * context.resources.displayMetrics.density).toInt()
        setPadding(pad, pad / 3, pad, pad / 2)
        isClickable = true
        setOnClickListener {
            // The Kompakt may have nothing registered for a web address at all.
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://hotspringsllamas.org/donate/")),
                )
            }
        }
        val size = (22 * context.resources.displayMetrics.density).toInt()
        addView(
            ImageView(context).apply { setImageResource(R.drawable.llama) },
            LinearLayout.LayoutParams(size, size),
        )
        addView(
            TextView(context).apply {
                text = context.getString(R.string.about_llama)
                val gap = (10 * context.resources.displayMetrics.density).toInt()
                setPadding(gap, 0, 0, 0)
            },
        )
    }

    MaterialAlertDialogBuilder(context)
        .setTitle("${context.getString(R.string.app_name)} ${BuildConfig.VERSION_NAME}")
        .setMessage(body)
        .setView(row)
        // The full dependency list is too long for a dialog, but the licences
        // have to be reachable, so it keeps its own screen.
        .setNeutralButton(R.string.about_libraries) { _, _ ->
            context.startActivity(Intent(context, AboutActivity::class.java))
        }
        .setPositiveButton(android.R.string.ok, null)
        .show()
}
