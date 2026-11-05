package com.sduduzog.slimlauncher.icons

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pixel-level post-pass used after style transfer (or on its own in tests).
 * Operates on packed ARGB ints so it can run on the JVM.
 */
object IconPostProcess {

    fun apply(
        pixels: IntArray,
        width: Int,
        height: Int,
        spec: PostProcessSpec,
        shape: BackplateShape,
        strength: Float
    ): IntArray {
        require(pixels.size == width * height)
        val blend = strength.coerceIn(0f, 1f)
        var work = pixels.copyOf()
        spec.posterizeLevels?.let { work = posterize(work, it) }
        if (spec.duotoneShadows != null && spec.duotoneHighlights != null && spec.duotoneMix > 0f) {
            work = duotone(work, spec.duotoneShadows, spec.duotoneHighlights, spec.duotoneMix)
        }
        if (spec.palette != null && spec.palette.isNotEmpty() && spec.paletteMix > 0f) {
            work = mapPalette(work, spec.palette, spec.paletteMix)
        }
        if (spec.edgeGlowColor != null && spec.edgeGlowStrength > 0f) {
            work = edgeGlow(work, width, height, spec.edgeGlowColor, spec.edgeGlowStrength)
        }
        if (blend < 1f) {
            work = mix(pixels, work, blend)
        }
        return applyShapeMask(work, width, height, shape)
    }

    fun mix(original: IntArray, stylized: IntArray, amount: Float): IntArray {
        val t = amount.coerceIn(0f, 1f)
        val out = IntArray(original.size)
        for (i in original.indices) {
            val a = original[i]
            val b = stylized[i]
            out[i] = argb(
                lerpChan(alpha(a), alpha(b), t),
                lerpChan(red(a), red(b), t),
                lerpChan(green(a), green(b), t),
                lerpChan(blue(a), blue(b), t)
            )
        }
        return out
    }

    fun duotone(pixels: IntArray, shadows: Int, highlights: Int, mix: Float): IntArray {
        val amount = mix.coerceIn(0f, 1f)
        val out = IntArray(pixels.size)
        for (i in pixels.indices) {
            val src = pixels[i]
            val lum = luminance(src) / 255f
            val mapped = argb(
                alpha(src),
                lerpChan(red(shadows), red(highlights), lum),
                lerpChan(green(shadows), green(highlights), lum),
                lerpChan(blue(shadows), blue(highlights), lum)
            )
            out[i] = mixPixel(src, mapped, amount)
        }
        return out
    }

    fun mapPalette(pixels: IntArray, palette: IntArray, mix: Float): IntArray {
        val amount = mix.coerceIn(0f, 1f)
        val out = IntArray(pixels.size)
        val last = (palette.size - 1).coerceAtLeast(0)
        for (i in pixels.indices) {
            val src = pixels[i]
            val idx = (luminance(src) / 255f * last).roundToInt().coerceIn(0, last)
            val mapped = palette[idx]
            out[i] = argb(
                alpha(src),
                lerpChan(red(src), red(mapped), amount),
                lerpChan(green(src), green(mapped), amount),
                lerpChan(blue(src), blue(mapped), amount)
            )
        }
        return out
    }

    fun posterize(pixels: IntArray, levels: Int): IntArray {
        val steps = levels.coerceAtLeast(2)
        val out = IntArray(pixels.size)
        for (i in pixels.indices) {
            val src = pixels[i]
            out[i] = argb(
                alpha(src),
                quantize(red(src), steps),
                quantize(green(src), steps),
                quantize(blue(src), steps)
            )
        }
        return out
    }

    fun edgeGlow(
        pixels: IntArray,
        width: Int,
        height: Int,
        color: Int,
        strength: Float
    ): IntArray {
        val amount = strength.coerceIn(0f, 2f)
        val out = pixels.copyOf()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx =
                    luminance(pixels[(y - 1) * width + x + 1]) +
                        2 * luminance(pixels[y * width + x + 1]) +
                        luminance(pixels[(y + 1) * width + x + 1]) -
                        luminance(pixels[(y - 1) * width + x - 1]) -
                        2 * luminance(pixels[y * width + x - 1]) -
                        luminance(pixels[(y + 1) * width + x - 1])
                val gy =
                    luminance(pixels[(y + 1) * width + x - 1]) +
                        2 * luminance(pixels[(y + 1) * width + x]) +
                        luminance(pixels[(y + 1) * width + x + 1]) -
                        luminance(pixels[(y - 1) * width + x - 1]) -
                        2 * luminance(pixels[(y - 1) * width + x]) -
                        luminance(pixels[(y - 1) * width + x + 1])
                val mag = (sqrt((gx * gx + gy * gy).toDouble()) / 1020.0).toFloat().coerceIn(0f, 1f)
                if (mag <= 0.05f) continue
                val idx = y * width + x
                val src = pixels[idx]
                val glow = mag * amount
                out[idx] = argb(
                    alpha(src),
                    clamp(red(src) + (red(color) * glow).roundToInt()),
                    clamp(green(src) + (green(color) * glow).roundToInt()),
                    clamp(blue(src) + (blue(color) * glow).roundToInt())
                )
            }
        }
        return out
    }

    fun applyShapeMask(
        pixels: IntArray,
        width: Int,
        height: Int,
        shape: BackplateShape
    ): IntArray {
        val out = pixels.copyOf()
        val cx = (width - 1) / 2f
        val cy = (height - 1) / 2f
        val rx = width / 2f
        val ry = height / 2f
        for (y in 0 until height) {
            for (x in 0 until width) {
                val nx = (x - cx) / rx
                val ny = (y - cy) / ry
                val coverage = when (shape) {
                    BackplateShape.CIRCLE -> {
                        val d = sqrt(nx * nx + ny * ny)
                        smoothstep(1.02f, 0.96f, d)
                    }
                    BackplateShape.SQUIRCLE -> {
                        val d = (abs(nx).toDouble().pow(4.0) + abs(ny).toDouble().pow(4.0)).toFloat()
                        smoothstep(1.05f, 0.88f, d)
                    }
                }
                val idx = y * width + x
                val src = out[idx]
                out[idx] = argb(
                    (alpha(src) * coverage).roundToInt().coerceIn(0, 255),
                    red(src),
                    green(src),
                    blue(src)
                )
            }
        }
        return out
    }

    fun luminance(color: Int): Int {
        return (0.299f * red(color) + 0.587f * green(color) + 0.114f * blue(color)).roundToInt()
    }

    fun alpha(c: Int) = c ushr 24 and 0xFF
    fun red(c: Int) = c ushr 16 and 0xFF
    fun green(c: Int) = c ushr 8 and 0xFF
    fun blue(c: Int) = c and 0xFF

    fun argb(a: Int, r: Int, g: Int, b: Int): Int {
        return (clamp(a) shl 24) or (clamp(r) shl 16) or (clamp(g) shl 8) or clamp(b)
    }

    private fun mixPixel(a: Int, b: Int, t: Float): Int = argb(
        lerpChan(alpha(a), alpha(b), t),
        lerpChan(red(a), red(b), t),
        lerpChan(green(a), green(b), t),
        lerpChan(blue(a), blue(b), t)
    )

    private fun lerpChan(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).roundToInt()

    private fun quantize(channel: Int, levels: Int): Int {
        val step = 255f / (levels - 1)
        return (Math.round(channel / step) * step).roundToInt().coerceIn(0, 255)
    }

    private fun clamp(v: Int) = max(0, min(255, v))

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }
}
