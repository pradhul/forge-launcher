package com.sduduzog.slimlauncher.icons

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Glyph alpha (0..255 per pixel) plus quality metrics. */
class GlyphResult(
    val alpha: IntArray,
    val coverage: Float,
    val noise: Float,
    val inverted: Boolean,
    val ok: Boolean
)

/**
 * Turns an app icon into a single-colour glyph mask, Android-themed-icon
 * style. Pure JVM (packed ARGB ints) so it is unit tested.
 */
object GlyphExtractor {
    const val MIN_COVERAGE = 0.03f
    const val MAX_COVERAGE = 0.70f
    const val MAX_NOISE = 28f

    /** Glyph from a layer whose alpha already is the logo (adaptive foreground). */
    fun fromAlpha(pixels: IntArray, width: Int, height: Int, region: IntArray? = null): GlyphResult {
        val alpha = IntArray(pixels.size) { IconPostProcess.alpha(pixels[it]) }
        return score(alpha, width, height, region, inverted = false)
    }

    /**
     * Separates the logo from its dominant plate colour in a full icon bitmap.
     * [region] (optional alpha mask of the icon shape) limits the measured area.
     */
    fun fromBitmap(pixels: IntArray, width: Int, height: Int): GlyphResult {
        val n = width * height
        val opaque = IntArray(n) { if (IconPostProcess.alpha(pixels[it]) > 128) 255 else 0 }
        val bg = dominantColor(pixels, width, height)
            ?: return fromAlpha(pixels, width, height)
        val bgLum = IconPostProcess.luminance(bg)
        val dist = FloatArray(n)
        for (i in 0 until n) {
            val p = pixels[i]
            if (IconPostProcess.alpha(p) <= 128) continue
            val dr = (IconPostProcess.red(p) - IconPostProcess.red(bg)).toFloat()
            val dg = (IconPostProcess.green(p) - IconPostProcess.green(bg)).toFloat()
            val db = (IconPostProcess.blue(p) - IconPostProcess.blue(bg)).toFloat()
            val rgb = sqrt(dr * dr + dg * dg + db * db) / 441.7f
            val lum = kotlin.math.abs(IconPostProcess.luminance(p) - bgLum) / 255f
            dist[i] = max(rgb, lum)
        }
        val t = otsu(dist, opaque).coerceIn(0.12f, 0.6f)
        val hard = BooleanArray(n) { opaque[it] > 0 && dist[it] >= t }
        val cleaned = open(hard, width, height)
        val soft = IntArray(n)
        for (i in 0 until n) {
            if (!cleaned[i] && !(hard[i] && neighbourOn(cleaned, i, width, height))) continue
            val a = smooth(t - 0.08f, t + 0.08f, dist[i])
            soft[i] = (a * 255).toInt().coerceIn(0, 255)
        }
        val direct = score(soft, width, height, opaque, inverted = false)
        if (direct.ok) return direct
        // Inverted: the edge-sampled colour was actually the logo.
        if (direct.coverage > MAX_COVERAGE) {
            val inv = IntArray(n) { if (opaque[it] > 0) 255 - soft[it] else 0 }
            val alt = score(inv, width, height, opaque, inverted = true)
            if (alt.ok) return alt
        }
        return direct
    }

    fun score(alpha: IntArray, width: Int, height: Int, region: IntArray?, inverted: Boolean): GlyphResult {
        var area = 0f
        var on = 0
        var edge = 0
        var regionArea = 0f
        for (y in 0 until height) for (x in 0 until width) {
            val i = y * width + x
            regionArea += if (region == null) 1f else region[i] / 255f
            area += alpha[i] / 255f
            if (alpha[i] >= 128) {
                on++
                if (x == 0 || y == 0 || x == width - 1 || y == height - 1 ||
                    alpha[i - 1] < 128 || alpha[i + 1] < 128 ||
                    alpha[i - width] < 128 || alpha[i + width] < 128) edge++
            }
        }
        val coverage = if (regionArea <= 0f) 0f else area / regionArea
        val noise = if (on == 0) Float.MAX_VALUE else edge / sqrt(on.toFloat())
        val ok = coverage in MIN_COVERAGE..MAX_COVERAGE && noise <= MAX_NOISE
        return GlyphResult(alpha, coverage, noise, inverted, ok)
    }

    /** Most common (quantised) colour along the opaque border ring of the icon. */
    fun dominantColor(pixels: IntArray, width: Int, height: Int): Int? {
        val counts = HashMap<Int, IntArray>()
        val cx = width / 2f; val cy = height / 2f
        for (y in 0 until height) for (x in 0 until width) {
            val p = pixels[y * width + x]
            if (IconPostProcess.alpha(p) <= 200) continue
            // only the outer ~12% ring of the opaque area: sample along rays
            val nx = (x - cx) / cx; val ny = (y - cy) / cy
            if (max(kotlin.math.abs(nx), kotlin.math.abs(ny)) < 0.70f) continue
            val q = (IconPostProcess.red(p) shr 4 shl 8) or (IconPostProcess.green(p) shr 4 shl 4) or (IconPostProcess.blue(p) shr 4)
            val acc = counts.getOrPut(q) { IntArray(4) }
            acc[0]++; acc[1] += IconPostProcess.red(p); acc[2] += IconPostProcess.green(p); acc[3] += IconPostProcess.blue(p)
        }
        val best = counts.values.maxByOrNull { it[0] } ?: return null
        return IconPostProcess.argb(255, best[1] / best[0], best[2] / best[0], best[3] / best[0])
    }

    private fun otsu(values: FloatArray, mask: IntArray): Float {
        val hist = IntArray(64)
        var total = 0
        for (i in values.indices) if (mask[i] > 0) { hist[min(63, (values[i] * 63).toInt())]++; total++ }
        if (total == 0) return 0.2f
        var sum = 0.0
        for (i in 0 until 64) sum += i * hist[i].toDouble()
        var sumB = 0.0; var wB = 0; var best = 0.0; var th = 12
        for (i in 0 until 64) {
            wB += hist[i]; if (wB == 0) continue
            val wF = total - wB; if (wF == 0) break
            sumB += i * hist[i].toDouble()
            val mB = sumB / wB; val mF = (sum - sumB) / wF
            val between = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            if (between > best) { best = between; th = i }
        }
        return (th + 0.5f) / 63f
    }

    /** 3x3 morphological open (erode then dilate) to drop speckle. */
    fun open(src: BooleanArray, w: Int, h: Int): BooleanArray {
        val er = BooleanArray(src.size)
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            er[i] = src[i] && src[i - 1] && src[i + 1] && src[i - w] && src[i + w]
        }
        val out = BooleanArray(src.size)
        for (y in 0 until h) for (x in 0 until w) {
            val i = y * w + x
            out[i] = er[i] || neighbourOn(er, i, w, h)
        }
        return out
    }

    private fun neighbourOn(m: BooleanArray, i: Int, w: Int, h: Int): Boolean {
        val x = i % w; val y = i / w
        return (x > 0 && m[i - 1]) || (x < w - 1 && m[i + 1]) || (y > 0 && m[i - w]) || (y < h - 1 && m[i + w])
    }

    private fun smooth(e0: Float, e1: Float, x: Float): Float {
        val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }

    /** Glyph in [fg] over [bg], clipped by [shape] alpha. */
    fun compose(glyph: IntArray, shape: IntArray, bg: Int, fg: Int): IntArray {
        return IntArray(glyph.size) { i ->
            val s = shape[i]
            if (s == 0) 0 else {
                val t = glyph[i] / 255f
                fun ch(sh: Int): Int {
                    val b = bg ushr sh and 0xFF; val f = fg ushr sh and 0xFF
                    return (b + (f - b) * t).toInt()
                }
                (s shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
            }
        }
    }

    /** Fallback: whole icon mapped dark->light wallpaper colours, clipped to shape. */
    fun duotone(pixels: IntArray, shape: IntArray, dark: Int, light: Int): IntArray {
        val mapped = IconPostProcess.duotone(pixels, dark, light, 1f)
        return IntArray(pixels.size) { i ->
            val s = shape[i]
            if (s == 0) 0 else {
                val src = mapped[i]
                val a = IconPostProcess.alpha(pixels[i]) / 255f
                // transparent holes inside the shape take the light colour
                val r = (IconPostProcess.red(light) + (IconPostProcess.red(src) - IconPostProcess.red(light)) * a).toInt()
                val g = (IconPostProcess.green(light) + (IconPostProcess.green(src) - IconPostProcess.green(light)) * a).toInt()
                val b = (IconPostProcess.blue(light) + (IconPostProcess.blue(src) - IconPostProcess.blue(light)) * a).toInt()
                IconPostProcess.argb(s, r, g, b)
            }
        }
    }
}
