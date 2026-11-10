package com.sduduzog.slimlauncher

import android.app.Application
import com.sduduzog.slimlauncher.diag.DebugLogStore
import com.sduduzog.slimlauncher.diag.ForgeStrictMode
import com.sduduzog.slimlauncher.diag.MainThreadStallWatchdog
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.market.MarketKinds
import com.sduduzog.slimlauncher.market.MarketQuoteScheduler
import com.sduduzog.slimlauncher.wallpaper.BingWallpaperScheduler
import com.sduduzog.slimlauncher.widgets.WidgetStore
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
        val marketPlaced = WidgetStore(this).load().any { MarketKinds.isBuiltin(it.kind) }
        MarketQuoteScheduler.sync(this, marketPlaced)
    }
}
