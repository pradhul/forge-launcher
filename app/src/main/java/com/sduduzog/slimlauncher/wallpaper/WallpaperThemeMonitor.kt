package com.sduduzog.slimlauncher.wallpaper

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.PackThemeExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperThemeMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: IconPreferences,
    private val extractor: PackThemeExtractor,
    private val coordinator: WallpaperPackCoordinator,
    private val snapshotter: WallpaperSnapshotter,
    private val store: WallpaperBitmapStore
) {
    private val wallpaperManager = WallpaperManager.getInstance(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private var colorsListener: WallpaperManager.OnColorsChangedListener? = null
    private var wallpaperReceiver: BroadcastReceiver? = null
    private var debounce: Runnable? = null

    fun start() {
        registerColorsListener()
        registerWallpaperReceiver()
        scheduleApply(force = prefs.lastAppliedPaletteKey == null, reason = "start")
    }

    fun onHostResume() {
        scheduleApply(force = false, reason = "resume")
    }

    fun onUiModeChanged() {
        scheduleApply(force = false, reason = "uiMode")
    }

    fun refreshFromSystem() {
        scheduleApply(force = false, reason = "refresh")
    }

    fun applyNow(onStarted: () -> Unit = {}, onDone: () -> Unit = {}) {
        debounce?.let { mainHandler.removeCallbacks(it) }
        debounce = null
        mainHandler.post(onStarted)
        worker.execute {
            applyInternal(force = true, reason = "manual")
            mainHandler.post(onDone)
        }
    }

    private fun registerColorsListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return
        if (colorsListener != null) return
        val listener = WallpaperManager.OnColorsChangedListener { _, _ ->
            scheduleApply(force = false, reason = "colors")
        }
        colorsListener = listener
        wallpaperManager.addOnColorsChangedListener(listener, mainHandler)
    }

    private fun registerWallpaperReceiver() {
        if (wallpaperReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_WALLPAPER_CHANGED) {
                    scheduleApply(force = false, reason = "wallpaperChanged")
                }
            }
        }
        wallpaperReceiver = receiver
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun scheduleApply(force: Boolean, reason: String) {
        debounce?.let { mainHandler.removeCallbacks(it) }
        val task = Runnable {
            worker.execute { applyInternal(force, reason) }
        }
        debounce = task
        mainHandler.postDelayed(task, if (force) 0L else DEBOUNCE_MS)
    }

    private fun applyInternal(force: Boolean, reason: String) {
        if (!force &&
            prefs.lastAppliedPaletteKey != null &&
            !isDefaultLauncher()
        ) {
            Log.i(TAG, "skip not-default-launcher reason=$reason")
            return
        }
        val extracted = extract()
        val key = ForgeThemeMapper.paletteKey(extracted.swatches)
        if (!ForgeThemeMapper.shouldApply(prefs.lastAppliedPaletteKey, key, force)) {
            Log.i(TAG, "skip unchanged key=$key reason=$reason")
            return
        }
        val theme = ForgeThemeMapper.map(extracted.swatches)
        Log.i(TAG, ForgeThemeMapper.describe(extracted.sourceName, extracted.swatches, theme))
        prefs.wallpaperTheme = theme
        prefs.wallpaperPresent = true
        prefs.lastAppliedPaletteKey = key
        snapshotter.invalidate()
        coordinator.applyTheme(
            theme = theme,
            paletteKey = key,
            bitmap = extracted.forgeBitmap,
            force = force
        )
    }

    private fun extract(): Extracted {
        val colors = readWallpaperColors()
        val dynamic = readDynamicPalette()
        val forgeBitmap = if (store.exists()) store.load() else null
        val hasColors = colors != null
        val hasDynamic = dynamic != null
        val hasBitmap = forgeBitmap != null
        val source = ForgeThemeMapper.primarySource(hasColors, hasDynamic, hasBitmap)
        val sourceName = ForgeThemeMapper.resolveSourceName(hasColors, hasDynamic, hasBitmap)

        val swatches = when {
            colors != null -> swatchesFromColors(colors, dynamic, source)
            dynamic != null -> WallpaperSwatches(
                primary = dynamic.accent1,
                secondary = dynamic.neutral900,
                tertiary = dynamic.accent2,
                supportsDarkText = ContrastPickerLight(dynamic.neutral50),
                supportsDarkTheme = !ContrastPickerLight(dynamic.neutral900),
                dynamicAccent1 = dynamic.accent1,
                dynamicAccent2 = dynamic.accent2,
                dynamicAccent3 = dynamic.accent3,
                dynamicNeutral50 = dynamic.neutral50,
                dynamicNeutral900 = dynamic.neutral900,
                source = ThemeExtractSource.DYNAMIC
            )
            forgeBitmap != null -> {
                val fromBitmap = extractor.extract(forgeBitmap)
                WallpaperSwatches(
                    primary = fromBitmap.seed,
                    secondary = fromBitmap.background,
                    tertiary = fromBitmap.accent,
                    supportsDarkText = !fromBitmap.darkSurface,
                    supportsDarkTheme = fromBitmap.darkSurface,
                    source = ThemeExtractSource.BITMAP
                )
            }
            else -> WallpaperSwatches(
                primary = 0xFF121212.toInt(),
                source = ThemeExtractSource.WALLPAPER_COLORS
            )
        }
        return Extracted(swatches, forgeBitmap, sourceName)
    }

    private fun swatchesFromColors(
        colors: WallpaperColors,
        dynamic: DynamicPalette?,
        source: ThemeExtractSource
    ): WallpaperSwatches {
        val hints = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            colors.colorHints
        } else {
            0
        }
        val supportsDarkText = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
            hints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
        val supportsDarkTheme = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            hints and WallpaperColors.HINT_SUPPORTS_DARK_THEME != 0
        return WallpaperSwatches(
            primary = colors.primaryColor.toArgb(),
            secondary = colors.secondaryColor?.toArgb(),
            tertiary = colors.tertiaryColor?.toArgb(),
            supportsDarkText = supportsDarkText,
            supportsDarkTheme = supportsDarkTheme,
            dynamicAccent1 = dynamic?.accent1,
            dynamicAccent2 = dynamic?.accent2,
            dynamicAccent3 = dynamic?.accent3,
            dynamicNeutral50 = dynamic?.neutral50,
            dynamicNeutral900 = dynamic?.neutral900,
            source = source
        )
    }

    private fun readWallpaperColors(): WallpaperColors? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return null
        return runCatching {
            wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        }.getOrNull()
    }

    private fun readDynamicPalette(): DynamicPalette? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val resources = context.resources
        return runCatching {
            DynamicPalette(
                accent1 = resources.getColor(android.R.color.system_accent1_200, null),
                accent2 = resources.getColor(android.R.color.system_accent2_200, null),
                accent3 = resources.getColor(android.R.color.system_accent3_200, null),
                neutral50 = resources.getColor(android.R.color.system_neutral1_50, null),
                neutral900 = resources.getColor(android.R.color.system_neutral1_900, null)
            )
        }.getOrNull()
    }

    private fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolved?.activityInfo?.packageName == context.packageName
    }

    private data class DynamicPalette(
        val accent1: Int,
        val accent2: Int,
        val accent3: Int,
        val neutral50: Int,
        val neutral900: Int
    )

    private data class Extracted(
        val swatches: WallpaperSwatches,
        val forgeBitmap: android.graphics.Bitmap?,
        val sourceName: String
    )

    private companion object {
        const val TAG = "ForgeTheme"
        const val DEBOUNCE_MS = 450L

        fun ContrastPickerLight(color: Int): Boolean {
            return com.sduduzog.slimlauncher.icons.ContrastPicker.relativeLuminance(
                color or 0xFF000000.toInt()
            ) >= ForgeThemeMapper.LIGHT_LUMINANCE
        }
    }
}
