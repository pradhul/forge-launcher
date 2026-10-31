package com.sduduzog.slimlauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build

object BingWallpaperApplier {
    fun whichFlags(): Int {
        return WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
    }

    fun colorDecodeOptions(): BitmapFactory.Options {
        return BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inPremultiplied = true
            inDither = false
        }
    }

    fun isGreyscaleConfig(config: Bitmap.Config?): Boolean {
        return config == Bitmap.Config.ALPHA_8
    }

    fun ensureFullColor(bitmap: Bitmap): Bitmap {
        return if (isGreyscaleConfig(bitmap.config) || bitmap.config == null) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else {
            bitmap
        }
    }

    fun setWallpaper(context: Context, bitmap: Bitmap) {
        val color = ensureFullColor(bitmap)
        WallpaperBitmapStore.saveTo(context, color)
        val manager = WallpaperManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager.setBitmap(color, null, true, whichFlags())
        } else {
            @Suppress("DEPRECATION")
            manager.setBitmap(color)
        }
    }
}
