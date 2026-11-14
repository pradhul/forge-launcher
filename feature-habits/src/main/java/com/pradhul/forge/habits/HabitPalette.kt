package com.pradhul.forge.habits

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Wallpaper colours passed in by the launcher. The module derives card, text,
 * and chip colours and nudges them to WCAG AA against the card.
 */
data class HabitPalette(
    val background: Int,
    val backgroundEnd: Int,
    val onBackground: Int,
    val accent: Int,
    val onAccent: Int,
    val darkSurface: Boolean
) {
    val card: Int
    val text: Int
    val muted: Int
    val success: Int
    val danger: Int
    val popup: Int

    init {
        val resolved = HabitContrast.resolve(this)
        card = resolved.card
        text = resolved.text
        muted = resolved.muted
        success = resolved.success
        danger = resolved.danger
        popup = resolved.popup
    }

    fun toJson(): String = org.json.JSONObject()
        .put("background", background)
        .put("backgroundEnd", backgroundEnd)
        .put("onBackground", onBackground)
        .put("accent", accent)
        .put("onAccent", onAccent)
        .put("darkSurface", darkSurface)
        .toString()

    companion object {
        val FALLBACK = HabitPalette(
            background = 0xFF1A1028.toInt(),
            backgroundEnd = 0xFF120818.toInt(),
            onBackground = 0xFFF7F0FF.toInt(),
            accent = 0xFFD4B4FF.toInt(),
            onAccent = 0xFF2A1248.toInt(),
            darkSurface = true
        )

        fun fromJson(raw: String?): HabitPalette {
            if (raw.isNullOrBlank()) return FALLBACK
            return try {
                val obj = org.json.JSONObject(raw)
                HabitPalette(
                    background = obj.getInt("background"),
                    backgroundEnd = obj.getInt("backgroundEnd"),
                    onBackground = obj.getInt("onBackground"),
                    accent = obj.getInt("accent"),
                    onAccent = obj.getInt("onAccent"),
                    darkSurface = obj.optBoolean("darkSurface", true)
                )
            } catch (_: Exception) {
                FALLBACK
            }
        }
    }
}

data class ResolvedColors(
    val card: Int,
    val text: Int,
    val muted: Int,
    val success: Int,
    val danger: Int,
    val popup: Int
)

object HabitContrast {
    const val WCAG_AA = 4.5
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val BLACK = 0xFF000000.toInt()
    private const val UP = 0xFF1B8A4A.toInt()
    private const val DOWN = 0xFFC62828.toInt()

    internal fun resolve(palette: HabitPalette): ResolvedColors {
        val base = if (palette.darkSurface) tone(palette.accent, 18.0, 1.2, 0.5)
        else tone(palette.accent, 92.0, 1.2, 0.4)
        val card = (base and 0x00FFFFFF) or (0xE6 shl 24)
        val opaque = card or 0xFF000000.toInt()
        val text = nudge(
            pick(opaque, intArrayOf(palette.onBackground, WHITE, BLACK)),
            opaque,
            WCAG_AA
        )
        val muted = (text and 0x00FFFFFF) or (0xB3 shl 24)
        val popup = (opaque and 0x00FFFFFF) or (0xF2 shl 24)
        return ResolvedColors(
            card = card,
            text = text,
            muted = muted,
            success = nudge(UP, opaque, 3.0),
            danger = nudge(DOWN, opaque, 3.0),
            popup = popup
        )
    }

    fun contrastRatio(first: Int, second: Int): Double {
        val a = luminance(first)
        val b = luminance(second)
        val lighter = max(a, b)
        val darker = min(a, b)
        return (lighter + 0.05) / (darker + 0.05)
    }

    fun pick(background: Int, candidates: IntArray): Int {
        val ranked = candidates.sortedByDescending { contrastRatio(background, it) }
        return ranked.firstOrNull { contrastRatio(background, it) >= WCAG_AA } ?: ranked.first()
    }

    fun nudge(foreground: Int, background: Int, minRatio: Double): Int {
        var current = foreground or 0xFF000000.toInt()
        val bg = background or 0xFF000000.toInt()
        var guard = 0
        val toward = if (luminance(current) >= luminance(bg)) WHITE else BLACK
        while (contrastRatio(bg, current) < minRatio && guard < 14) {
            current = mix(current, toward, 0.18f)
            guard++
        }
        return current
    }

    private fun tone(color: Int, lightness: Double, chromaMul: Double, mix: Double): Int {
        val r = red(color) / 255.0
        val g = green(color) / 255.0
        val b = blue(color) / 255.0
        val l = (0.2126 * r + 0.7152 * g + 0.0722 * b)
        val target = (lightness / 100.0).coerceIn(0.0, 1.0)
        val scale = if (l < 0.001) target else (target / l) * chromaMul
        fun chan(v: Double): Int {
            val mixed = v * (1 - mix) + target * mix
            return (mixed * scale * 255.0).toInt().coerceIn(0, 255)
        }
        return 0xFF000000.toInt() or (chan(r) shl 16) or (chan(g) shl 8) or chan(b)
    }

    private fun mix(color: Int, target: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        return rgb(
            (red(color) + ((red(target) - red(color)) * t)).toInt(),
            (green(color) + ((green(target) - green(color)) * t)).toInt(),
            (blue(color) + ((blue(target) - blue(color)) * t)).toInt()
        )
    }

    private fun luminance(color: Int): Double {
        fun lin(c: Int): Double {
            val s = c / 255.0
            return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * lin(red(color)) + 0.7152 * lin(green(color)) + 0.0722 * lin(blue(color))
    }

    private fun red(c: Int) = c shr 16 and 0xFF
    private fun green(c: Int) = c shr 8 and 0xFF
    private fun blue(c: Int) = c and 0xFF
    private fun rgb(r: Int, g: Int, b: Int) =
        0xFF000000.toInt() or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
}
