package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PackThemeLogicTest {

    @Test
    fun neonSwatchesProduceDarkReadableTheme() {
        val theme = PackThemeLogic.fromSwatches(
            dominant = 0xFF2A1050.toInt(),
            darkMuted = 0xFF0B0614.toInt(),
            vibrant = 0xFF39F3FF.toInt(),
            muted = 0xFF6B3FA0.toInt()
        )
        assertThat(ContrastPicker.relativeLuminance(theme.background)).isAtMost(0.22)
        assertThat(
            ContrastPicker.contrastRatio(theme.background, theme.onBackground)
        ).isAtLeast(ContrastPicker.WCAG_AA)
        assertThat(
            ContrastPicker.contrastRatio(theme.accent, theme.onAccent)
        ).isAtLeast(ContrastPicker.WCAG_AA)
        assertThat(theme.background).isNotEqualTo(PackTheme.STOCK.background)
        assertThat(theme.accent).isEqualTo(0xFF39F3FF.toInt())
    }

    @Test
    fun resolveUsesStockWhenMatchDisabled() {
        val packTheme = PackThemeLogic.fromSwatches(
            dominant = 0xFF2A1050.toInt(),
            darkMuted = 0xFF0B0614.toInt(),
            vibrant = 0xFFFF2BD6.toInt(),
            muted = null
        )
        assertThat(PackThemeLogic.resolve(matchBackground = false, packTheme = packTheme))
            .isEqualTo(PackTheme.STOCK)
    }

    @Test
    fun resolveUsesStockWhenNoPackTheme() {
        assertThat(PackThemeLogic.resolve(matchBackground = true, packTheme = null))
            .isEqualTo(PackTheme.STOCK)
    }

    @Test
    fun resolveKeepsPackThemeWhenMatchEnabled() {
        val packTheme = PackTheme(
            background = 0xFF111111.toInt(),
            backgroundEnd = 0xFF222222.toInt(),
            onBackground = 0xFFF5F5F5.toInt(),
            accent = 0xFFFFCC00.toInt(),
            onAccent = 0xFF111111.toInt()
        )
        assertThat(PackThemeLogic.resolve(matchBackground = true, packTheme = packTheme))
            .isEqualTo(packTheme)
    }
}
