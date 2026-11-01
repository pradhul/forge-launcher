package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemedIconPaletteTest {
    private val seeds = intArrayOf(
        0xFF3366AA.toInt(), 0xFFE53935.toInt(), 0xFF1DB954.toInt(),
        0xFFFFEB3B.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF7E57C2.toInt()
    )

    @Test
    fun lightPaletteHasLightBackgroundAndDarkGlyph() {
        for (seed in seeds) {
            val c = ThemedIconPalette.colors(seed, dark = false)
            assertThat(ThemedIconPalette.lstar(c.background)).isGreaterThan(80.0)
            assertThat(ThemedIconPalette.lstar(c.foreground)).isLessThan(35.0)
        }
    }

    @Test
    fun darkPaletteIsOpposite() {
        for (seed in seeds) {
            val c = ThemedIconPalette.colors(seed, dark = true)
            assertThat(ThemedIconPalette.lstar(c.background)).isLessThan(35.0)
            assertThat(ThemedIconPalette.lstar(c.foreground)).isGreaterThan(75.0)
        }
    }

    @Test
    fun contrastMeetsWcagAA() {
        for (seed in seeds) for (dark in listOf(false, true)) {
            val c = ThemedIconPalette.colors(seed, dark)
            assertThat(ThemedIconPalette.contrast(c.background, c.foreground)).isAtLeast(4.5)
        }
    }

    @Test
    fun contrastFormulaMatchesWcag() {
        assertThat(ThemedIconPalette.contrast(0xFF000000.toInt(), 0xFFFFFFFF.toInt())).isWithin(0.01).of(21.0)
    }

    @Test
    fun iconStyleDefaultsAndMigration() {
        assertThat(IconStyle.fromStorage(null)).isEqualTo(IconStyle.THEMED)
        assertThat(IconStyle.fromStorage(null, legacyArtistic = true)).isEqualTo(IconStyle.ARTISTIC)
        assertThat(IconStyle.fromStorage("tinted")).isEqualTo(IconStyle.TINTED)
        assertThat(IconCacheLogic.KEY_VERSION).isEqualTo(3)
        assertThat(com.sduduzog.slimlauncher.wallpaper.WallpaperPackLogic.RENDER_VERSION).isEqualTo("r3")
    }
}
