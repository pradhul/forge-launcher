package com.sduduzog.slimlauncher.ui.home

/** Launcher shortcut (App Shortcuts / static + dynamic) shown on the ring. */
data class AppShortcut(
    val id: String,
    val packageName: String,
    val userSerial: Long,
    val label: String
)

sealed class RingOption {
    data class Builtin(val action: RingAction) : RingOption()
    data class Shortcut(val shortcut: AppShortcut) : RingOption()
}

/**
 * Ring contents: App info, Add/Remove home, Uninstall, then up to [MAX]
 * app shortcuts. Hide/Rename/Regenerate are not ring items.
 */
object AppShortcutLogic {
    const val MAX = 4

    val BUILTINS: List<RingOption> = listOf(
        RingOption.Builtin(RingAction.APP_INFO),
        RingOption.Builtin(RingAction.HOME),
        RingOption.Builtin(RingAction.UNINSTALL)
    )

    fun visible(shortcuts: List<AppShortcut>): List<AppShortcut> {
        return shortcuts
            .filter { it.id.isNotBlank() && it.label.isNotBlank() }
            .distinctBy { it.id }
            .take(MAX)
    }

    fun ringOptions(shortcuts: List<AppShortcut> = emptyList()): List<RingOption> {
        return BUILTINS + visible(shortcuts).map { RingOption.Shortcut(it) }
    }

    fun glyph(option: RingOption, pinned: Boolean): String = when (option) {
        is RingOption.Builtin -> when (option.action) {
            RingAction.APP_INFO -> "i"
            RingAction.HOME -> if (pinned) "−" else "+"
            RingAction.UNINSTALL -> "—"
        }
        is RingOption.Shortcut -> option.shortcut.label.trim()
            .firstOrNull { it.isLetterOrDigit() }
            ?.uppercaseChar()
            ?.toString()
            ?: "•"
    }

    fun label(option: RingOption, pinned: Boolean): String = when (option) {
        is RingOption.Builtin -> when (option.action) {
            RingAction.APP_INFO -> "App info"
            RingAction.HOME -> if (pinned) "Remove from home" else "Add to home"
            RingAction.UNINSTALL -> "Uninstall"
        }
        is RingOption.Shortcut -> option.shortcut.label
    }
}
