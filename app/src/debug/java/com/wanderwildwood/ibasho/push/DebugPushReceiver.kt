package com.wanderwildwood.ibasho.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wanderwildwood.ibasho.services.unregisterWithUnifiedPush
import org.unifiedpush.android.connector.UnifiedPush

/**
 * Debug builds only. Drives the push choice from adb, so the built-in distributor can be
 * tested on an emulator without a server account:
 *
 *   adb shell am broadcast -n com.wanderwildwood.ibasho/.push.DebugPushReceiver --es do <what>
 *
 * start    act as if there were an account, and use the built-in distributor
 * builtin  switch to the built-in distributor
 * another  switch to another app (the only one installed, or none)
 * stop     stop acting as if there were an account, and unregister
 * status   log the current state
 */
class DebugPushReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = EmbeddedPushStore(context)
        when (intent.getStringExtra("do")) {
            "start" -> {
                store.debugWithoutAccount = true
                PushChoice.useBuiltIn(context)
            }

            "builtin" -> PushChoice.useBuiltIn(context)
            "another" -> PushChoice.useAnotherApp(context, PushChoice.otherApps(context).firstOrNull())
            "stop" -> {
                store.debugWithoutAccount = false
                unregisterWithUnifiedPush(context)
                EmbeddedPush.sync(context)
            }
        }
        Log.i(
            TAG, "choice=${PushChoice.get(context)} " +
                    "saved=${UnifiedPush.getSavedDistributor(context)} " +
                    "acked=${UnifiedPush.getAckDistributor(context)} " +
                    "others=${PushChoice.otherApps(context)} " +
                    "channels=${store.channels()} " +
                    "service=${PushConnectionService.instance != null} " +
                    "open=${PushConnectionService.instance?.isOpen}"
        )
    }

    companion object {
        private const val TAG = "DebugPush"
    }
}
