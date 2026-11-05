package com.sduduzog.slimlauncher.wallpaper

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.icons.ContrastPicker
import org.junit.Test

class ForgeThemeMapperTest {

    @Test
    fun lightWallpaperUsesDarkLabelsAndLightChrome() {
        val theme = ForgeThemeMapper.map(
            WallpaperSwatches(
                primary = 0xFFE8D5B5.toInt(),
                secondary = 0xFFF4E8D0.toInt(),
                tertiary = 0xFFC4783A.toInt(),
                supportsDarkText = true,
                supportsDarkTheme = false
            )
        )
        assertThat(theme.darkSurface).isFalse()
        assertThat(ContrastPicker.relativeLuminance(theme.onBackground))
            .isLessThan(ContrastPicker.relativeLuminance(theme.background))
        assertThat(ContrastPicker.contrastRatio(theme.background, theme.onBackground))
            .isAtLeast(ForgeThemeMapper.MIN_CONTRAST)
        assertThat(ContrastPicker.contrastRatio(theme.accent, theme.onAccent))
            .isAtLeast(ForgeThemeMapper.MIN_CONTRAST)
    }

    @Test
    fun darkWallpaperUsesLightLabels() {
        val theme = ForgeThemeMapper.map(
            WallpaperSwatches(
                primary = 0xFF1A1028.toInt(),
                secondary = 0xFF0B0614.toInt(),
                tertiary = 0xFF39F3FF.toInt(),
                supportsDarkText = false,
                supportsDarkTheme = true
            )
        )
        assertThat(theme.darkSurface).isTrue()
        assertThat(ContrastPicker.relativeLuminance(theme.onBackground))
            .isGreaterThan(ContrastPicker.relativeLuminance(theme.background))
        assertThat(ContrastPicker.contrastRatio(theme.background, theme.onBackground))
            .isAtLeast(ForgeThemeMapper.MIN_CONTRAST)
    }

    @Test
    fun dynamicNeutralsWinWallpaperHueDrivesAccent() {
        val theme = ForgeThemeMapper.map(
            WallpaperSwatches(
                primary = 0xFF202020.toInt(),
                secondary = 0xFF111111.toInt(),
                tertiary = 0xFFFF00FF.toInt(),
                supportsDarkTheme = true,
                dynamicAccent1 = 0xFF80C0FF.toInt(),
                dynamicNeutral50 = 0xFFF5F5F5.toInt(),
                dynamicNeutral900 = 0xFF1C1B1F.toInt(),
                source = ThemeExtractSource.DYNAMIC
            )
        )
        assertThat(theme.darkSurface).isTrue()
        assertThat(theme.background).isNotEqualTo(0xFF000000.toInt())
        assertThat(theme.background).isNotEqualTo(0xFFFFFFFF.toInt())
        assertThat(ContrastPicker.saturation(theme.accent).toDouble()).isAtMost(ForgeThemeMapper.ICON_SAT_MAX + 0.01)
    }

    @Test
    fun contrastNudgeLiftsGreyOnGreyToWcag() {
        val background = 0xFF4A4A52.toInt()
        val foreground = 0xFF8A8A90.toInt()
        assertThat(ContrastPicker.contrastRatio(background, foreground))
            .isLessThan(ForgeThemeMapper.MIN_CONTRAST)
        val nudged = ForgeThemeMapper.nudgeForContrast(foreground, background)
        assertThat(ContrastPicker.contrastRatio(background, nudged))
            .isAtLeast(ForgeThemeMapper.MIN_CONTRAST)
        assertThat(nudged).isNotEqualTo(foreground)
    }

    @Test
    fun dockAndSearchAreTranslucentAndNotPureBlackOrWhite() {
        val dark = ForgeThemeMapper.translucentTone(0xFF000000.toInt(), ForgeThemeMapper.DOCK_ALPHA, true)
        val light = ForgeThemeMapper.translucentTone(0xFFFFFFFF.toInt(), ForgeThemeMapper.SEARCH_PILL_ALPHA, false)
        assertThat(dark ushr 24).isEqualTo(ForgeThemeMapper.DOCK_ALPHA)
        assertThat(light ushr 24).isEqualTo(ForgeThemeMapper.SEARCH_PILL_ALPHA)
        assertThat(dark and 0x00FFFFFF).isNotEqualTo(0x000000)
        assertThat(light and 0x00FFFFFF).isNotEqualTo(0xFFFFFF)
        assertThat(ForgeThemeMapper.isNearBlack(dark or 0xFF000000.toInt())).isFalse()
        assertThat(ForgeThemeMapper.isNearWhite(light or 0xFF000000.toInt())).isFalse()
        assertThat(ContrastPicker.relativeLuminance(dark or 0xFF000000.toInt())).isGreaterThan(0.02)
        assertThat(ContrastPicker.relativeLuminance(light or 0xFF000000.toInt())).isLessThan(0.94)
    }

    @Test
    fun paletteKeyChangesWhenColoursOrHintsChange() {
        val first = WallpaperSwatches(
            primary = 0xFF112233.toInt(),
            secondary = 0xFF445566.toInt(),
            supportsDarkText = false
        )
        val second = first.copy(primary = 0xFF223344.toInt())
        val third = first.copy(supportsDarkText = true)
        assertThat(ForgeThemeMapper.paletteKey(first))
            .isNotEqualTo(ForgeThemeMapper.paletteKey(second))
        assertThat(ForgeThemeMapper.paletteKey(first))
            .isNotEqualTo(ForgeThemeMapper.paletteKey(third))
        assertThat(ForgeThemeMapper.paletteKey(first))
            .isEqualTo(ForgeThemeMapper.paletteKey(first.copy()))
    }

    @Test
    fun changeDetectionSkipsUnchangedAndAppliesForce() {
        val key = "wallpaperColors:FF112233:-:-:-:-:-:-:-"
        assertThat(ForgeThemeMapper.colorsChanged(key, key)).isFalse()
        assertThat(ForgeThemeMapper.colorsChanged(null, key)).isTrue()
        assertThat(ForgeThemeMapper.colorsChanged(key, "other")).isTrue()
        assertThat(ForgeThemeMapper.colorsChanged(key, null)).isFalse()
        assertThat(ForgeThemeMapper.shouldApply(key, key, force = false)).isFalse()
        assertThat(ForgeThemeMapper.shouldApply(key, key, force = true)).isTrue()
        assertThat(ForgeThemeMapper.shouldApply(null, key, force = false)).isTrue()
    }

    @Test
    fun sourceNameListsWallpaperColorsDynamicAndBitmap() {
        assertThat(ForgeThemeMapper.resolveSourceName(true, false, false))
            .isEqualTo("wallpaperColors")
        assertThat(ForgeThemeMapper.resolveSourceName(true, true, false))
            .isEqualTo("wallpaperColors+dynamic")
        assertThat(ForgeThemeMapper.resolveSourceName(false, false, true))
            .isEqualTo("bitmap")
        assertThat(ForgeThemeMapper.primarySource(true, true, false))
            .isEqualTo(ThemeExtractSource.DYNAMIC)
        assertThat(ForgeThemeMapper.primarySource(true, false, false))
            .isEqualTo(ThemeExtractSource.WALLPAPER_COLORS)
    }
}
