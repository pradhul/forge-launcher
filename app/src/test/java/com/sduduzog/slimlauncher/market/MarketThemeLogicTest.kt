package com.sduduzog.slimlauncher.market

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.icons.ContrastPicker
import com.sduduzog.slimlauncher.icons.PackTheme
import org.junit.Test

class MarketThemeLogicTest {
    @Test
    fun wallpaperThemeKeepsReadableTextOnDarkAndLightCards() {
        val dark = PackTheme(
            background = 0xFF1A1028.toInt(),
            backgroundEnd = 0xFF120818.toInt(),
            onBackground = 0xFFF7F0FF.toInt(),
            accent = 0xFFB388FF.toInt(),
            onAccent = 0xFF1A1028.toInt(),
            darkSurface = true
        )
        val light = PackTheme(
            background = 0xFFF6F1E8.toInt(),
            backgroundEnd = 0xFFECE4D4.toInt(),
            onBackground = 0xFF1A1208.toInt(),
            accent = 0xFF8A4B12.toInt(),
            onAccent = 0xFFFFF8EE.toInt(),
            darkSurface = false
        )
        for (theme in listOf(dark, light, PackTheme.STOCK)) {
            val colors = MarketThemeLogic.colors(theme)
            val card = colors.card or 0xFF000000.toInt()
            assertThat(ContrastPicker.contrastRatio(card, colors.text))
                .isAtLeast(ContrastPicker.WCAG_AA)
            assertThat(colors.card ushr 24).isEqualTo(0xE6)
            val upFg = MarketThemeLogic.chipForeground(colors, 1)
            val upBg = MarketThemeLogic.chipBackground(colors, 1) or 0xFF000000.toInt()
            assertThat(ContrastPicker.contrastRatio(upBg, upFg)).isAtLeast(ContrastPicker.WCAG_AA)
            val downFg = MarketThemeLogic.chipForeground(colors, -1)
            val downBg = MarketThemeLogic.chipBackground(colors, -1) or 0xFF000000.toInt()
            assertThat(ContrastPicker.contrastRatio(downBg, downFg)).isAtLeast(ContrastPicker.WCAG_AA)
        }
    }
}
