package com.wanderwildwood.ibasho.ui.common

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wanderwildwood.ibasho.R

/**
 * Every dialog in the views half of the app, drawn as `EInkDialog.kt` draws the Compose half's
 * (STYLE.md): no dimmed backdrop -- on this panel a scrim is a screenful of dithered grey,
 * painted in and painted out again -- white, a 2dp rim of ink and a 12dp corner.
 *
 * Material's builder has no stroke of its own, so the rim is the background drawable.
 */
class EInkAlertDialogBuilder(context: Context) :
    MaterialAlertDialogBuilder(context, R.style.ThemeOverlay_Ibasho_Dialog) {
    init {
        background = ContextCompat.getDrawable(context, R.drawable.dialog_eink)
    }
}
