package com.sduduzog.slimlauncher.wallpaper

import com.sduduzog.slimlauncher.icons.ContrastPicker
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.icons.PackThemeLogic

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
        matchBackground: Boolean
    ): PackTheme {
        return when (source) {
            ThemeSource.FROM_WALLPAPER -> {
                wallpaperTheme ?: PackThemeLogic.resolve(matchBackground, packTheme)
            }
            ThemeSource.FROM_ICON_PACK -> {
                PackThemeLogic.resolve(matchBackground, packTheme)
            }
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
        return PackThemeLogic.fromSwatches(
            dominant = primary,
            darkMuted = secondary,
            vibrant = tertiary,
            muted = secondary
        )
    }

    fun isLightSeed(color: Int): Boolean {
        return ContrastPicker.relativeLuminance(color or 0xFF000000.toInt()) >= LIGHT_SEED_LUMINANCE
    }

    fun scrimTint(seed: Int): Int {
        val opaque = seed or 0xFF000000.toInt()
        return if (isLightSeed(opaque)) {
            mixTowardWhite(opaque, 0.35f)
        } else {
            ContrastPicker.ensureDarkBackground(opaque)
        }
    }

    fun drawerGradient(seed: Int): IntArray {
        val tint = scrimTint(seed)
        return intArrayOf(scrim(tint, DRAWER_TOP_ALPHA), scrim(tint, DRAWER_BOTTOM_ALPHA))
    }

    fun homeClockScrim(seed: Int): Int = scrim(scrimTint(seed), HOME_CLOCK_ALPHA)

    fun homeDockScrim(seed: Int): Int = scrim(scrimTint(seed), HOME_DOCK_ALPHA)

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
