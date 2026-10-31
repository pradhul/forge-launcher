package com.sduduzog.slimlauncher.wallpaper

import com.sduduzog.slimlauncher.icons.ContrastPicker
import com.sduduzog.slimlauncher.icons.PackTheme

enum class ThemeExtractSource(val logName: String) {
    WALLPAPER_COLORS("wallpaperColors"),
    DYNAMIC("dynamic"),
    BITMAP("bitmap")
}

data class WallpaperSwatches(
    val primary: Int,
    val secondary: Int? = null,
    val tertiary: Int? = null,
    val supportsDarkText: Boolean = false,
    val supportsDarkTheme: Boolean = false,
    val dynamicAccent1: Int? = null,
    val dynamicAccent2: Int? = null,
    val dynamicAccent3: Int? = null,
    val dynamicNeutral50: Int? = null,
    val dynamicNeutral900: Int? = null,
    val source: ThemeExtractSource = ThemeExtractSource.WALLPAPER_COLORS
)

object ForgeThemeMapper {
    const val MIN_CONTRAST = ContrastPicker.WCAG_AA
    const val DOCK_ALPHA = 0x8C
    const val SEARCH_PILL_ALPHA = 0x99
    const val ICON_SAT_MIN = 0.22f
    const val ICON_SAT_MAX = 0.55f
    const val SURFACE_SAT_MIN = 0.10f
    const val SURFACE_SAT_MAX = 0.48f
    const val LIGHT_LUMINANCE = 0.45

    fun map(swatches: WallpaperSwatches): PackTheme {
        val darkSurface = isDarkSurface(swatches)
        val seed = opaque(swatches.primary)
        val surfaceSeed = swatches.secondary?.let { opaque(it) } ?: seed
        val background = when {
            darkSurface && swatches.dynamicNeutral900 != null ->
                ContrastPicker.ensureDarkBackground(opaque(swatches.dynamicNeutral900))
            !darkSurface && swatches.dynamicNeutral50 != null ->
                ContrastPicker.ensureLightBackground(opaque(swatches.dynamicNeutral50))
            darkSurface -> ContrastPicker.ensureDarkBackground(surfaceSeed)
            else -> ContrastPicker.ensureLightBackground(surfaceSeed)
        }.let { clampSurface(it) }

        val endSeed = swatches.dynamicNeutral900?.takeIf { darkSurface }
            ?: swatches.dynamicNeutral50?.takeIf { !darkSurface }
            ?: swatches.primary
        val backgroundEnd = if (darkSurface) {
            ContrastPicker.ensureDarkBackground(opaque(endSeed), maxLuminance = 0.28)
        } else {
            ContrastPicker.ensureLightBackground(opaque(endSeed), minLuminance = 0.70)
        }.let { clampSurface(it) }

        val rawAccent = swatches.dynamicAccent1
            ?: swatches.tertiary
            ?: swatches.dynamicAccent2
            ?: swatches.secondary
            ?: seed
        val accent = iconBackground(opaque(rawAccent), darkSurface)
        val onBackground = ContrastPicker.nudgeForeground(
            labelCandidate(darkSurface, swatches.supportsDarkText),
            background
        )
        val onAccent = ContrastPicker.nudgeForeground(
            ContrastPicker.pickReadable(accent),
            accent
        )
        return PackTheme(
            background = background,
            backgroundEnd = backgroundEnd,
            onBackground = onBackground,
            accent = accent,
            onAccent = onAccent,
            seed = seed,
            darkSurface = darkSurface
        )
    }

    fun isDarkSurface(swatches: WallpaperSwatches): Boolean {
        if (swatches.supportsDarkText && !swatches.supportsDarkTheme) return false
        if (swatches.supportsDarkTheme && !swatches.supportsDarkText) return true
        return ContrastPicker.relativeLuminance(opaque(swatches.primary)) < LIGHT_LUMINANCE
    }

    fun nudgeForContrast(
        foreground: Int,
        background: Int,
        minRatio: Double = MIN_CONTRAST
    ): Int = ContrastPicker.nudgeForeground(foreground, background, minRatio)

    fun iconBackground(accent: Int, darkSurface: Boolean): Int {
        val clamped = ContrastPicker.clampSaturation(opaque(accent), ICON_SAT_MIN, ICON_SAT_MAX)
        return if (darkSurface) {
            ContrastPicker.ensureDarkBackground(clamped, maxLuminance = 0.38)
        } else {
            ContrastPicker.ensureLightBackground(clamped, minLuminance = 0.42)
                .let { ContrastPicker.clampSaturation(it, ICON_SAT_MIN, ICON_SAT_MAX) }
        }
    }

    fun translucentTone(seed: Int, alpha: Int, darkSurface: Boolean): Int {
        val tone = surfaceTone(seed, darkSurface)
        return (tone and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
    }

    fun surfaceTone(seed: Int, darkSurface: Boolean = ContrastPicker.relativeLuminance(opaque(seed)) < LIGHT_LUMINANCE): Int {
        val opaque = opaque(seed)
        val lifted = when {
            isNearBlack(opaque) -> if (darkSurface) 0xFF3C3C48.toInt() else 0xFFE6E2DA.toInt()
            isNearWhite(opaque) -> if (darkSurface) 0xFF3C3C48.toInt() else 0xFFE6E2DA.toInt()
            darkSurface -> ContrastPicker.ensureDarkBackground(opaque, maxLuminance = 0.22)
            else -> ContrastPicker.mixToward(opaque, 0xFFF4F1EA.toInt(), 0.28f)
        }
        return clampSurface(lifted)
    }

    fun paletteKey(swatches: WallpaperSwatches): String {
        fun hex(color: Int?): String = color?.let { String.format("%08X", opaque(it)) } ?: "-"
        return listOf(
            swatches.source.logName,
            hex(swatches.primary),
            hex(swatches.secondary),
            hex(swatches.tertiary),
            if (swatches.supportsDarkText) "dt" else "-",
            if (swatches.supportsDarkTheme) "dark" else "-",
            hex(swatches.dynamicAccent1),
            hex(swatches.dynamicNeutral50),
            hex(swatches.dynamicNeutral900)
        ).joinToString(":")
    }

    fun colorsChanged(previousKey: String?, currentKey: String?): Boolean {
        if (currentKey.isNullOrBlank()) return false
        return previousKey != currentKey
    }

    fun shouldApply(previousKey: String?, currentKey: String?, force: Boolean): Boolean {
        if (force) return true
        return colorsChanged(previousKey, currentKey)
    }

    fun describe(
        source: String,
        swatches: WallpaperSwatches,
        theme: PackTheme
    ): String {
        fun hex(color: Int?): String = color?.let { String.format("#%08X", opaque(it)) } ?: "-"
        return "source=$source primary=${hex(swatches.primary)} " +
            "secondary=${hex(swatches.secondary)} tertiary=${hex(swatches.tertiary)} " +
            "accent1=${hex(swatches.dynamicAccent1)} " +
            "darkSurface=${theme.darkSurface} bg=${hex(theme.background)} " +
            "onBg=${hex(theme.onBackground)} accent=${hex(theme.accent)}"
    }

    fun resolveSourceName(
        hasWallpaperColors: Boolean,
        hasDynamic: Boolean,
        hasBitmap: Boolean
    ): String {
        return buildList {
            if (hasWallpaperColors) add(ThemeExtractSource.WALLPAPER_COLORS.logName)
            if (hasDynamic) add(ThemeExtractSource.DYNAMIC.logName)
            if (hasBitmap) add(ThemeExtractSource.BITMAP.logName)
        }.joinToString("+").ifBlank { ThemeExtractSource.WALLPAPER_COLORS.logName }
    }

    fun primarySource(
        hasWallpaperColors: Boolean,
        hasDynamic: Boolean,
        hasBitmap: Boolean
    ): ThemeExtractSource {
        return when {
            hasDynamic -> ThemeExtractSource.DYNAMIC
            hasWallpaperColors -> ThemeExtractSource.WALLPAPER_COLORS
            hasBitmap -> ThemeExtractSource.BITMAP
            else -> ThemeExtractSource.WALLPAPER_COLORS
        }
    }

    private fun labelCandidate(darkSurface: Boolean, supportsDarkText: Boolean): Int {
        return if (!darkSurface || supportsDarkText) {
            0xFF161616.toInt()
        } else {
            0xFFF4F4F4.toInt()
        }
    }

    private fun clampSurface(color: Int): Int {
        return ContrastPicker.clampSaturation(opaque(color), SURFACE_SAT_MIN, SURFACE_SAT_MAX)
    }

    fun isNearBlack(color: Int): Boolean = ContrastPicker.relativeLuminance(opaque(color)) < 0.012

    fun isNearWhite(color: Int): Boolean = ContrastPicker.relativeLuminance(opaque(color)) > 0.96

    fun opaque(color: Int): Int = color or 0xFF000000.toInt()
}
