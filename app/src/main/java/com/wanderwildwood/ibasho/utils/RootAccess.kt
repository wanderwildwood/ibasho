package com.wanderwildwood.ibasho.utils

import android.content.Context
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.Notices
import java.io.OutputStream


class RootAccess {
    companion object {
        @JvmStatic
        fun isRooted(): Boolean {
            var proc: Process? = null
            return try {
                proc = Runtime.getRuntime().exec("su -c exit")
                proc.waitFor() == 0
            } catch (e: Exception) {
                false
            } finally {
                proc?.destroy()
            }
        }

        @JvmStatic
        fun execCommand(context: Context, com: String) {
            var proc: Process? = null
            try {
                val toExec = "$com && echo hi && exit\n"
                proc = Runtime.getRuntime().exec("su")

                val outputStream: OutputStream = proc.outputStream
                outputStream.write(toExec.toByteArray())
                outputStream.flush()
                outputStream.close()

                proc.waitFor()
            } catch (e: Exception) {
                Notices.say(context, context.getString(R.string.perm_root_denied))
                e.printStackTrace()
            } finally {
                proc?.destroy()
            }
        }
    }
}
