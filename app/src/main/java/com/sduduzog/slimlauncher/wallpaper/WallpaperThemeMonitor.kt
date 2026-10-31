package com.sduduzog.slimlauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.IconRasterizer
import com.sduduzog.slimlauncher.icons.PackThemeExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperThemeMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: IconPreferences,
    private val extractor: PackThemeExtractor,
    private val coordinator: WallpaperPackCoordinator,
    private val snapshotter: WallpaperSnapshotter
) {
    private val wallpaperManager = WallpaperManager.getInstance(context)
    private var colorsListener: WallpaperManager.OnColorsChangedListener? = null

    fun start() {
        refreshFromSystem()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && colorsListener == null) {
            val listener = WallpaperManager.OnColorsChangedListener { colors, _ ->
                applyColors(colors)
            }
            colorsListener = listener
            wallpaperManager.addOnColorsChangedListener(
                listener,
                Handler(Looper.getMainLooper())
            )
        }
    }

    fun refreshFromSystem() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val colors = runCatching {
                wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            }.getOrNull()
            if (colors != null) {
                applyColors(colors)
                return
            }
        }
        applyFromDrawable()
    }

    private fun applyColors(colors: android.app.WallpaperColors?) {
        if (colors == null) {
            applyFromDrawable()
            return
        }
        prefs.wallpaperTheme = WallpaperThemeLogic.fromColors(
            primary = colors.primaryColor.toArgb(),
            secondary = colors.secondaryColor?.toArgb(),
            tertiary = colors.tertiaryColor?.toArgb()
        )
        prefs.wallpaperPresent = true
        snapshotter.invalidate()
        coordinator.checkCurrentWallpaper()
    }

    private fun applyFromDrawable() {
        snapshotter.invalidate()
        coordinator.checkCurrentWallpaper()
        if (prefs.wallpaperTheme != null) return
        val drawable = runCatching { wallpaperManager.drawable }.getOrNull() ?: return
        val bitmap = runCatching { IconRasterizer.toBitmap(drawable, 160) }.getOrNull() ?: return
        prefs.wallpaperTheme = extractor.extract(bitmap)
        prefs.wallpaperPresent = true
    }
}
