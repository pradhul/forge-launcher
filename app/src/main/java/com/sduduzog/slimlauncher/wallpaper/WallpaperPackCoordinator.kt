package com.sduduzog.slimlauncher.wallpaper

import com.sduduzog.slimlauncher.icons.IconStyle
import android.graphics.Bitmap
import com.sduduzog.slimlauncher.icons.GenerationMode
import com.sduduzog.slimlauncher.icons.IconPackGenerator
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.ImageUtils
import com.sduduzog.slimlauncher.icons.InstalledAppsQuery
import com.sduduzog.slimlauncher.icons.IconPackStore
import com.sduduzog.slimlauncher.icons.PackCreateSpec
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.icons.PackThemeExtractor
import com.sduduzog.slimlauncher.icons.StylePreset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperPackCoordinator @Inject constructor(
    private val prefs: IconPreferences,
    private val store: IconPackStore,
    private val generator: IconPackGenerator,
    private val wallpaperStore: WallpaperBitmapStore,
    private val extractor: PackThemeExtractor,
    private val installedAppsQuery: InstalledAppsQuery
) {
    @Volatile
    private var inFlightKey: String? = null

    fun checkCurrentWallpaper() {
        val theme = prefs.wallpaperTheme ?: return
        applyTheme(
            theme = theme,
            paletteKey = prefs.lastAppliedPaletteKey ?: "stored",
            bitmap = forgeBitmap(),
            force = false
        )
    }

    fun onWallpaperUpdated(bitmap: Bitmap) {
        val extracted = extractor.extract(bitmap)
        val swatches = WallpaperSwatches(
            primary = extracted.seed,
            secondary = extracted.background,
            tertiary = extracted.accent,
            supportsDarkText = !extracted.darkSurface,
            supportsDarkTheme = extracted.darkSurface,
            source = ThemeExtractSource.BITMAP
        )
        val theme = ForgeThemeMapper.map(swatches)
        val key = ForgeThemeMapper.paletteKey(swatches)
        prefs.lastAppliedPaletteKey = key
        applyTheme(theme, key, bitmap, force = true)
    }

    fun applyTheme(
        theme: PackTheme,
        paletteKey: String,
        bitmap: Bitmap?,
        force: Boolean
    ) {
        prefs.wallpaperTheme = theme
        prefs.wallpaperPresent = true
        if (!prefs.wallpaperStyledIcons) {
            if (prefs.appliedPackId == WallpaperPackLogic.PACK_ID) {
                prefs.revertToStock()
            }
            return
        }
        val style = prefs.iconStyle
        if (style == IconStyle.ORIGINAL) {
            if (prefs.appliedPackId == WallpaperPackLogic.PACK_ID) prefs.revertToStock()
            return
        }
        val artistic = bitmap != null && style == IconStyle.ARTISTIC
        val key = if (artistic) {
            WallpaperPackLogic.cacheKey(
                "${WallpaperPackLogic.RENDER_VERSION}:${style.storageValue}:bitmap:$paletteKey",
                WallpaperPackLogic.strengthHundredths(prefs.wallpaperStyleStrength)
            )
        } else {
            WallpaperPackLogic.cacheKey(
                "${WallpaperPackLogic.RENDER_VERSION}:${style.storageValue}:palette:$paletteKey",
                WallpaperPackLogic.strengthHundredths(prefs.wallpaperStyleStrength)
            )
        }
        if (!force && !WallpaperPackLogic.shouldRegenerate(
                styledIconsEnabled = true,
                currentKey = key,
                cachedKey = prefs.wallpaperStyleHash,
                inFlightKey = inFlightKey
            )
        ) {
            store.getPack(WallpaperPackLogic.PACK_ID)?.let { pack ->
                if (prefs.appliedPackId != pack.id) {
                    prefs.applyPack(pack.id)
                }
            }
            return
        }
        inFlightKey = key
        val pack = if (artistic && bitmap != null) {
            val styleBytes = ImageUtils.encodePng(downscaleStyle(bitmap))
            store.createPack(
                PackCreateSpec(
                    name = "Wallpaper",
                    stylePrompt = "Wallpaper",
                    generatorMode = GenerationMode.ON_DEVICE,
                    stylePresetId = StylePreset.WALLPAPER_ID,
                    styleStrength = prefs.wallpaperStyleStrength,
                    customStyleBytes = styleBytes,
                    fixedId = WallpaperPackLogic.PACK_ID
                )
            )
        } else {
            store.createPack(
                PackCreateSpec(
                    name = "Wallpaper",
                    stylePrompt = "Wallpaper palette",
                    generatorMode = GenerationMode.PALETTE,
                    stylePresetId = StylePreset.WALLPAPER_ID,
                    styleStrength = prefs.wallpaperStyleStrength,
                    fixedId = WallpaperPackLogic.PACK_ID
                )
            )
        }
        val themed = pack.copy(theme = theme)
        store.updatePack(themed)
        prefs.wallpaperStyleHash = key
        if (prefs.appliedPackId != themed.id) {
            prefs.applyPack(themed.id)
        }
        generator.startPack(themed, installedAppsQuery.load())
        inFlightKey = null
    }

    fun setStyledIcons(enabled: Boolean) {
        prefs.wallpaperStyledIcons = enabled
        if (!enabled) {
            prefs.revertToStock()
            return
        }
        prefs.wallpaperStyleHash = null
        val theme = prefs.wallpaperTheme ?: return
        applyTheme(theme, prefs.lastAppliedPaletteKey ?: "styled", forgeBitmap(), force = true)
    }

    fun setIconStyle(style: IconStyle) {
        prefs.iconStyle = style
        prefs.wallpaperStyleHash = null
        if (style == IconStyle.ORIGINAL) {
            if (prefs.appliedPackId == WallpaperPackLogic.PACK_ID) prefs.revertToStock()
            return
        }
        prefs.wallpaperStyledIcons = true
        val theme = prefs.wallpaperTheme ?: return
        applyTheme(theme, prefs.lastAppliedPaletteKey ?: "style", forgeBitmap(), force = true)
    }

    fun setStrength(strength: Float) {
        prefs.wallpaperStyleStrength = strength
        prefs.wallpaperStyleHash = null
        val theme = prefs.wallpaperTheme ?: return
        applyTheme(theme, prefs.lastAppliedPaletteKey ?: "strength", forgeBitmap(), force = true)
    }

    private fun forgeBitmap(): Bitmap? {
        return if (wallpaperStore.exists()) wallpaperStore.load() else null
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

    companion object {
        private const val STYLE_CAPTURE_PX = 512
    }
}
