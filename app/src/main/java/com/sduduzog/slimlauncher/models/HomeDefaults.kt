package com.sduduzog.slimlauncher.models

import com.sduduzog.slimlauncher.data.model.App

/**
 * First-run home pins. Picks common phone apps when present, then fills
 * remaining slots from whatever else is installed so home is never empty.
 */
object HomeDefaults {

    data class Role(
        val id: String,
        val packages: List<String>,
        val nameHints: List<String>
    )

    val ROLES: List<Role> = listOf(
        Role(
            id = "phone",
            packages = listOf(
                "com.google.android.dialer",
                "com.android.dialer",
                "com.samsung.android.dialer",
                "com.android.phone"
            ),
            nameHints = listOf("phone", "dialer", "call")
        ),
        Role(
            id = "messages",
            packages = listOf(
                "com.google.android.apps.messaging",
                "com.google.android.apps.messagings",
                "com.android.mms",
                "com.samsung.android.messaging"
            ),
            nameHints = listOf("messages", "messaging", "sms", "text")
        ),
        Role(
            id = "browser",
            packages = listOf(
                "com.android.chrome",
                "com.chrome.beta",
                "com.chrome.dev",
                "com.google.android.apps.chrome",
                "com.brave.browser"
            ),
            nameHints = listOf("chrome", "browser")
        ),
        Role(
            id = "camera",
            packages = listOf(
                "com.google.android.GoogleCamera",
                "com.google.android.apps.cameralite",
                "com.android.camera2",
                "com.android.camera",
                "com.samsung.android.camera"
            ),
            nameHints = listOf("camera")
        ),
        Role(
            id = "settings",
            packages = listOf("com.android.settings"),
            nameHints = listOf("settings")
        ),
        Role(
            id = "store",
            packages = listOf("com.android.vending"),
            nameHints = listOf("play store", "playstore", "google play")
        )
    )

    fun pick(installed: List<App>, limit: Int = 8): List<App> {
        if (installed.isEmpty() || limit <= 0) return emptyList()
        val remaining = installed.toMutableList()
        val picked = mutableListOf<App>()
        for (role in ROLES) {
            if (picked.size >= limit) break
            val match = remaining.firstOrNull { app ->
                role.packages.any { it.equals(app.packageName, ignoreCase = true) }
            } ?: remaining.firstOrNull { app ->
                val name = app.appName.lowercase()
                role.nameHints.any { hint -> name == hint || name.contains(hint) }
            }
            if (match != null) {
                picked.add(match)
                remaining.removeAll {
                    it.packageName == match.packageName && it.userSerial == match.userSerial
                }
            }
        }
        if (picked.size < limit) {
            picked.addAll(remaining.take(limit - picked.size))
        }
        return picked
    }
}
