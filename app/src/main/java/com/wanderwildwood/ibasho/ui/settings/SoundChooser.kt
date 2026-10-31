package com.wanderwildwood.ibasho.ui.settings

import android.content.Context
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.wanderwildwood.ibasho.R

/**
 * Chooses one of the phone's own sounds in a list of the app's own: a press chooses it and plays
 * a few seconds of it, and Done closes the list. Android's picker is not used, because on a
 * Kompakt it has no OK button and a sound picked there is lost on going back. The marks are plain
 * characters, not Android's radio buttons, whose dot animates in slowly on an e-ink panel.
 */
object SoundChooser {
    private const val PREVIEW_MS = 5_000L

    /**
     * [current] null is silent; [onPick] is called at each press with the chosen sound, null for
     * silent. [usage] is the audio stream the preview plays on: media by default, so it can be heard
     * when the phone's ring and notification volumes are turned down.
     */
    fun show(
        context: Context,
        ringtoneType: Int,
        title: CharSequence?,
        current: Uri?,
        showSilent: Boolean = true,
        usage: Int = AudioAttributes.USAGE_MEDIA,
        onPick: (Uri?) -> Unit,
    ) {
        val titles = ArrayList<String>()
        val uris = ArrayList<Uri?>()
        if (showSilent) {
            titles.add(context.getString(R.string.sound_chooser_silent))
            uris.add(null)
        }
        titles.add(context.getString(R.string.sound_chooser_default))
        uris.add(RingtoneManager.getDefaultUri(ringtoneType))
        val manager = RingtoneManager(context).apply { setType(ringtoneType) }
        manager.cursor?.use { c ->
            while (c.moveToNext()) {
                titles.add(c.getString(RingtoneManager.TITLE_COLUMN_INDEX))
                uris.add(manager.getRingtoneUri(c.position))
            }
        }

        var chosen = uris.indexOfFirst { it == current }.let { if (it >= 0) it else if (current == null && showSilent) 0 else -1 }
        var playing: Ringtone? = null
        val handler = Handler(Looper.getMainLooper())
        val stop = Runnable { playing?.stop() }
        fun preview(uri: Uri?) {
            handler.removeCallbacks(stop)
            playing?.stop()
            playing = uri?.let { u ->
                RingtoneManager.getRingtone(context, u)?.apply {
                    audioAttributes = AudioAttributes.Builder().setUsage(usage).build()
                    play()
                }
            }
            if (playing != null) handler.postDelayed(stop, PREVIEW_MS)
        }

        val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, titles) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row = super.getView(position, convertView, parent) as TextView
                val on = position == chosen
                row.text = (if (on) "●   " else "○   ") + titles[position]
                row.setTypeface(null, if (on) Typeface.BOLD else Typeface.NORMAL)
                return row
            }
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(title)
            .setAdapter(adapter, null)
            .setPositiveButton(R.string.sound_chooser_done, null)
            .setOnDismissListener { handler.removeCallbacks(stop); playing?.stop() }
            .create()
        dialog.window?.setWindowAnimations(0)
        dialog.show()
        // AlertDialog closes on a press of a list item; this list stays open until Done.
        dialog.listView.setOnItemClickListener { _, _, which, _ ->
            chosen = which
            adapter.notifyDataSetChanged()
            onPick(uris[which])
            preview(uris[which])
        }
    }
}
