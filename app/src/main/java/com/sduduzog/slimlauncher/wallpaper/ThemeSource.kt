package com.sduduzog.slimlauncher.wallpaper

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

    fun fromColors(primary: Int, secondary: Int?, tertiary: Int?): PackTheme {
        return PackThemeLogic.fromSwatches(
            dominant = primary,
            darkMuted = secondary,
            vibrant = tertiary,
            muted = secondary
        )
    }

    fun scrim(color: Int, alpha: Int = SCRIM_ALPHA): Int {
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }
}
