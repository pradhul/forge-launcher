package com.sduduzog.slimlauncher.wallpaper

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.icons.PackThemeLogic
import org.junit.Test

class ThemeSourceLogicTest {

    private val wallpaperTheme = PackTheme(
        background = 0xFF102030.toInt(),
        backgroundEnd = 0xFF203040.toInt(),
        onBackground = 0xFFF5F5F5.toInt(),
        accent = 0xFF80C0FF.toInt(),
        onAccent = 0xFF102030.toInt()
    )
    private val packTheme = PackTheme(
        background = 0xFF111111.toInt(),
        backgroundEnd = 0xFF222222.toInt(),
        onBackground = 0xFFF5F5F5.toInt(),
        accent = 0xFFFFCC00.toInt(),
        onAccent = 0xFF111111.toInt()
    )

    @Test
    fun defaultSourceIsWallpaperWhenWallpaperIsSet() {
        assertThat(ThemeSourceLogic.defaultSource(hasWallpaper = true))
            .isEqualTo(ThemeSource.FROM_WALLPAPER)
    }

    @Test
    fun defaultSourceIsIconPackWhenNoWallpaper() {
        assertThat(ThemeSourceLogic.defaultSource(hasWallpaper = false))
            .isEqualTo(ThemeSource.FROM_ICON_PACK)
    }

    @Test
    fun selectedUsesExplicitWallpaperChoice() {
        assertThat(
            ThemeSourceLogic.selected(
                explicitId = ThemeSource.FROM_WALLPAPER.id,
                bingEnabled = false,
                wallpaperSet = false
            )
        ).isEqualTo(ThemeSource.FROM_WALLPAPER)
    }

    @Test
    fun selectedUsesExplicitIconPackEvenWhenBingIsOn() {
        assertThat(
            ThemeSourceLogic.selected(
                explicitId = ThemeSource.FROM_ICON_PACK.id,
                bingEnabled = true,
                wallpaperSet = true
            )
        ).isEqualTo(ThemeSource.FROM_ICON_PACK)
    }

    @Test
    fun selectedDefaultsToWallpaperWhenBingEnabled() {
        assertThat(
            ThemeSourceLogic.selected(
                explicitId = null,
                bingEnabled = true,
                wallpaperSet = false
            )
        ).isEqualTo(ThemeSource.FROM_WALLPAPER)
    }

    @Test
    fun selectedDefaultsToWallpaperWhenAnyWallpaperIsSet() {
        assertThat(
            ThemeSourceLogic.selected(
                explicitId = null,
                bingEnabled = false,
                wallpaperSet = true
            )
        ).isEqualTo(ThemeSource.FROM_WALLPAPER)
    }

    @Test
    fun selectedDefaultsToIconPackWhenNothingIsSet() {
        assertThat(
            ThemeSourceLogic.selected(
                explicitId = null,
                bingEnabled = false,
                wallpaperSet = false
            )
        ).isEqualTo(ThemeSource.FROM_ICON_PACK)
    }

    @Test
    fun resolveWallpaperUsesWallpaperTheme() {
        assertThat(
            ThemeSourceLogic.resolve(
                source = ThemeSource.FROM_WALLPAPER,
                wallpaperTheme = wallpaperTheme,
                packTheme = packTheme,
                matchBackground = true
            )
        ).isEqualTo(wallpaperTheme)
    }

    @Test
    fun resolveWallpaperFallsBackToPackWhenColorsMissing() {
        assertThat(
            ThemeSourceLogic.resolve(
                source = ThemeSource.FROM_WALLPAPER,
                wallpaperTheme = null,
                packTheme = packTheme,
                matchBackground = true
            )
        ).isEqualTo(packTheme)
    }

    @Test
    fun resolveIconPackIgnoresWallpaperTheme() {
        assertThat(
            ThemeSourceLogic.resolve(
                source = ThemeSource.FROM_ICON_PACK,
                wallpaperTheme = wallpaperTheme,
                packTheme = packTheme,
                matchBackground = true
            )
        ).isEqualTo(packTheme)
    }

    @Test
    fun resolveIconPackUsesStockWhenMatchDisabled() {
        assertThat(
            ThemeSourceLogic.resolve(
                source = ThemeSource.FROM_ICON_PACK,
                wallpaperTheme = wallpaperTheme,
                packTheme = packTheme,
                matchBackground = false
            )
        ).isEqualTo(PackTheme.STOCK)
    }

    @Test
    fun fromColorsMapsPrimarySecondaryTertiary() {
        val theme = WallpaperThemeLogic.fromColors(
            primary = 0xFF2A1050.toInt(),
            secondary = 0xFF0B0614.toInt(),
            tertiary = 0xFF39F3FF.toInt()
        )
        assertThat(theme.accent).isEqualTo(0xFF39F3FF.toInt())
        assertThat(theme.background).isNotEqualTo(PackTheme.STOCK.background)
        assertThat(theme).isEqualTo(
            PackThemeLogic.fromSwatches(
                dominant = 0xFF2A1050.toInt(),
                darkMuted = 0xFF0B0614.toInt(),
                vibrant = 0xFF39F3FF.toInt(),
                muted = 0xFF0B0614.toInt()
            )
        )
    }

    @Test
    fun scrimKeepsRgbAndAppliesAlpha() {
        val color = 0xFF102030.toInt()
        assertThat(WallpaperThemeLogic.scrim(color, 0x99))
            .isEqualTo(0x99102030.toInt())
        assertThat(WallpaperThemeLogic.scrim(color) and 0x00FFFFFF)
            .isEqualTo(color and 0x00FFFFFF)
    }
}
