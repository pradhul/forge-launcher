package com.sduduzog.slimlauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build

object BingWallpaperApplier {
    fun setWallpaper(context: Context, bitmap: Bitmap, lockScreen: Boolean) {
        val manager = WallpaperManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            var flags = WallpaperManager.FLAG_SYSTEM
            if (lockScreen) {
                flags = flags or WallpaperManager.FLAG_LOCK
            }
            manager.setBitmap(bitmap, null, true, flags)
        } else {
            @Suppress("DEPRECATION")
            manager.setBitmap(bitmap)
        }
    }
}
