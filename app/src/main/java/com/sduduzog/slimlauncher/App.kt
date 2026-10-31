package com.sduduzog.slimlauncher

import android.app.Application
import android.os.StrictMode
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
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
        }
        super.onCreate()
        if (iconPreferences.bingWallpaperEnabled) {
            BingWallpaperScheduler.sync(this, iconPreferences)
        }
    }
}
