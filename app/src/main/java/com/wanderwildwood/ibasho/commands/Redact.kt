package com.wanderwildwood.ibasho.commands

/** The command words, as the commands themselves name them. */
val COMMAND_KEYWORDS = setOf(
    "bluetooth", "bluetoothscan", "delete", "flash", "gps", "help", "locate", "lock",
    "nodisturb", "ring", "ringermode", "stats",
)

private const val HIDDEN = "***"

/**
 * A command as it may be shown or logged: an access password (the word between the trigger
 * word and the command) and the wipe password (the word after "delete") are replaced by ***.
 *
 * Commands are shown in notifications, which can be read on the lock screen, and kept in the
 * app's log, which can be exported; neither should carry a password.
 *
 * [triggerWord] is the first word when the command came with one (SMS, notification), or null
 * when it did not (from the server, before the trigger word is put in front).
 */
fun redactCommand(raw: String, triggerWord: String?): String {
    val words = raw.trim().split(Regex("\\s+")).toMutableList()
    if (words.isEmpty() || words[0].isEmpty()) return raw

    var i = 0
    if (triggerWord != null && words[0].equals(triggerWord, ignoreCase = true)) {
        i = 1
        // A word that is not a command, right after the trigger word, is a password.
        if (i < words.size && words[i].lowercase() !in COMMAND_KEYWORDS) {
            words[i] = HIDDEN
            i++
        }
    }
    if (i < words.size && words[i].equals("delete", ignoreCase = true) && i + 1 < words.size) {
        words[i + 1] = HIDDEN
    }
    return words.joinToString(" ")
}
