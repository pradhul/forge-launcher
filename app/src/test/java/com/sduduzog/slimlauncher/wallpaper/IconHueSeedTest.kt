package com.sduduzog.slimlauncher.wallpaper

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.icons.ThemedIconPalette
import org.junit.Test

class IconHueSeedTest {
    private fun hueDiff(a: Double, b: Double): Double {
        val d = kotlin.math.abs(a - b) % 360.0
        return if (d > 180) 360 - d else d
    }

    private fun assertFamily(sw: WallpaperSwatches, hue: Double) {
        val theme = ForgeThemeMapper.map(sw)
        assertThat(hueDiff(ThemedIconPalette.hue(theme.accent), hue)).isAtMost(20.0)
        val c = ThemedIconPalette.forTheme(theme)
        assertThat(hueDiff(ThemedIconPalette.hue(c.foreground), hue)).isAtMost(20.0)
        assertThat(hueDiff(ThemedIconPalette.hue(c.background), hue)).isAtMost(20.0)
    }

    @Test
    fun redAndBlackWallpaperGivesRedIconsEvenWithGreenMaterialYouAccent() {
        // Black dominates, red second; system accent / tertiary hue-shifted to green.
        assertFamily(
            WallpaperSwatches(
                primary = 0xFF0A0A0A.toInt(),
                secondary = 0xFFC8102E.toInt(),
                tertiary = 0xFF2E7D32.toInt(),
                supportsDarkTheme = true,
                dynamicAccent1 = 0xFF8BD88B.toInt(),
                dynamicAccent2 = 0xFFA5D6A7.toInt()
            ),
            ThemedIconPalette.hue(0xFFC8102E.toInt())
        )
    }

    @Test
    fun redPrimaryWins() {
        assertFamily(
            WallpaperSwatches(primary = 0xFFB71C1C.toInt(), secondary = 0xFF000000.toInt(), tertiary = 0xFF00C853.toInt()),
            ThemedIconPalette.hue(0xFFB71C1C.toInt())
        )
    }

    @Test
    fun blueWallpaperGivesBlue() {
        assertFamily(
            WallpaperSwatches(primary = 0xFF1E4FA0.toInt(), secondary = 0xFF0D1B2A.toInt(), dynamicAccent1 = 0xFFFFB74D.toInt()),
            ThemedIconPalette.hue(0xFF1E4FA0.toInt())
        )
    }

    @Test
    fun allBlackOrGreyIsNeutral() {
        val sw = WallpaperSwatches(primary = 0xFF050505.toInt(), secondary = 0xFF202020.toInt(), tertiary = 0xFF808080.toInt())
        assertThat(ForgeThemeMapper.pickIconSeed(sw)).isNull()
        val c = ThemedIconPalette.forTheme(ForgeThemeMapper.map(sw))
        assertThat(ThemedIconPalette.chroma(c.foreground)).isAtMost(6)
        assertThat(ThemedIconPalette.chroma(c.background)).isAtMost(6)
        assertThat(ThemedIconPalette.contrast(c.background, c.foreground)).isAtLeast(4.5)
    }

    @Test
    fun greySkippedForNextColour() {
        assertThat(ForgeThemeMapper.isChromatic(0xFF777777.toInt())).isFalse()
        assertThat(ForgeThemeMapper.isChromatic(0xFF101010.toInt())).isFalse()
        assertThat(ForgeThemeMapper.isChromatic(0xFFC8102E.toInt())).isTrue()
        val sw = WallpaperSwatches(primary = 0xFF777777.toInt(), secondary = 0xFF2E7DD2.toInt())
        assertThat(ForgeThemeMapper.pickIconSeed(sw)).isEqualTo(0xFF2E7DD2.toInt())
    }
}

class PaletteKeyVersionTest {
    @Test
    fun paletteKeyCarriesMapperVersionSoOldThemesRemap() {
        val key = ForgeThemeMapper.paletteKey(WallpaperSwatches(primary = 0xFF0A0A0A.toInt()))
        assertThat(key).startsWith(ForgeThemeMapper.MAPPER_VERSION)
    }
}
