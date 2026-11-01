package com.sduduzog.slimlauncher.icons

import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Background/foreground pair for monochrome themed icons. */
data class ThemedColors(val background: Int, val foreground: Int)

/**
 * Tonal palette for themed icons, derived from one wallpaper seed colour.
 * Tones follow CIE L* (Material "tone"): light = bg tone 90 / glyph tone 25,
 * dark = bg tone 22 / glyph tone 88. Contrast is pushed to WCAG >= 4.5.
 */
object ThemedIconPalette {
    const val MIN_CONTRAST = 4.5

    fun forTheme(theme: PackTheme): ThemedColors = colors(theme.accent, theme.darkSurface)

    fun colors(seed: Int, dark: Boolean): ThemedColors {
        var bgTone = if (dark) 22.0 else 90.0
        var fgTone = if (dark) 88.0 else 25.0
        var bg = tone(seed, bgTone, if (dark) 0.45 else 0.35)
        var fg = tone(seed, fgTone, if (dark) 0.35 else 0.6)
        var guard = 0
        while (contrast(bg, fg) < MIN_CONTRAST && guard++ < 40) {
            if (dark) { fgTone = min(100.0, fgTone + 1.5); bgTone = max(0.0, bgTone - 0.5) }
            else { fgTone = max(0.0, fgTone - 1.5); bgTone = min(100.0, bgTone + 0.5) }
            bg = tone(seed, bgTone, if (dark) 0.45 else 0.35)
            fg = tone(seed, fgTone, if (dark) 0.35 else 0.6)
        }
        return ThemedColors(bg, fg)
    }

    /** Dark/light ends for the duotone fallback. */
    fun duotoneEnds(colors: ThemedColors): Pair<Int, Int> {
        return if (lstar(colors.background) > lstar(colors.foreground)) {
            colors.foreground to colors.background
        } else colors.background to colors.foreground
    }

    /** Seed hue, saturation scaled by [satScale], lightness solved to hit L* [target]. */
    fun tone(seed: Int, target: Double, satScale: Double): Int {
        val hsl = toHsl(seed)
        val s = (hsl[1] * satScale).coerceIn(0.0, 0.85)
        var lo = 0.0
        var hi = 1.0
        var best = fromHsl(hsl[0], s, 0.5)
        repeat(24) {
            val mid = (lo + hi) / 2
            best = fromHsl(hsl[0], s, mid)
            if (lstar(best) < target) lo = mid else hi = mid
        }
        return best
    }

    fun relativeLuminance(c: Int): Double {
        fun ch(v: Int): Double {
            val x = v / 255.0
            return if (x <= 0.03928) x / 12.92 else ((x + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * ch(c ushr 16 and 0xFF) + 0.7152 * ch(c ushr 8 and 0xFF) + 0.0722 * ch(c and 0xFF)
    }

    fun contrast(a: Int, b: Int): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    fun lstar(c: Int): Double {
        val y = relativeLuminance(c)
        val f = if (y > 216.0 / 24389.0) cbrt(y) else (24389.0 / 27.0 * y + 16) / 116.0
        return 116.0 * f - 16.0
    }

    private fun toHsl(c: Int): DoubleArray {
        val r = (c ushr 16 and 0xFF) / 255.0
        val g = (c ushr 8 and 0xFF) / 255.0
        val b = (c and 0xFF) / 255.0
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b))
        val l = (mx + mn) / 2
        if (mx == mn) return doubleArrayOf(0.0, 0.0, l)
        val d = mx - mn
        val s = if (l > 0.5) d / (2 - mx - mn) else d / (mx + mn)
        val h = when (mx) {
            r -> ((g - b) / d + (if (g < b) 6 else 0))
            g -> ((b - r) / d + 2)
            else -> ((r - g) / d + 4)
        } * 60
        return doubleArrayOf(h, s, l)
    }

    private fun fromHsl(h: Double, s: Double, l: Double): Int {
        val c = (1 - abs(2 * l - 1)) * s
        val hp = h / 60
        val x = c * (1 - abs(hp % 2 - 1))
        val (r1, g1, b1) = when {
            hp < 1 -> Triple(c, x, 0.0); hp < 2 -> Triple(x, c, 0.0)
            hp < 3 -> Triple(0.0, c, x); hp < 4 -> Triple(0.0, x, c)
            hp < 5 -> Triple(x, 0.0, c); else -> Triple(c, 0.0, x)
        }
        val m = l - c / 2
        fun q(v: Double) = ((v + m) * 255).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (q(r1) shl 16) or (q(g1) shl 8) or q(b1)
    }
}
