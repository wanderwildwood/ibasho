package com.wanderwildwood.ibasho.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.permissions.LocationPermission


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
        Notices.front(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            // A way back on every screen but the first, as the Compose screens have one: on
            // the Kompakt the hardware back is easy to forget is there.
            if (this !is MainActivity) {
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                toolbar.setNavigationOnClickListener { finish() }
            }
        }
    }

    override fun onPause() {
        Notices.front(null)
        super.onPause()
    }

    // A Grant press Android refuses without showing anything goes to the app's settings instead.
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        LocationPermission.onResult(this, requestCode, permissions, grantResults)
    }

    // A notice stays until the next press (see Notices): the presses are handed to it here.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        Notices.pressed(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        Notices.pressed(event)
        return super.dispatchKeyEvent(event)
    }

    // Black on white, always. An ink screen has one appearance, so the theme and dynamic-colour
    // choices upstream offered are gone, and a phone that saved "dark" before is light now.
    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
