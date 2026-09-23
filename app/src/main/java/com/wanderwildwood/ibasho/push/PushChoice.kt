package com.wanderwildwood.ibasho.push

import android.content.Context
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.services.unregisterWithUnifiedPush
import com.wanderwildwood.ibasho.utils.log
import org.unifiedpush.android.connector.INSTANCE_DEFAULT
import org.unifiedpush.android.connector.UnifiedPush

/**
 * Which push distributor FMD Server wakes this phone through: the one built in, or another
 * UnifiedPush app such as Sunup or ntfy.
 */
object PushChoice {
    const val BUILT_IN = "built_in"
    const val ANOTHER_APP = "another_app"

    private const val TAG = "PushChoice"

    fun get(context: Context): String {
        val value = SettingsRepository.getInstance(context).get(Settings.SET_FMDSERVER_PUSH_SOURCE) as String
        return if (value == ANOTHER_APP) ANOTHER_APP else BUILT_IN
    }

    private fun set(context: Context, value: String) =
        SettingsRepository.getInstance(context).set(Settings.SET_FMDSERVER_PUSH_SOURCE, value)

    /**
     * An install from before the built-in distributor existed, already registered with another
     * app, keeps that app: it was chosen, and switching it silently would be the app deciding.
     */
    fun migrate(context: Context) {
        val settings = SettingsRepository.getInstance(context)
        if ((settings.get(Settings.SET_FMDSERVER_PUSH_SOURCE) as String).isNotEmpty()) return
        val current = UnifiedPush.getAckDistributor(context)
        val value = if (current != null && current != context.packageName) ANOTHER_APP else BUILT_IN
        context.log().i(TAG, "Push choice set to $value (current distributor: $current)")
        set(context, value)
    }

    /** Installed UnifiedPush apps other than this one. */
    fun otherApps(context: Context): List<String> =
        UnifiedPush.getDistributors(context).filter { it != context.packageName }

    /** Push is set up as chosen, or will be as soon as the push server answers. */
    fun isSetUp(context: Context): Boolean {
        val acked = UnifiedPush.getAckDistributor(context)
        if (!acked.isNullOrEmpty()) return true
        return get(context) == BUILT_IN && UnifiedPush.getSavedDistributor(context) == context.packageName
    }

    fun useBuiltIn(context: Context) {
        set(context, BUILT_IN)
        switchTo(context, context.packageName)
    }

    /**
     * Chooses another app. With no [distributor] (none installed) the built-in one still
     * stops: the choice is kept, and the screen says plainly that nothing will arrive.
     */
    fun useAnotherApp(context: Context, distributor: String?) {
        set(context, ANOTHER_APP)
        if (distributor == null) {
            context.log().w(TAG, "Another app chosen, and none is installed")
            unregisterWithUnifiedPush(context)
            EmbeddedPush.sync(context)
        } else {
            switchTo(context, distributor)
        }
    }

    /**
     * Unregisters from whichever distributor holds the registration now, then registers with
     * [distributor]. The new endpoint goes to FMD Server when the distributor answers.
     */
    private fun switchTo(context: Context, distributor: String) {
        context.log().i(TAG, "Switching push to $distributor")
        unregisterWithUnifiedPush(context)
        UnifiedPush.saveDistributor(context, distributor)
        UnifiedPush.register(context, INSTANCE_DEFAULT, null, null)
        EmbeddedPush.sync(context)
    }

    /**
     * Called on every start while there is a server account and nothing registered: needs no
     * screen, so it runs from the background too. Returns false when push cannot be set up
     * without the user, which is the case the "Missing UnifiedPush" warning is for.
     */
    fun registerIfPossible(context: Context): Boolean {
        return when {
            get(context) == BUILT_IN -> {
                switchTo(context, context.packageName)
                true
            }

            otherApps(context).size == 1 -> {
                switchTo(context, otherApps(context).first())
                true
            }

            else -> false
        }
    }
}
