package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class ThemedIconTintTest {
    private val accent = IconPostProcess.argb(255, 40, 120, 220)

    @Test
    fun amountStaysSubtle() {
        assertThat(ThemedIconTint.amountFor(0f)).isAtLeast(ThemedIconTint.MIN_AMOUNT - 1e-4f)
        assertThat(ThemedIconTint.amountFor(1f)).isAtMost(ThemedIconTint.MAX_AMOUNT)
        assertThat(ThemedIconTint.amountFor(0.7f)).isWithin(0.02f).of(0.45f)
        assertThat(ThemedIconTint.amountFor(5f)).isAtMost(0.5f)
    }

    @Test
    fun preservesAlphaAndTransparency() {
        val px = intArrayOf(0, IconPostProcess.argb(128, 200, 10, 10), IconPostProcess.argb(255, 0, 255, 0))
        val out = ThemedIconTint.tint(px, accent, 1f)
        assertThat(out[0]).isEqualTo(0)
        assertThat(IconPostProcess.alpha(out[1])).isEqualTo(128)
        assertThat(IconPostProcess.alpha(out[2])).isEqualTo(255)
    }

    @Test
    fun keepsArtworkRecognisable() {
        val red = IconPostProcess.argb(255, 230, 30, 30)
        val out = ThemedIconTint.tint(intArrayOf(red), accent, 1f)[0]
        assertThat(abs(IconPostProcess.red(out) - 230)).isAtMost(110)
        assertThat(IconPostProcess.red(out)).isGreaterThan(IconPostProcess.blue(out))
    }

    @Test
    fun nudgesTowardAccent() {
        val grey = IconPostProcess.argb(255, 128, 128, 128)
        val out = ThemedIconTint.tint(intArrayOf(grey), accent, 1f)[0]
        assertThat(IconPostProcess.blue(out)).isGreaterThan(IconPostProcess.red(out))
    }

    @Test
    fun luminanceMatchedKeepsBrightness() {
        for (lum in listOf(10, 80, 160, 240)) {
            val m = ThemedIconTint.luminanceMatched(accent, lum)
            assertThat(abs(IconPostProcess.luminance(m) - lum)).isAtMost(4)
        }
    }

    @Test
    fun noBackplateFillOnTransparentCorners() {
        val px = IntArray(16) { 0 }
        assertThat(ThemedIconTint.tint(px, accent, 1f).all { it == 0 }).isTrue()
    }

    @Test
    fun iconSizingUsesDeviceSizeWithoutUpscaleFloor() {
        assertThat(IconSizing.clamp(192, 144)).isEqualTo(192)
        assertThat(IconSizing.clamp(0, 216)).isEqualTo(216)
        assertThat(IconSizing.clamp(2000, 10)).isEqualTo(IconSizing.MAX_PX)
    }

    @Test
    fun cacheKeyCarriesVersion() {
        val key = IconCacheLogic.key("p", "a", 0L, null, "", 128)
        assertThat(key).startsWith("v${IconCacheLogic.KEY_VERSION}\u0000")
        assertThat(IconCacheLogic.KEY_VERSION).isAtLeast(2)
    }
}
