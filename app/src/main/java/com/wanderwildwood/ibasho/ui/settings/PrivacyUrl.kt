package com.wanderwildwood.ibasho.ui.settings

/**
 * Where a server's privacy policy page is, or null when the server address is not https: the
 * policy is only ever shown over a secure connection (the app forbids clear text anyway), so an
 * http address is refused up front rather than left to fail as a blank page.
 */
fun privacyPolicyUrl(serverUrl: String): String? {
    val base = serverUrl.trim().removeSuffix("/")
    if (!base.startsWith("https://", ignoreCase = true) || base.length <= "https://".length) return null
    return "$base/privacy?embedded=true"
}
