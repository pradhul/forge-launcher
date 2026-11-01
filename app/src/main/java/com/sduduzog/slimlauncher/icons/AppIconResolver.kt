package com.sduduzog.slimlauncher.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserManager
import android.util.DisplayMetrics
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIconResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: IconPackStore,
    private val prefs: IconPreferences,
    private val stylizer: OnDeviceIconStylizer
) {
    private val bitmaps = LruCache<String, Bitmap>(IconCacheLogic.MAX_ENTRIES)

    fun shouldShowIcons(): Boolean = prefs.showIcons

    fun evictCache() {
        synchronized(bitmaps) { bitmaps.evictAll() }
    }

    fun resolveDrawable(
        packageName: String,
        activityName: String,
        userSerial: Long,
        allowStyle: Boolean = true
    ): Drawable {
        val applied = appliedPack()
        // Always render an icon (pack or stock). A persisted "toggle icons off"
        // must not blank the whole launcher.
        return when (IconApplyLogic.resolve(true, applied, packageName)) {
            ResolvedIcon.Hidden -> fallbackIcon()
            is ResolvedIcon.Pack -> decodePackIcon(applied!!.id, packageName)
                ?: styleOrStock(applied, packageName, activityName, userSerial, allowStyle)
            is ResolvedIcon.PackFallback -> styleOrStock(
                applied!!,
                packageName,
                activityName,
                userSerial,
                allowStyle
            )
            ResolvedIcon.Stock -> loadStockIcon(packageName, activityName, userSerial)
                ?: fallbackIcon()
        }
    }

    fun resolveBitmap(
        packageName: String,
        activityName: String,
        userSerial: Long,
        size: Int = iconPx(),
        allowStyle: Boolean = true
    ): Bitmap {
        val key = cacheKey(packageName, activityName, userSerial, size)
        synchronized(bitmaps) { bitmaps.get(key) }?.let { return it }
        val bitmap = IconRasterizer.toBitmap(
            resolveDrawable(packageName, activityName, userSerial, allowStyle),
            size
        )
        val visible = if (IconRasterizer.hasVisiblePixels(bitmap)) {
            bitmap
        } else {
            IconRasterizer.letterTile(packageName, size)
        }
        synchronized(bitmaps) { bitmaps.put(key, visible) }
        return visible
    }

    fun bind(
        imageView: ImageView,
        packageName: String,
        activityName: String,
        userSerial: Long
    ) {
        imageView.visibility = View.VISIBLE
        imageView.imageTintList = null
        imageView.imageTintMode = null
        ImageViewCompat.setImageTintList(imageView, null)
        imageView.clearColorFilter()
        imageView.colorFilter = null
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.adjustViewBounds = false
        imageView.setImageDrawable(null)
        imageView.setImageBitmap(
            resolveBitmap(
                packageName,
                activityName,
                userSerial,
                iconPx(),
                allowStyle = false
            )
        )
    }

    fun iconPx(): Int = IconSizing.devicePx(context)

    private fun appliedPack(): IconPack? {
        return prefs.appliedPackId?.let { store.getPack(it) }
    }

    private fun cacheKey(
        packageName: String,
        activityName: String,
        userSerial: Long,
        size: Int
    ): String {
        val packId = prefs.appliedPackId
        return IconCacheLogic.key(
            packageName = packageName,
            activityName = activityName,
            userSerial = userSerial,
            packId = packId,
            themeHash = IconCacheLogic.themeHash(packId, prefs.wallpaperTheme),
            size = size
        )
    }

    private fun styleOrStock(
        pack: IconPack,
        packageName: String,
        activityName: String,
        userSerial: Long,
        allowStyle: Boolean
    ): Drawable {
        if (!allowStyle) {
            return loadStockIcon(packageName, activityName, userSerial) ?: fallbackIcon()
        }
        return styledFallback(pack, packageName, activityName, userSerial)
    }

    fun loadStockIcon(
        packageName: String,
        activityName: String,
        userSerial: Long
    ): Drawable? {
        val manager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userHandle = try {
            manager.getUserForSerialNumber(userSerial)
        } catch (_: Exception) {
            null
        } ?: Process.myUserHandle()
        val component = ComponentName(packageName, activityName)
        val density = context.resources.displayMetrics.densityDpi
            .coerceAtLeast(DisplayMetrics.DENSITY_MEDIUM)
        val pm = context.packageManager

        try {
            val activities = launcher.getActivityList(packageName, userHandle)
            val info = activities.firstOrNull { it.componentName == component }
                ?: activities.firstOrNull()
            info?.getIcon(density)?.let { return it }
        } catch (_: Exception) {
        }
        try {
            return pm.getActivityIcon(component)
        } catch (_: Exception) {
        }
        try {
            return pm.getApplicationIcon(packageName)
        } catch (_: Exception) {
        }
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            return appInfo.loadUnbadgedIcon(pm)
        } catch (_: Exception) {
        }
        return null
    }

    fun fallbackIcon(): Drawable = context.packageManager.defaultActivityIcon

    fun stockIconPng(
        packageName: String,
        activityName: String,
        userSerial: Long,
        size: Int = 256
    ): ByteArray? {
        val drawable = loadStockIcon(packageName, activityName, userSerial) ?: return null
        return ImageUtils.encodePng(IconRasterizer.toBitmap(drawable, size))
    }

    private fun styledFallback(
        pack: IconPack,
        packageName: String,
        activityName: String,
        userSerial: Long
    ): Drawable {
        val source = loadStockIcon(packageName, activityName, userSerial) ?: fallbackIcon()
        val preset = StylePreset.byId(pack.stylePresetId)
        return try {
            val bytes = if (pack.generatorMode == GenerationMode.PALETTE) {
                val theme = pack.theme ?: prefs.wallpaperTheme ?: PackTheme.STOCK
                stylizer.renderForStyle(source, theme, pack.styleStrength, prefs.iconStyle)
            } else {
                stylizer.styleWithoutTransfer(source, preset)
            }
            store.saveIcon(pack.id, packageName, bytes)
            decodePackIcon(pack.id, packageName) ?: BitmapDrawable(
                context.resources,
                IconRasterizer.toBitmap(source, iconPx())
            )
        } catch (_: Exception) {
            BitmapDrawable(context.resources, IconRasterizer.toBitmap(source, iconPx()))
        }
    }

    private fun decodePackIcon(packId: String, packageName: String): Drawable? {
        val file = store.getIconFile(packId, packageName) ?: return null
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return BitmapDrawable(context.resources, bitmap)
    }

    companion object {
        const val ICON_PX = 128
    }
}
