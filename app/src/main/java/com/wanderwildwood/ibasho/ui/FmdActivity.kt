package com.wanderwildwood.ibasho.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.wanderwildwood.ibasho.R


abstract class FmdActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyTheme()

        // Needs to be called before setContentView.
        // Thus children need to call super.onCreate before setContentView.
        // Needs to be after dynamic colors.
        enableEdgeToEdge()
    }

    override fun onResume() {
        super.onResume()

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
        }
    }

    // Black on white, always. An ink screen has one appearance, so the theme and dynamic-colour
    // choices upstream offered are gone, and a phone that saved "dark" before is light now.
    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
