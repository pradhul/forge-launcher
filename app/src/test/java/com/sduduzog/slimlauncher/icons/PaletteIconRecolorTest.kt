package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaletteIconRecolorTest {

    private val theme = PackTheme(
        background = 0xFF1A1028.toInt(),
        backgroundEnd = 0xFF0B0614.toInt(),
        onBackground = 0xFFF4F4F4.toInt(),
        accent = 0xFF80C0FF.toInt(),
        onAccent = 0xFF101018.toInt(),
        seed = 0xFF1A1028.toInt(),
        darkSurface = true
    )

    @Test
    fun recolorMapsGlyphOntoPaletteAndMasksCorners() {
        val size = 12
        val pixels = IntArray(size * size) { IconPostProcess.argb(255, 200, 40, 40) }
        val out = PaletteIconRecolor.recolor(pixels, size, size, theme, 0.8f)
        assertThat(IconPostProcess.alpha(out[0])).isEqualTo(0)
        val center = (size / 2) * size + size / 2
        assertThat(IconPostProcess.alpha(out[center])).isGreaterThan(200)
        assertThat(IconPostProcess.red(out[center])).isNotEqualTo(200)
        assertThat(IconPostProcess.blue(out[center])).isGreaterThan(IconPostProcess.red(out[center]))
    }

    @Test
    fun transparentPixelsBecomeThemedPlate() {
        val size = 8
        val pixels = IntArray(size * size) { 0 }
        pixels[(size / 2) * size + size / 2] = IconPostProcess.argb(255, 10, 10, 10)
        val out = PaletteIconRecolor.recolor(pixels, size, size, theme, 0.7f)
        val midEdge = (size / 2) * size + 1
        assertThat(IconPostProcess.alpha(out[midEdge])).isGreaterThan(0)
        assertThat(out[midEdge] and 0x00FFFFFF).isNotEqualTo(0)
    }
}
