package com.sduduzog.slimlauncher.market

import com.sduduzog.slimlauncher.icons.ContrastPicker
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.icons.ThemedIconPalette

data class MarketCardColors(
    val card: Int,
    val text: Int,
    val muted: Int,
    val chipUpBg: Int,
    val chipUpFg: Int,
    val chipDownBg: Int,
    val chipDownFg: Int,
    val chipFlatBg: Int,
    val chipFlatFg: Int,
    val accent: Int
)

object MarketThemeLogic {
    private const val UP_GREEN = 0xFF1B8A4A.toInt()
    private const val DOWN_RED = 0xFFC62828.toInt()

    fun colors(theme: PackTheme): MarketCardColors {
        val base = if (theme.darkSurface) ThemedIconPalette.tone(theme.accent, 18.0, 1.2, 0.5)
        else ThemedIconPalette.tone(theme.accent, 92.0, 1.2, 0.4)
        val card = (base and 0x00FFFFFF) or (0xE6 shl 24)
        val opaque = card or 0xFF000000.toInt()
        val text = ContrastPicker.nudgeForeground(
            ContrastPicker.pickReadable(
                opaque,
                intArrayOf(theme.onBackground, ContrastPicker.WHITE, ContrastPicker.BLACK)
            ),
            opaque,
            ContrastPicker.WCAG_AA
        )
        val muted = (text and 0x00FFFFFF) or (0xB3 shl 24)
        val upBg = ContrastPicker.nudgeForeground(UP_GREEN, opaque, 3.0)
        val downBg = ContrastPicker.nudgeForeground(DOWN_RED, opaque, 3.0)
        val flatBg = (text and 0x00FFFFFF) or (0x33 shl 24)
        return MarketCardColors(
            card = card,
            text = text,
            muted = muted,
            chipUpBg = upBg,
            chipUpFg = ContrastPicker.pickReadable(upBg),
            chipDownBg = downBg,
            chipDownFg = ContrastPicker.pickReadable(downBg),
            chipFlatBg = flatBg,
            chipFlatFg = text,
            accent = theme.accent
        )
    }

    fun chipBackground(colors: MarketCardColors, direction: Int): Int = when {
        direction > 0 -> colors.chipUpBg
        direction < 0 -> colors.chipDownBg
        else -> colors.chipFlatBg
    }

    fun chipForeground(colors: MarketCardColors, direction: Int): Int = when {
        direction > 0 -> colors.chipUpFg
        direction < 0 -> colors.chipDownFg
        else -> colors.chipFlatFg
    }
}
