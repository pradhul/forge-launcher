package com.sduduzog.slimlauncher.ui.home

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.icons.PackTheme
import org.junit.Test

class DrawerSearchStyleTest {

    @Test
    fun containerIsTranslucentMixOfSurfaceAndAccent() {
        val surface = PackTheme.STOCK.background
        val accent = PackTheme.STOCK.accent
        val color = DrawerSearchStyle.containerColor(surface, accent)
        assertThat(color ushr 24).isEqualTo(DrawerSearchStyle.CONTAINER_ALPHA)
        assertThat(color and 0x00FFFFFF).isNotEqualTo(surface and 0x00FFFFFF)
        assertThat(color and 0x00FFFFFF).isNotEqualTo(accent and 0x00FFFFFF)
        val mixed = DrawerSearchStyle.mix(surface, accent, DrawerSearchStyle.ACCENT_MIX)
        assertThat(color and 0x00FFFFFF).isEqualTo(mixed and 0x00FFFFFF)
    }

    @Test
    fun wallpaperThemeTintsTextPlaceholderCursorAndIcon() {
        val theme = PackTheme(
            background = 0xFF102030.toInt(),
            backgroundEnd = 0xFF203040.toInt(),
            onBackground = 0xFFF5F5F5.toInt(),
            accent = 0xFF80C0FF.toInt(),
            onAccent = 0xFF102030.toInt()
        )
        assertThat(DrawerSearchStyle.textColor(theme.onBackground)).isEqualTo(theme.onBackground)
        assertThat(DrawerSearchStyle.cursorColor(theme.accent)).isEqualTo(theme.accent)
        assertThat(DrawerSearchStyle.placeholderColor(theme.onBackground) ushr 24)
            .isEqualTo(DrawerSearchStyle.PLACEHOLDER_ALPHA)
        assertThat(DrawerSearchStyle.iconColor(theme.onBackground) ushr 24)
            .isEqualTo(DrawerSearchStyle.ICON_ALPHA)
        assertThat(DrawerSearchStyle.containerColor(theme.background, theme.accent) ushr 24)
            .isEqualTo(DrawerSearchStyle.CONTAINER_ALPHA)
    }
}
