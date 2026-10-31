package com.sduduzog.slimlauncher.icons

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserManager
import android.view.View
import android.widget.ImageView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIconResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: IconPackStore,
    private val prefs: IconPreferences
) {

    fun shouldShowIcons(): Boolean = prefs.showIcons

    fun resolveDrawable(
        packageName: String,
        activityName: String,
        userSerial: Long
    ): Drawable? {
        val packs = store.listPacks()
        val applied = IconApplyLogic.appliedPackOrNull(prefs.appliedPackId, packs)
        return when (IconApplyLogic.resolve(prefs.showIcons, applied, packageName)) {
            ResolvedIcon.Hidden -> null
            is ResolvedIcon.Pack -> decodePackIcon(applied!!.id, packageName)
                ?: loadStockIcon(packageName, activityName, userSerial)
            ResolvedIcon.Stock -> loadStockIcon(packageName, activityName, userSerial)
        }
    }

    fun bind(
        imageView: ImageView,
        packageName: String,
        activityName: String,
        userSerial: Long
    ) {
        if (!prefs.showIcons) {
            imageView.visibility = View.GONE
            imageView.setImageDrawable(null)
            return
        }
        imageView.visibility = View.VISIBLE
        imageView.setImageDrawable(
            resolveDrawable(packageName, activityName, userSerial)
                ?: context.packageManager.defaultActivityIcon
        )
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
        return try {
            launcher.getActivityList(packageName, userHandle)
                .firstOrNull { it.componentName == component }
                ?.getIcon(0)
                ?: context.packageManager.getActivityIcon(component)
        } catch (_: Exception) {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        }
    }

    fun stockIconPng(
        packageName: String,
        activityName: String,
        userSerial: Long,
        size: Int = 256
    ): ByteArray? {
        val drawable = loadStockIcon(packageName, activityName, userSerial) ?: return null
        return ImageUtils.drawableToPng(drawable, size)
    }

    private fun decodePackIcon(packId: String, packageName: String): Drawable? {
        val file = store.getIconFile(packId, packageName) ?: return null
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return BitmapDrawable(context.resources, bitmap)
    }
}
