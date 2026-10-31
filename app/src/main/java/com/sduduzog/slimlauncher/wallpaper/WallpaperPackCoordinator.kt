package com.sduduzog.slimlauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import com.sduduzog.slimlauncher.icons.IconPackGenerator
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.IconRasterizer
import com.sduduzog.slimlauncher.icons.ImageUtils
import com.sduduzog.slimlauncher.icons.InstalledAppsQuery
import com.sduduzog.slimlauncher.icons.IconPackStore
import com.sduduzog.slimlauncher.icons.PackCreateSpec
import com.sduduzog.slimlauncher.icons.PackThemeExtractor
import com.sduduzog.slimlauncher.icons.StylePreset
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperPackCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: IconPreferences,
    private val store: IconPackStore,
    private val generator: IconPackGenerator,
    private val extractor: PackThemeExtractor,
    private val installedAppsQuery: InstalledAppsQuery
) {
    private val wallpaperManager = WallpaperManager.getInstance(context)
    @Volatile
    private var inFlightKey: String? = null

    fun checkCurrentWallpaper() {
        val bitmap = captureWallpaper() ?: return
        onWallpaperUpdated(bitmap)
    }

    fun onWallpaperUpdated(bitmap: Bitmap) {
        prefs.wallpaperTheme = extractor.extract(bitmap)
        prefs.wallpaperPresent = true
        val key = currentCacheKey(bitmap)
        if (!prefs.wallpaperStyledIcons) {
            if (prefs.appliedPackId == WallpaperPackLogic.PACK_ID) {
                prefs.revertToStock()
            }
            return
        }
        if (!WallpaperPackLogic.shouldRegenerate(
                styledIconsEnabled = true,
                currentKey = key,
                cachedKey = prefs.wallpaperStyleHash,
                inFlightKey = inFlightKey
            )
        ) {
            if (prefs.appliedPackId != WallpaperPackLogic.PACK_ID) {
                store.getPack(WallpaperPackLogic.PACK_ID)?.let { prefs.applyPack(it.id) }
            }
            return
        }
        inFlightKey = key
        val styleBytes = ImageUtils.encodePng(downscaleStyle(bitmap))
        val pack = store.createPack(
            PackCreateSpec(
                name = "Wallpaper",
                stylePrompt = "Wallpaper",
                stylePresetId = StylePreset.WALLPAPER_ID,
                styleStrength = prefs.wallpaperStyleStrength,
                customStyleBytes = styleBytes,
                fixedId = WallpaperPackLogic.PACK_ID
            )
        )
        prefs.wallpaperStyleHash = key
        if (prefs.appliedPackId != pack.id) {
            prefs.applyPack(pack.id)
        }
        generator.startPack(pack, installedAppsQuery.load())
        inFlightKey = null
    }

    fun setStyledIcons(enabled: Boolean) {
        prefs.wallpaperStyledIcons = enabled
        if (!enabled) {
            prefs.revertToStock()
            return
        }
        prefs.wallpaperStyleHash = null
        checkCurrentWallpaper()
    }

    fun setStrength(strength: Float) {
        prefs.wallpaperStyleStrength = strength
        prefs.wallpaperStyleHash = null
        checkCurrentWallpaper()
    }

    private fun currentCacheKey(bitmap: Bitmap): String {
        val wallpaperId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            WallpaperPackLogic.systemWallpaperId(
                wallpaperManager.getWallpaperId(WallpaperManager.FLAG_SYSTEM)
            )
        } else {
            null
        } ?: hashBitmap(bitmap)
        return WallpaperPackLogic.cacheKey(
            wallpaperId,
            WallpaperPackLogic.strengthHundredths(prefs.wallpaperStyleStrength)
        )
    }

    private fun captureWallpaper(): Bitmap? {
        val drawable = runCatching { wallpaperManager.drawable }.getOrNull()
            ?: runCatching { wallpaperManager.peekDrawable() }.getOrNull()
            ?: return null
        return runCatching { IconRasterizer.toBitmap(drawable, STYLE_CAPTURE_PX) }.getOrNull()
    }

    private fun downscaleStyle(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= STYLE_CAPTURE_PX) return bitmap
        val scale = STYLE_CAPTURE_PX.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    }

    private fun hashBitmap(bitmap: Bitmap): String {
        val scaled = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        val samples = IntArray(16 * 16)
        scaled.getPixels(samples, 0, 16, 0, 0, 16, 16)
        return WallpaperPackLogic.hashSamples(bitmap.width, bitmap.height, samples)
    }

    companion object {
        private const val STYLE_CAPTURE_PX = 512
    }
}
