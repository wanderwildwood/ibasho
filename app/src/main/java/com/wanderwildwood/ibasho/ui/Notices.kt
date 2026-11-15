package com.wanderwildwood.ibasho.ui

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference

/**
 * A notice stays until the next press. On this panel one on a timer (a toast, a short
 * snackbar) could be gone before it had been read; one that waits is read first and goes on
 * the way to whatever the next press does.
 *
 * Every screen of the app (FmdActivity) hands its touches and Back here, and says when it is in
 * front, so a notice from work that ends as a page closes, or from the background, still has
 * somewhere to be said.
 */
object Notices {
    private var shown = WeakReference<Snackbar>(null)
    private var front = WeakReference<Activity>(null)
    private val main = Handler(Looper.getMainLooper())

    /**
     * Says [text] at the foot of the page in front, until the next press. From any thread.
     * With no page of this app in front, the phone's own toast is all there is.
     */
    @JvmStatic
    fun say(context: Context, text: CharSequence) {
        val app = context.applicationContext
        main.post {
            val root = front.get()?.findViewById<View>(android.R.id.content)
            if (root != null) {
                shown.get()?.dismiss()
                val bar = Snackbar.make(root, text, Snackbar.LENGTH_INDEFINITE)
                bar.view.findViewById<android.widget.TextView>(com.google.android.material.R.id.snackbar_text)
                    ?.maxLines = 6
                shown = WeakReference(bar)
                bar.show()
            } else {
                Toast.makeText(app, text, Toast.LENGTH_LONG).show()
            }
        }
    }

    @JvmStatic
    fun say(context: Context, text: Int) = say(context, context.getString(text))

    /** Called by the activity as it comes to the front (with itself) and goes (with null). */
    @JvmStatic
    fun front(activity: Activity?) {
        front = WeakReference(activity)
    }

    /** A finger lifting anywhere takes the notice away. */
    @JvmStatic
    fun pressed(event: MotionEvent) {
        if (event.actionMasked == MotionEvent.ACTION_UP) shown.get()?.dismiss()
    }

    /** So does Back, or any other button. */
    @JvmStatic
    fun pressed(event: KeyEvent) {
        if (event.action == KeyEvent.ACTION_DOWN) shown.get()?.dismiss()
    }
}
