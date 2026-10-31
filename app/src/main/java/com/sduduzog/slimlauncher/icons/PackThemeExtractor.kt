package com.sduduzog.slimlauncher.icons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.palette.graphics.Palette
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackThemeExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: IconPackStore,
    private val stylizer: OnDeviceIconStylizer
) {
    fun extract(bitmap: Bitmap): PackTheme {
        val palette = Palette.from(bitmap)
            .clearFilters()
            .maximumColorCount(16)
            .generate()
        return PackThemeLogic.fromSwatches(
            dominant = palette.dominantSwatch?.rgb,
            darkMuted = palette.darkMutedSwatch?.rgb,
            vibrant = palette.vibrantSwatch?.rgb,
            muted = palette.mutedSwatch?.rgb,
            lightVibrant = palette.lightVibrantSwatch?.rgb
        )
    }

    fun loadStyleBitmap(pack: IconPack): Bitmap {
        val custom = store.getCustomStyleFile(pack.id)
        if (custom != null) {
            BitmapFactory.decodeFile(custom.absolutePath)?.let { return it }
        }
        val preset = when {
            pack.stylePresetId == StylePreset.CUSTOM_ID -> StylePreset.CUSTOM
            else -> StylePreset.byId(pack.stylePresetId) ?: StylePreset.ALL.first()
        }
        return stylizer.loadPresetStyle(preset)
    }

    fun ensureTheme(packId: String): IconPack? {
        val pack = store.getPack(packId) ?: return null
        if (pack.theme != null) return pack
        val theme = extract(loadStyleBitmap(pack))
        val latest = store.getPack(packId) ?: pack
        val updated = latest.copy(theme = theme)
        store.updatePack(updated)
        return updated
    }
}

@Singleton
class PackThemeController @Inject constructor(
    private val prefs: IconPreferences,
    private val store: IconPackStore,
    private val extractor: PackThemeExtractor
) {
    fun active(): PackTheme {
        val packId = prefs.appliedPackId ?: return PackTheme.STOCK
        val pack = extractor.ensureTheme(packId) ?: store.getPack(packId)
        return PackThemeLogic.resolve(prefs.matchBackgroundToPack, pack?.theme)
    }
}
