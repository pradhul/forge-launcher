package com.sduduzog.slimlauncher.wallpaper

object WallpaperPackLogic {
    const val PACK_ID = "wallpaper"

    /** Bump when themed-icon rendering changes so saved pack PNGs regenerate. */
    const val RENDER_VERSION = "r3"

    fun cacheKey(wallpaperId: String, strengthHundredths: Int): String {
        return "$wallpaperId:${strengthHundredths.coerceIn(0, 100)}"
    }

    fun strengthHundredths(strength: Float): Int {
        return (strength.coerceIn(0f, 1f) * 100f).toInt()
    }

    fun shouldRegenerate(
        styledIconsEnabled: Boolean,
        currentKey: String?,
        cachedKey: String?,
        inFlightKey: String?
    ): Boolean {
        if (!styledIconsEnabled) return false
        if (currentKey.isNullOrBlank()) return false
        if (currentKey == cachedKey) return false
        if (currentKey == inFlightKey) return false
        return true
    }

    fun hashSamples(width: Int, height: Int, samples: IntArray): String {
        var hash = width * 31 + height
        for (pixel in samples) {
            hash = 31 * hash + pixel
        }
        return "${width}x$height:${hash.toUInt().toString(16)}"
    }

    fun systemWallpaperId(id: Int): String? {
        return if (id >= 0) "sys:$id" else null
    }
}
