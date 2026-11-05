package com.sduduzog.slimlauncher.icons

import com.sduduzog.slimlauncher.wallpaper.ForgeThemeMapper

/**
 * Cheap wallpaper theming for icons when the live wallpaper bitmap is
 * unreadable (Android 13+ / third-party live wallpapers such as Tapet).
 *
 * Tone-maps the glyph onto the extracted palette and plants it on a
 * saturation-clamped adaptive-shape backplate. Used instead of Magenta
 * style transfer when Forge did not set the wallpaper itself.
 */
object PaletteIconRecolor {

    fun recolor(
        pixels: IntArray,
        width: Int,
        height: Int,
        theme: PackTheme,
        strength: Float
    ): IntArray {
        require(pixels.size == width * height)
        val amount = strength.coerceIn(0.35f, 0.9f)
        val plate = ForgeThemeMapper.iconBackground(theme.accent, theme.darkSurface)
        val shadows = if (theme.darkSurface) {
            ContrastPicker.mixToward(plate, theme.background, 0.35f)
        } else {
            ContrastPicker.mixToward(theme.onBackground, plate, 0.45f)
        }
        val highlights = ContrastPicker.nudgeForeground(theme.accent, plate)
        val mapped = IconPostProcess.duotone(pixels, shadows, highlights, amount)
        val composed = IntArray(pixels.size)
        for (i in pixels.indices) {
            val src = pixels[i]
            val alpha = IconPostProcess.alpha(src)
            composed[i] = if (alpha <= 8) {
                plate
            } else {
                over(mapped[i], plate)
            }
        }
        return IconPostProcess.applyShapeMask(composed, width, height, BackplateShape.SQUIRCLE)
    }

    fun over(fore: Int, back: Int): Int {
        val fa = IconPostProcess.alpha(fore) / 255f
        if (fa <= 0f) return back
        if (fa >= 1f) return fore
        val ba = IconPostProcess.alpha(back) / 255f
        val outA = fa + ba * (1f - fa)
        if (outA <= 0f) return 0
        fun chan(extract: (Int) -> Int): Int {
            val f = extract(fore) / 255f
            val b = extract(back) / 255f
            return ((f * fa + b * ba * (1f - fa)) / outA * 255f).toInt()
        }
        return IconPostProcess.argb(
            (outA * 255f).toInt(),
            chan(IconPostProcess::red),
            chan(IconPostProcess::green),
            chan(IconPostProcess::blue)
        )
    }
}
