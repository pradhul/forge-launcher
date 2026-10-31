package com.sduduzog.slimlauncher.ui.home

import com.sduduzog.slimlauncher.data.model.App

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
        home: List<Pair<String, Long>>,
        packageName: String,
        userSerial: Long
    ): Boolean {
        return home.any { it.first == packageName && it.second == userSerial }
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
}
