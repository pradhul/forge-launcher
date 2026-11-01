package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IconSurfaceParityTest {
    private val theme = PackTheme(
        background = 0xFF102030.toInt(), backgroundEnd = 0xFF203040.toInt(),
        onBackground = 0xFFFFFFFF.toInt(), accent = 0xFF4A7FB0.toInt(), onAccent = 0xFF000000.toInt()
    )

    private fun req(surface: IconSurface, style: IconStyle, t: PackTheme? = theme) =
        IconCacheLogic.request(surface, "pkg", "act", 0L, "wallpaper", t, style, 168)

    @Test
    fun homeDockAndDrawerRequestIdenticalStyleAndColours() {
        for (style in IconStyle.values()) {
            val drawer = req(IconSurface.DRAWER, style)
            assertThat(req(IconSurface.HOME, style)).isEqualTo(drawer)
            assertThat(req(IconSurface.DOCK, style)).isEqualTo(drawer)
            assertThat(drawer.allowStyle).isTrue()
        }
    }

    @Test
    fun themedRequestCarriesWallpaperPalette() {
        val r = req(IconSurface.HOME, IconStyle.THEMED)
        assertThat(r.colors).isEqualTo(ThemedIconPalette.forTheme(theme))
        assertThat(req(IconSurface.HOME, IconStyle.ORIGINAL).colors).isNull()
    }

    @Test
    fun styleOrThemeChangeChangesCacheKey() {
        val a = req(IconSurface.HOME, IconStyle.THEMED).key
        assertThat(req(IconSurface.HOME, IconStyle.TINTED).key).isNotEqualTo(a)
        assertThat(req(IconSurface.HOME, IconStyle.THEMED, theme.copy(accent = 0xFFAA3344.toInt())).key).isNotEqualTo(a)
    }

    @Test
    fun stockFallbackWhilePackGeneratingIsNotCached() {
        assertThat(IconCacheLogic.shouldCache(packApplied = true, packIconReady = false, styled = false)).isFalse()
        assertThat(IconCacheLogic.shouldCache(packApplied = true, packIconReady = true, styled = false)).isTrue()
        assertThat(IconCacheLogic.shouldCache(packApplied = false, packIconReady = false, styled = false)).isTrue()
    }
}
