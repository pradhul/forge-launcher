package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IconPostProcessTest {

    @Test
    fun mixBlendsTowardStylized() {
        val black = IntArray(1) { IconPostProcess.argb(255, 0, 0, 0) }
        val white = IntArray(1) { IconPostProcess.argb(255, 255, 255, 255) }
        val half = IconPostProcess.mix(black, white, 0.5f)
        assertThat(IconPostProcess.red(half[0])).isEqualTo(128)
    }

    @Test
    fun posterizeReducesLevels() {
        val src = intArrayOf(IconPostProcess.argb(255, 90, 90, 90))
        val out = IconPostProcess.posterize(src, 2)
        val channel = IconPostProcess.red(out[0])
        assertThat(channel == 0 || channel == 255).isTrue()
    }

    @Test
    fun duotoneMapsDarkToShadows() {
        val src = intArrayOf(IconPostProcess.argb(255, 0, 0, 0))
        val out = IconPostProcess.duotone(src, 0xFF112233.toInt(), 0xFFDDEEFF.toInt(), 1f)
        assertThat(IconPostProcess.red(out[0])).isEqualTo(0x11)
        assertThat(IconPostProcess.blue(out[0])).isEqualTo(0x33)
    }

    @Test
    fun circleMaskClearsCorners() {
        val size = 8
        val src = IntArray(size * size) { IconPostProcess.argb(255, 255, 0, 0) }
        val out = IconPostProcess.applyShapeMask(src, size, size, BackplateShape.CIRCLE)
        assertThat(IconPostProcess.alpha(out[0])).isEqualTo(0)
        val center = (size / 2) * size + size / 2
        assertThat(IconPostProcess.alpha(out[center])).isEqualTo(255)
    }

    @Test
    fun squircleMaskKeepsCenter() {
        val size = 10
        val src = IntArray(size * size) { IconPostProcess.argb(255, 10, 10, 10) }
        val out = IconPostProcess.apply(
            pixels = src,
            width = size,
            height = size,
            spec = PostProcessSpec(),
            shape = BackplateShape.SQUIRCLE,
            strength = 1f
        )
        val center = (size / 2) * size + size / 2
        assertThat(IconPostProcess.alpha(out[center])).isGreaterThan(200)
        assertThat(IconPostProcess.alpha(out[0])).isEqualTo(0)
    }

    @Test
    fun wallpaperPresetKeepsColorWithoutDuotone() {
        assertThat(StylePreset.WALLPAPER.postProcess.duotoneMix).isEqualTo(0f)
        assertThat(StylePreset.WALLPAPER.assetPath).isEmpty()
        assertThat(StylePreset.byId("neon").id).isEqualTo(StylePreset.WALLPAPER_ID)
    }
}
