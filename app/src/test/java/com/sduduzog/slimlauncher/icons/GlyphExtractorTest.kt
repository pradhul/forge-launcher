package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.sqrt

class GlyphExtractorTest {
    private val n = 96

    /** Disc plate of [plate] with a centred square logo of [logo]. */
    private fun icon(plate: Int, logo: Int, logoHalf: Int): IntArray {
        val c = n / 2f
        return IntArray(n * n) { i ->
            val x = i % n; val y = i / n
            val d = sqrt((x - c) * (x - c) + (y - c) * (y - c))
            when {
                d > c -> 0
                kotlin.math.abs(x - c) < logoHalf && kotlin.math.abs(y - c) < logoHalf -> logo
                else -> plate
            }
        }
    }

    private val white = 0xFFFFFFFF.toInt()
    private val green = 0xFF1DB954.toInt()
    private val black = 0xFF000000.toInt()

    @Test
    fun darkLogoOnLightPlateIsExtracted() {
        val r = GlyphExtractor.fromBitmap(icon(white, black, 18), n, n)
        assertThat(r.ok).isTrue()
        assertThat(r.alpha[(n / 2) * n + n / 2]).isAtLeast(200)
        assertThat(r.alpha[(n / 2) * n + 6]).isAtMost(30)
    }

    @Test
    fun lightLogoOnColouredPlateIsExtracted() {
        val r = GlyphExtractor.fromBitmap(icon(green, white, 18), n, n)
        assertThat(r.ok).isTrue()
        assertThat(r.coverage).isIn(com.google.common.collect.Range.closed(0.03f, 0.70f))
    }

    @Test
    fun invertedCaseWhenLogoTouchesEdges() {
        // Huge logo reaching the sampled ring: plate colour is the minority.
        val r = GlyphExtractor.fromBitmap(icon(green, white, 40), n, n)
        if (r.ok) {
            assertThat(r.coverage).isAtMost(GlyphExtractor.MAX_COVERAGE)
        } else {
            assertThat(r.coverage).isGreaterThan(GlyphExtractor.MAX_COVERAGE)
        }
        // explicit inverted score: complement of a large mask falls inside range
        val big = IntArray(n * n) { if (it % n in 4..91 && it / n in 4..91) 255 else 0 }
        val inv = IntArray(n * n) { 255 - big[it] }
        assertThat(GlyphExtractor.score(big, n, n, null, false).ok).isFalse()
        assertThat(GlyphExtractor.score(inv, n, n, null, true).ok).isTrue()
    }

    @Test
    fun tinyGlyphIsRejected() {
        val r = GlyphExtractor.fromBitmap(icon(white, black, 1), n, n)
        assertThat(r.ok).isFalse()
    }

    @Test
    fun flatIconHasNoGlyphAndFallsBack() {
        val r = GlyphExtractor.fromBitmap(icon(green, green, 10), n, n)
        assertThat(r.ok).isFalse()
        val shape = IntArray(n * n) { 255 }
        val colors = ThemedIconPalette.colors(0xFF3366AA.toInt(), dark = false)
        val (dark, light) = ThemedIconPalette.duotoneEnds(colors)
        val out = GlyphExtractor.duotone(icon(green, green, 10), shape, dark, light)
        assertThat(out.all { IconPostProcess.alpha(it) == 255 }).isTrue()
    }

    @Test
    fun speckleNoiseIsRejected() {
        val rnd = java.util.Random(7)
        val alpha = IntArray(n * n) { if (rnd.nextFloat() < 0.3f) 255 else 0 }
        val r = GlyphExtractor.score(alpha, n, n, null, false)
        assertThat(r.noise).isGreaterThan(GlyphExtractor.MAX_NOISE)
        assertThat(r.ok).isFalse()
    }

    @Test
    fun coverageBounds() {
        val full = IntArray(n * n) { 255 }
        assertThat(GlyphExtractor.score(full, n, n, null, false).ok).isFalse()
        val empty = IntArray(n * n)
        assertThat(GlyphExtractor.score(empty, n, n, null, false).ok).isFalse()
    }

    @Test
    fun composeUsesOnlyPaletteColours() {
        val colors = ThemedIconPalette.colors(0xFF8844CC.toInt(), dark = true)
        val glyph = IntArray(4) { if (it < 2) 255 else 0 }
        val out = GlyphExtractor.compose(glyph, IntArray(4) { 255 }, colors.background, colors.foreground)
        assertThat(out[0]).isEqualTo(colors.foreground)
        assertThat(out[3]).isEqualTo(colors.background)
    }

    @Test
    fun fromAlphaUsesLayerAlpha() {
        val layer = IntArray(n * n) { if (it % n in 30..60 && it / n in 30..60) 0xFF123456.toInt() else 0 }
        assertThat(GlyphExtractor.fromAlpha(layer, n, n).ok).isTrue()
    }
}
