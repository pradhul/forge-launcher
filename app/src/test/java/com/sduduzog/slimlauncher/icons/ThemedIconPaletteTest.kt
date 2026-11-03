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
        assertThat(IconCacheLogic.KEY_VERSION).isEqualTo(4)
        assertThat(com.sduduzog.slimlauncher.wallpaper.WallpaperPackLogic.RENDER_VERSION).isEqualTo("r4")
    }
}

class ThemedIconPaletteChromaTest {
    private fun hueDiff(a: Double, b: Double): Double {
        val d = kotlin.math.abs(a - b) % 360.0
        return if (d > 180) 360 - d else d
    }

    @Test
    fun glyphAndBackgroundFollowSeedHueWithRealChroma() {
        // Includes muted, saturation-clamped accents like ForgeThemeMapper produces.
        val seeds = intArrayOf(
            0xFF3366AA.toInt(), 0xFFE53935.toInt(), 0xFF1DB954.toInt(), 0xFFFFB300.toInt(),
            0xFF7E57C2.toInt(), 0xFF6B7F8E.toInt(), 0xFF8A7560.toInt(), 0xFF5C7A66.toInt()
        )
        for (seed in seeds) for (dark in listOf(false, true)) {
            val c = ThemedIconPalette.colors(seed, dark)
            val h = ThemedIconPalette.hue(seed)
            assertThat(hueDiff(ThemedIconPalette.hue(c.foreground), h)).isAtMost(20.0)
            assertThat(hueDiff(ThemedIconPalette.hue(c.background), h)).isAtMost(20.0)
            com.google.common.truth.Truth.assertWithMessage("seed=%s dark=%s fg=%s", Integer.toHexString(seed), dark, Integer.toHexString(c.foreground)).that(ThemedIconPalette.chroma(c.foreground)).isAtLeast(35)
            assertThat(ThemedIconPalette.chroma(c.background)).isAtLeast(12)
            assertThat(ThemedIconPalette.contrast(c.background, c.foreground)).isAtLeast(4.5)
        }
    }

    @Test
    fun lightGlyphIsColouredDarkToneNotBlack() {
        val c = ThemedIconPalette.colors(0xFF6B7F8E.toInt(), dark = false)
        assertThat(ThemedIconPalette.lstar(c.foreground)).isAtLeast(25.0)
        assertThat(ThemedIconPalette.lstar(c.foreground)).isAtMost(40.0)
    }

    @Test
    fun cacheKeyIncludesPalette() {
        val t = PackTheme(0xFF102030.toInt(), 0xFF203040.toInt(), -1, 0xFF4A7FB0.toInt(), 0xFF000000.toInt())
        val p = ThemedIconPalette.forTheme(t)
        assertThat(IconCacheLogic.themeHash("w", t, IconStyle.THEMED)).contains(Integer.toHexString(p.foreground))
    }
}
