package com.sduduzog.slimlauncher.ui.home

/**
 * Colours for the borderless pill search field. Uses the active launcher
 * theme (stock, pack, or wallpaper) so the fill is a translucent mix of
 * surface + accent instead of Material grey.
 */
object DrawerSearchStyle {
    const val CONTAINER_ALPHA = 0x29
    const val PLACEHOLDER_ALPHA = 0x80
    const val ICON_ALPHA = 0xB3
    const val ACCENT_MIX = 0.35f

    fun containerColor(surface: Int, accent: Int): Int {
        return withAlpha(mix(surface, accent, ACCENT_MIX), CONTAINER_ALPHA)
    }

    fun textColor(onBackground: Int): Int = onBackground

    fun placeholderColor(onBackground: Int): Int {
        return withAlpha(onBackground, PLACEHOLDER_ALPHA)
    }

    fun iconColor(onBackground: Int): Int {
        return withAlpha(onBackground, ICON_ALPHA)
    }

    fun cursorColor(accent: Int): Int = accent

    fun withAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    fun mix(from: Int, to: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        fun channel(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + ((b - a) * t)).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
