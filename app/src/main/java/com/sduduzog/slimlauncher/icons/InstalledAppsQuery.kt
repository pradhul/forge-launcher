package com.sduduzog.slimlauncher.icons

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager
import com.sduduzog.slimlauncher.BuildConfig
import com.sduduzog.slimlauncher.data.model.App
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppsQuery @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun load(): List<App> {
        val list = mutableListOf<App>()
        val manager = context.getSystemService(Context.USER_SERVICE) as UserManager
        val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val myUserHandle = Process.myUserHandle()

        for (profile in manager.userProfiles) {
            val prefix = if (profile == myUserHandle) "" else "\uD83C\uDD46 "
            val profileSerial = manager.getSerialNumberForUser(profile)
            for (activityInfo in launcher.getActivityList(null, profile)) {
                list.add(
                    App(
                        appName = prefix + activityInfo.label.toString(),
                        packageName = activityInfo.applicationInfo.packageName,
                        activityName = activityInfo.name,
                        userSerial = profileSerial
                    )
                )
            }
        }

        return list
            .filterNot { it.packageName == BuildConfig.APPLICATION_ID }
            .sortedBy { it.appName.lowercase() }
    }
}
