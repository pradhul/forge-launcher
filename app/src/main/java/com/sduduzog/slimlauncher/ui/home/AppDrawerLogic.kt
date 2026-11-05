package com.sduduzog.slimlauncher.ui.home

import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.models.HomeApp

object AppDrawerLogic {
    fun filter(apps: List<App>, query: String): List<App> {
        val needle = query.trim()
        val filtered = if (needle.isEmpty()) {
            apps
        } else {
            apps.filter {
                it.appName.contains(needle, ignoreCase = true) ||
                    it.packageName.contains(needle, ignoreCase = true)
            }
        }
        return distinctLaunchers(filtered).sortedBy { it.appName.lowercase() }
    }

    fun isPinned(
        home: List<HomeApp>,
        packageName: String,
        activityName: String,
        userSerial: Long
    ): Boolean {
        return HomeGridLogic.isOnHome(home, packageName, activityName, userSerial)
    }

    fun itemKey(app: App): String {
        return itemKey(app.packageName, app.activityName, app.userSerial)
    }

    fun itemKey(packageName: String, activityName: String, userSerial: Long): String {
        return "$packageName:$activityName:$userSerial"
    }

    fun distinctLaunchers(apps: List<App>): List<App> {
        return apps.distinctBy { itemKey(it) }
    }

    fun imeLaunchTarget(apps: List<App>): App? = apps.singleOrNull()

    fun shouldShowKeyboard(openKeyboardOnDrawer: Boolean, drawerVisible: Boolean): Boolean {
        return openKeyboardOnDrawer && drawerVisible
    }

    const val SEARCH_DEBOUNCE_MS = 80L
    const val CATALOG_STALE_MS = 15_000L
}
