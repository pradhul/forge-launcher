package com.sduduzog.slimlauncher

import android.app.Application
import com.sduduzog.slimlauncher.diag.DebugLogStore
import com.sduduzog.slimlauncher.diag.ForgeStrictMode
import com.sduduzog.slimlauncher.diag.MainThreadStallWatchdog
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.wallpaper.BingWallpaperScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var iconPreferences: IconPreferences

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            ForgeStrictMode.install()
        }
        super.onCreate()
        DebugLogStore.dumpStartup(this)
        MainThreadStallWatchdog.start(this)
        if (iconPreferences.bingWallpaperEnabled) {
            BingWallpaperScheduler.sync(this, iconPreferences)
        }
    }
}
