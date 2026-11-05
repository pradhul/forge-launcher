package com.sduduzog.slimlauncher.wallpaper

/**
 * Resolution order for a wallpaper bitmap the drawer can blur.
 *
 * On API 33+ a third-party app's [android.app.WallpaperManager.getDrawable],
 * [android.app.WallpaperManager.peekDrawable] and
 * [android.app.WallpaperManager.getWallpaperFile] all need
 * READ_WALLPAPER_INTERNAL (signature/privileged). They throw
 * [SecurityException] or return null — never the user's wallpaper.
 */
object WallpaperCaptureLogic {
    enum class Source { CACHE, DRAWABLE, PEEK, FILE, NONE }

    fun pickSource(
        cacheAvailable: Boolean,
        drawableAvailable: Boolean,
        peekAvailable: Boolean,
        fileAvailable: Boolean
    ): Source {
        return when {
            cacheAvailable -> Source.CACHE
            drawableAvailable -> Source.DRAWABLE
            peekAvailable -> Source.PEEK
            fileAvailable -> Source.FILE
            else -> Source.NONE
        }
    }

    fun isPermissionDenied(error: Throwable): Boolean {
        if (error is SecurityException) return true
        val message = error.message ?: return false
        return message.contains("READ_WALLPAPER", ignoreCase = true) ||
            message.contains("wallpaper", ignoreCase = true) &&
            message.contains("permission", ignoreCase = true)
    }

    fun describe(source: Source, width: Int = 0, height: Int = 0): String {
        return when (source) {
            Source.CACHE -> "cached wallpaper ${width}x$height"
            Source.DRAWABLE -> "getDrawable ${width}x$height"
            Source.PEEK -> "peekDrawable ${width}x$height"
            Source.FILE -> "getWallpaperFile ${width}x$height"
            Source.NONE -> "no wallpaper bitmap (READ_WALLPAPER_INTERNAL / empty)"
        }
    }
}
