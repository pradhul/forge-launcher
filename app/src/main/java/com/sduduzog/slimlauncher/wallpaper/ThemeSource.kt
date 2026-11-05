package com.sduduzog.slimlauncher.wallpaper

import com.sduduzog.slimlauncher.icons.ContrastPicker
import com.sduduzog.slimlauncher.icons.PackTheme

enum class ThemeSource(val id: String) {
    FROM_ICON_PACK("icon_pack"),
    FROM_WALLPAPER("wallpaper");

    companion object {
        fun fromId(id: String?): ThemeSource? = entries.firstOrNull { it.id == id }
    }
}

object ThemeSourceLogic {
    fun defaultSource(hasWallpaper: Boolean): ThemeSource {
        return if (hasWallpaper) ThemeSource.FROM_WALLPAPER else ThemeSource.FROM_ICON_PACK
    }

    fun selected(
        explicitId: String?,
        bingEnabled: Boolean,
        wallpaperSet: Boolean
    ): ThemeSource {
        ThemeSource.fromId(explicitId)?.let { return it }
        return defaultSource(bingEnabled || wallpaperSet)
    }

    fun resolve(
        source: ThemeSource,
        wallpaperTheme: PackTheme?,
        packTheme: PackTheme?,
        matchBackground: Boolean = true
    ): PackTheme {
        if (wallpaperTheme != null) return wallpaperTheme
        return if (matchBackground) {
            packTheme ?: PackTheme.STOCK
        } else {
            PackTheme.STOCK
        }
    }
}

object WallpaperThemeLogic {
    const val SCRIM_ALPHA = 0x99
    const val SCRIM_END_ALPHA = 0x66
    const val DRAWER_TOP_ALPHA = 0x8C
    const val DRAWER_BOTTOM_ALPHA = 0xCC
    const val HOME_CLOCK_ALPHA = 0x4D
    const val HOME_DOCK_ALPHA = 0x59
    const val LIGHT_SEED_LUMINANCE = 0.45

    fun fromColors(primary: Int, secondary: Int?, tertiary: Int?): PackTheme {
        return ForgeThemeMapper.map(
            WallpaperSwatches(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                supportsDarkText = isLightSeed(primary),
                supportsDarkTheme = !isLightSeed(primary),
                source = ThemeExtractSource.WALLPAPER_COLORS
            )
        )
    }

    fun isLightSeed(color: Int): Boolean {
        return ContrastPicker.relativeLuminance(color or 0xFF000000.toInt()) >= LIGHT_SEED_LUMINANCE
    }

    fun scrimTint(seed: Int): Int {
        return ForgeThemeMapper.surfaceTone(seed)
    }

    fun drawerGradient(seed: Int, fadePercent: Int = 100): IntArray {
        val tint = scrimTint(seed)
        return intArrayOf(
            scrim(tint, LauncherFadeLogic.scaleAlpha(DRAWER_TOP_ALPHA, fadePercent)),
            scrim(tint, LauncherFadeLogic.scaleAlpha(DRAWER_BOTTOM_ALPHA, fadePercent))
        )
    }

    fun homeClockScrim(
        seed: Int,
        fadePercent: Int = LauncherFadeLogic.DEFAULT_HOME_FADE
    ): Int = scrim(scrimTint(seed), LauncherFadeLogic.scaleAlpha(HOME_CLOCK_ALPHA, fadePercent))

    fun homeDockScrim(
        seed: Int,
        fadePercent: Int = LauncherFadeLogic.DEFAULT_HOME_FADE
    ): Int = scrim(scrimTint(seed), LauncherFadeLogic.scaleAlpha(HOME_DOCK_ALPHA, fadePercent))

    fun scrim(color: Int, alpha: Int = SCRIM_ALPHA): Int {
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    fun mixTowardWhite(color: Int, amount: Float): Int {
        val t = amount.coerceIn(0f, 1f)
        fun channel(value: Int): Int = (value + ((255 - value) * t)).toInt().coerceIn(0, 255)
        return ContrastPicker.rgb(
            channel(ContrastPicker.red(color)),
            channel(ContrastPicker.green(color)),
            channel(ContrastPicker.blue(color))
        )
    }
}
