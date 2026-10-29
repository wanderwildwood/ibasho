package com.wanderwildwood.ibasho.ui

import android.content.Context
import android.content.Intent
import com.wanderwildwood.ibasho.ui.common.EInkAlertDialogBuilder
import com.wanderwildwood.ibasho.BuildConfig
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.settings.LegalActivity
import android.net.Uri
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.graphics.Typeface
import com.wanderwildwood.ibasho.ui.paging.PagingScrollView

// TODO: this repository is not published yet. The link must resolve before release.
// No scheme: it has to fit one line at 480px, and a reader can type it.
const val SITE_URL = "wanderthe.dev"

/**
 * About is a dialog reached by the "i" in the top right, not a settings row:
 * it is not a setting, and it is the one thing a stranger looks for before
 * trusting an app.
 *
 * The order is fixed — name and version, what it sends, the licence named in
 * full, what is bundled and whose it is, then the source.
 */
fun showAboutDialog(context: Context) {
    val density = context.resources.displayMetrics.density
    val pad = (24 * density).toInt()

    // Plain text in ink: a heading in bold, a paragraph under it.
    fun heading(text: Int) = TextView(context).apply {
        setText(text)
        setTextColor(Color.BLACK)
        textSize = 18f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, (12 * density).toInt(), 0, (4 * density).toInt())
    }

    fun paragraph(text: String) = TextView(context).apply {
        this.text = text
        setTextColor(Color.BLACK)
        textSize = 16f
        setPadding(0, 0, 0, (8 * density).toInt())
    }

    // An address, opened in whatever the phone has for web pages. A Kompakt may have
    // nothing, so that says so rather than the press doing nothing.
    val open = { address: String ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(address)))
        }.onFailure {
            Toast.makeText(context, R.string.about_no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    // Under the message, one line: the site's name, which opens the site, then a llama
    // which opens the page a donation goes to. The dialog is a
    // builder rather than Compose, so the row is put together by hand rather than inflated:
    // it is a name, a drawing and three words, and a layout file for that would be a file to keep.
    val llama = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        setOnClickListener {
            // Straight to the checkout. The Donate button on the site only leads here
            // anyway, so the page in between is a press the reader does not need. The short
            // square.link form, not the long checkout.square.site address it redirects to --
            // the short one is what the site itself links to, so a regenerated checkout
            // follows it and a published app does not break.
            open("https://square.link/u/AGu8oT10")
        }
        val size = (22 * density).toInt()
        addView(
            ImageView(context).apply { setImageResource(R.drawable.llama) },
            LinearLayout.LayoutParams(size, size),
        )
        addView(
            TextView(context).apply {
                text = context.getString(R.string.about_llama)
                val gap = (6 * density).toInt()
                setPadding(gap, 0, 0, 0)
            },
        )
    }
    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, pad / 3, 0, pad / 2)
        addView(
            TextView(context).apply {
                text = SITE_URL
                isClickable = true
                setOnClickListener { open("https://$SITE_URL") }
                val gap = (4 * density).toInt()
                setPadding(0, gap, 0, gap)
            },
        )
        addView(
            llama,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { marginStart = (6 * density).toInt() },
        )
    }

    // Three doors, no rules between them: space sets them apart. Each opens its own page.
    fun door(label: Int, page: String) = TextView(context).apply {
        setText(label)
        setTextColor(Color.BLACK)
        textSize = 18f
        gravity = Gravity.CENTER_VERTICAL
        minHeight = (56 * density).toInt()
        isClickable = true
        setOnClickListener { context.startActivity(LegalActivity.intent(context, page)) }
    }
    val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(pad, 0, pad, 0)
        addView(door(R.string.about_privacy_title, LegalActivity.PAGE_PRIVACY))
        addView(door(R.string.about_terms_title, LegalActivity.PAGE_TERMS))
        addView(door(R.string.about_licences, LegalActivity.PAGE_LICENCES))
        addView(row)
    }
    val page = PagingScrollView(context).apply {
        isVerticalScrollBarEnabled = false
        addView(column)
    }

    EInkAlertDialogBuilder(context)
        .setTitle(context.getString(R.string.about_title, BuildConfig.VERSION_NAME))
        .setView(page)
        .setPositiveButton(android.R.string.ok, null)
        .show()
}
