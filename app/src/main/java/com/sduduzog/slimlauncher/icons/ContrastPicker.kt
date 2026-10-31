package com.sduduzog.slimlauncher.icons

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

    private fun linear(channel: Double): Double {
        return if (channel <= 0.04045) {
            channel / 12.92
        } else {
            ((channel + 0.055) / 1.055).pow(2.4)
        }
    }
}
