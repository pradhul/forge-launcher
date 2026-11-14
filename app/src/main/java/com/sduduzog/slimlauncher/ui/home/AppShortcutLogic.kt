package com.sduduzog.slimlauncher.ui.home

/** Launcher shortcut (App Shortcuts / static + dynamic) shown on the popup. */
data class AppShortcut(
    val id: String,
    val packageName: String,
    val userSerial: Long,
    val label: String
)

object AppShortcutLogic {
    const val MAX = 4

    fun visible(shortcuts: List<AppShortcut>): List<AppShortcut> {
        return shortcuts
            .filter { it.id.isNotBlank() && it.label.isNotBlank() }
            .distinctBy { it.id }
            .take(MAX)
    }
}
