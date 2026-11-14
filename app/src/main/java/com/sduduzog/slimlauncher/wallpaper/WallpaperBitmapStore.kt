package com.sduduzog.slimlauncher.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Last wallpaper this app applied (Bing daily, or a debug/setWallpaper call).
 *
 * Third-party apps on API 33+ cannot re-read the live wallpaper
 * ([WallpaperManager.getDrawable] needs READ_WALLPAPER_INTERNAL), so the
 * drawer blur fallback has to use this copy.
 */
@Singleton
class WallpaperBitmapStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun save(bitmap: Bitmap) = saveTo(context, bitmap)

    fun load(): Bitmap? = loadFrom(context)

    fun exists(): Boolean = file(context).let { it.isFile && it.length() > 0 }

    fun clear() {
        file(context).delete()
    }

    companion object {
        const val FILE_NAME = "cached_wallpaper.jpg"
        const val JPEG_QUALITY = 85
        const val MAX_EDGE = 720

        fun file(context: Context): File = File(context.filesDir, FILE_NAME)

        fun saveTo(context: Context, bitmap: Bitmap): Boolean {
            val scaled = scaleForCache(bitmap)
            return runCatching {
                FileOutputStream(file(context)).use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
                true
            }.getOrDefault(false)
        }

        fun loadFrom(context: Context): Bitmap? {
            val stored = file(context)
            if (!stored.isFile || stored.length() == 0L) return null
            return runCatching {
                BitmapFactory.decodeFile(stored.absolutePath)
            }.getOrNull()
        }

        fun scaleForCache(bitmap: Bitmap): Bitmap {
            val longest = maxOf(bitmap.width, bitmap.height)
            if (longest <= MAX_EDGE) return bitmap
            val scale = MAX_EDGE.toFloat() / longest
            return Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        }
    }
}
