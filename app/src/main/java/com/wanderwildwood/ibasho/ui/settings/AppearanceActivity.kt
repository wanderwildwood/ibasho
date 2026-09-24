package com.wanderwildwood.ibasho.ui.settings

import android.app.LocaleConfig
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.databinding.ActivityAppearanceBinding
import com.wanderwildwood.ibasho.ui.FmdActivity
import com.wanderwildwood.ibasho.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import com.wanderwildwood.ibasho.ui.UiUtil.Companion.setupEdgeToEdgeScrollView
import com.wanderwildwood.ibasho.utils.APP_LANGUAGES
import java.util.Locale

class AppearanceActivity : FmdActivity() {

    private lateinit var viewBinding: ActivityAppearanceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityAppearanceBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setupEdgeToEdgeAppBar(findViewById(R.id.appBar))
        setupEdgeToEdgeScrollView(findViewById(R.id.scrollView))
    }

    override fun onResume() {
        super.onResume()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setupLanguageAndroid13AndAfter()
        } else {
            setupLanguageAndroid12AndBefore()
        }
    }

    fun setupLanguageAndroid12AndBefore() {
        // XXX: This returns all locales, even if there is no translations for them
        // val availableLocales = Locale.getAvailableLocales()
        // Thus use a manually maintained list.
        val availableLocales = APP_LANGUAGES.map { Locale.forLanguageTag(it) }
        setupLanguagePicker(availableLocales.toMutableList())
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun setupLanguageAndroid13AndAfter() {
        val supportedLocales = LocaleConfig(this).supportedLocales!!
        val locales = mutableListOf<Locale>()
        for (i in 0..supportedLocales.size() - 1) {
            locales.add(supportedLocales.get(i))
        }
        setupLanguagePicker(locales)
    }

    // Note the offsets for "System default"
    fun setupLanguagePicker(locales: MutableList<Locale>) {
        locales.sortBy { it.displayName }

        val currentLocale = AppCompatDelegate.getApplicationLocales()

        viewBinding.textViewLanguage.text =
            currentLocale.get(0)?.displayLanguage?.capitalize(Locale.getDefault())
                ?: getString(R.string.appearance_language_system_default)

        var checkedIdx = locales.indexOfFirst {
            it.toLanguageTag() == currentLocale.toLanguageTags()
        }
        checkedIdx += 1

        val names = locales.map { it.displayName.capitalize(Locale.getDefault()) }.toMutableList()
        names.add(0, getString(R.string.appearance_language_system_default))

        viewBinding.buttonEditLanguage.setOnClickListener { _ ->
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.appearance_language_choose)
                .setSingleChoiceItems(names.toTypedArray(), checkedIdx) { _, idx ->
                    val newLocale = if (idx == 0) {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        val tag = locales[idx - 1].toLanguageTag()
                        LocaleListCompat.forLanguageTags(tag)
                    }
                    AppCompatDelegate.setApplicationLocales(newLocale)
                }
                .show()
        }
    }
}
