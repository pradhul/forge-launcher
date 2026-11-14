package com.sduduzog.slimlauncher.ui.home

/**
 * Long-press menu: shortcuts first, then App info, Add/Remove home,
 * Uninstall when allowed. No Cancel — outside tap dismisses.
 */
enum class AppPopupIcon { PRIVATE, NEW_TAB, SHORTCUT, INFO, ADD_HOME, REMOVE_HOME, UNINSTALL }

sealed class AppPopupAction {
    data class Shortcut(val shortcut: AppShortcut) : AppPopupAction()
    object AppInfo : AppPopupAction()
    object Home : AppPopupAction()
    object Uninstall : AppPopupAction()
}

data class AppPopupItem(
    val action: AppPopupAction,
    val label: String,
    val icon: AppPopupIcon
)

object AppPopupLogic {
    const val WIDTH_DP = 248
    const val ROW_DP = 48
    const val RADIUS_DP = 20
    const val GAP_DP = 10

    /** ApplicationInfo.FLAG_SYSTEM */
    const val FLAG_SYSTEM = 1

    fun items(
        shortcuts: List<AppShortcut>,
        onHome: Boolean,
        canUninstall: Boolean
    ): List<AppPopupItem> {
        val out = ArrayList<AppPopupItem>(AppShortcutLogic.MAX + 3)
        for (shortcut in AppShortcutLogic.visible(shortcuts)) {
            out += AppPopupItem(
                AppPopupAction.Shortcut(shortcut),
                shortcut.label,
                shortcutIcon(shortcut.label)
            )
        }
        out += AppPopupItem(AppPopupAction.AppInfo, "App info", AppPopupIcon.INFO)
        out += if (onHome) {
            AppPopupItem(AppPopupAction.Home, "Remove from home", AppPopupIcon.REMOVE_HOME)
        } else {
            AppPopupItem(AppPopupAction.Home, "Add to home", AppPopupIcon.ADD_HOME)
        }
        if (canUninstall) {
            out += AppPopupItem(AppPopupAction.Uninstall, "Uninstall", AppPopupIcon.UNINSTALL)
        }
        return out
    }

    fun shortcutIcon(label: String): AppPopupIcon {
        val n = label.lowercase()
        return when {
            "private" in n || "incognito" in n -> AppPopupIcon.PRIVATE
            "tab" in n && "new" in n || n == "new tab" -> AppPopupIcon.NEW_TAB
            else -> AppPopupIcon.SHORTCUT
        }
    }

    fun canUninstall(applicationFlags: Int): Boolean = applicationFlags and FLAG_SYSTEM == 0

    /**
     * Top-left of the popup. Prefer above the icon, horizontally centred,
     * then clamp so the card stays on screen.
     */
    fun place(
        anchorX: Float,
        anchorY: Float,
        popupW: Float,
        popupH: Float,
        screenW: Float,
        screenH: Float,
        margin: Float,
        gap: Float = GAP_DP.toFloat()
    ): Pair<Float, Float> {
        if (popupW <= 0f || popupH <= 0f || screenW <= 0f || screenH <= 0f) {
            return margin to margin
        }
        var x = anchorX - popupW / 2f
        var y = anchorY - popupH - gap
        if (y < margin) y = anchorY + gap
        val maxX = (screenW - popupW - margin).coerceAtLeast(margin)
        val maxY = (screenH - popupH - margin).coerceAtLeast(margin)
        x = x.coerceIn(margin, maxX)
        y = y.coerceIn(margin, maxY)
        return x to y
    }
}
