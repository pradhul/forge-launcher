package com.sduduzog.slimlauncher.icons

import kotlin.math.roundToInt

/**
 * Subtle wallpaper tint for full-size app icons. The original artwork stays
 * intact: each pixel is nudged a small amount toward the theme accent at the
 * pixel's own luminance, so shapes, contrast and detail are preserved.
 * Pure-JVM so it can be unit tested.
 */
object ThemedIconTint {
    const val MIN_AMOUNT = 0.06f
    const val MAX_AMOUNT = 0.20f

    /** Maps the user's 0.2..1 strength slider onto a low tint amount. */
    fun amountFor(strength: Float): Float {
        val s = strength.coerceIn(0f, 1f)
        return (MIN_AMOUNT + (MAX_AMOUNT - MIN_AMOUNT) * s).coerceIn(0f, MAX_AMOUNT)
    }

    fun tint(pixels: IntArray, accent: Int, strength: Float): IntArray {
        val amount = amountFor(strength)
        val out = IntArray(pixels.size)
        for (i in pixels.indices) {
            val src = pixels[i]
            val a = IconPostProcess.alpha(src)
            if (a == 0) {
                out[i] = 0
                continue
            }
            val target = luminanceMatched(accent, IconPostProcess.luminance(src))
            out[i] = IconPostProcess.argb(
                a,
                lerp(IconPostProcess.red(src), IconPostProcess.red(target), amount),
                lerp(IconPostProcess.green(src), IconPostProcess.green(target), amount),
                lerp(IconPostProcess.blue(src), IconPostProcess.blue(target), amount)
            )
        }
        return out
    }

    /** Accent hue scaled (or lifted toward white) to the requested luminance. */
    fun luminanceMatched(accent: Int, lum: Int): Int {
        val accLum = IconPostProcess.luminance(accent).coerceAtLeast(1)
        val r = IconPostProcess.red(accent)
        val g = IconPostProcess.green(accent)
        val b = IconPostProcess.blue(accent)
        return if (lum <= accLum) {
            val k = lum / accLum.toFloat()
            IconPostProcess.argb(255, (r * k).roundToInt(), (g * k).roundToInt(), (b * k).roundToInt())
        } else {
            val k = (lum - accLum) / (255f - accLum).coerceAtLeast(1f)
            IconPostProcess.argb(
                255,
                (r + (255 - r) * k).roundToInt(),
                (g + (255 - g) * k).roundToInt(),
                (b + (255 - b) * k).roundToInt()
            )
        }
    }

    private fun lerp(a: Int, b: Int, t: Float): Int = (a + (b - a) * t).roundToInt()
}
