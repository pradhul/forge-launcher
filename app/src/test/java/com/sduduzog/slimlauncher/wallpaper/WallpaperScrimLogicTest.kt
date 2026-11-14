package com.sduduzog.slimlauncher.wallpaper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WallpaperScrimLogicTest {

    @Test
    fun homeScrimsAreLighterThanDrawer() {
        assertThat(WallpaperThemeLogic.HOME_CLOCK_ALPHA)
            .isLessThan(WallpaperThemeLogic.DRAWER_TOP_ALPHA)
        assertThat(WallpaperThemeLogic.HOME_DOCK_ALPHA)
            .isLessThan(WallpaperThemeLogic.DRAWER_BOTTOM_ALPHA)
        val seed = 0xFF80C0FF.toInt()
        assertThat(WallpaperThemeLogic.homeClockScrim(seed) ushr 24)
            .isEqualTo(WallpaperThemeLogic.HOME_CLOCK_ALPHA)
        assertThat(WallpaperThemeLogic.homeDockScrim(seed) ushr 24)
            .isEqualTo(WallpaperThemeLogic.HOME_DOCK_ALPHA)
    }

    @Test
    fun lightWallpaperGetsLightTintAndDarkGetsDarkTint() {
        assertThat(WallpaperThemeLogic.isLightSeed(0xFFF5F0E6.toInt())).isTrue()
        assertThat(WallpaperThemeLogic.isLightSeed(0xFF101018.toInt())).isFalse()
        val lightTint = WallpaperThemeLogic.scrimTint(0xFFF5F0E6.toInt())
        val darkTint = WallpaperThemeLogic.scrimTint(0xFF101018.toInt())
        assertThat(WallpaperThemeLogic.isLightSeed(lightTint)).isTrue()
        assertThat(WallpaperThemeLogic.isLightSeed(darkTint)).isFalse()
    }
}
