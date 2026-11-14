package com.sduduzog.slimlauncher.icons

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ContrastPicker {
    const val WCAG_AA = 4.5
    const val WHITE = 0xFFFFFFFF.toInt()
    const val BLACK = 0xFF000000.toInt()

    fun red(color: Int): Int = color shr 16 and 0xFF
    fun green(color: Int): Int = color shr 8 and 0xFF
    fun blue(color: Int): Int = color and 0xFF
    fun rgb(r: Int, g: Int, b: Int): Int {
        return 0xFF000000.toInt() or (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
    }

    fun relativeLuminance(color: Int): Double {
        val r = linear(red(color) / 255.0)
        val g = linear(green(color) / 255.0)
        val b = linear(blue(color) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun contrastRatio(first: Int, second: Int): Double {
        val a = relativeLuminance(first)
        val b = relativeLuminance(second)
        val lighter = max(a, b)
        val darker = min(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun pickReadable(
        background: Int,
        candidates: IntArray = intArrayOf(WHITE, BLACK)
    ): Int {
        val ranked = candidates.sortedByDescending { contrastRatio(background, it) }
        return ranked.firstOrNull { contrastRatio(background, it) >= WCAG_AA }
            ?: ranked.first()
    }

    fun ensureDarkBackground(color: Int, maxLuminance: Double = 0.22): Int {
        var current = color or 0xFF000000.toInt()
        var luminance = relativeLuminance(current)
        var guard = 0
        while (luminance > maxLuminance && guard < 12) {
            current = rgb(
                (red(current) * 0.72).toInt(),
                (green(current) * 0.72).toInt(),
                (blue(current) * 0.72).toInt()
            )
            luminance = relativeLuminance(current)
            guard++
        }
        return current
    }

    fun ensureLightBackground(color: Int, minLuminance: Double = 0.78): Int {
        var current = color or 0xFF000000.toInt()
        var luminance = relativeLuminance(current)
        var guard = 0
        while (luminance < minLuminance && guard < 12) {
            current = mixToward(current, WHITE, 0.22f)
            luminance = relativeLuminance(current)
            guard++
        }
        return current
    }

    fun mixToward(color: Int, target: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        return rgb(
            (red(color) + ((red(target) - red(color)) * t)).toInt(),
            (green(color) + ((green(target) - green(color)) * t)).toInt(),
            (blue(color) + ((blue(target) - blue(color)) * t)).toInt()
        )
    }

    fun nudgeForeground(foreground: Int, background: Int, minRatio: Double = WCAG_AA): Int {
        var current = foreground or 0xFF000000.toInt()
        val bg = background or 0xFF000000.toInt()
        if (contrastRatio(current, bg) >= minRatio) return current
        val towardWhite = relativeLuminance(bg) < 0.5
        val target = if (towardWhite) WHITE else BLACK
        var guard = 0
        while (guard < 32 && contrastRatio(current, bg) < minRatio) {
            current = mixToward(current, target, 0.18f)
            guard++
        }
        if (contrastRatio(current, bg) < minRatio) {
            current = target
        }
        return current
    }

    fun saturation(color: Int): Float {
        val r = red(color) / 255f
        val g = green(color) / 255f
        val b = blue(color) / 255f
        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        if (max <= 0f) return 0f
        return (max - min) / max
    }

    fun clampSaturation(color: Int, minSat: Float, maxSat: Float): Int {
        val hsv = toHsv(color)
        hsv[1] = hsv[1].coerceIn(minSat, maxSat)
        return fromHsv(hsv)
    }

    fun toHsv(color: Int): FloatArray {
        val r = red(color) / 255f
        val g = green(color) / 255f
        val b = blue(color) / 255f
        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val delta = max - min
        val hue = when {
            delta <= 0f -> 0f
            max == r -> ((g - b) / delta).let { t -> (t % 6f + 6f) % 6f } * 60f
            max == g -> ((b - r) / delta + 2f) * 60f
            else -> ((r - g) / delta + 4f) * 60f
        }
        val sat = if (max <= 0f) 0f else delta / max
        return floatArrayOf(hue, sat, max)
    }

    fun fromHsv(hsv: FloatArray): Int {
        val h = ((hsv[0] % 360f) + 360f) % 360f
        val s = hsv[1].coerceIn(0f, 1f)
        val v = hsv[2].coerceIn(0f, 1f)
        val c = v * s
        val x = c * (1f - abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (rp, gp, bp) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return rgb(
            ((rp + m) * 255f).toInt(),
            ((gp + m) * 255f).toInt(),
            ((bp + m) * 255f).toInt()
        )
    }

    private fun linear(channel: Double): Double {
        return if (channel <= 0.04045) {
            channel / 12.92
        } else {
            ((channel + 0.055) / 1.055).pow(2.4)
        }
    }
}
