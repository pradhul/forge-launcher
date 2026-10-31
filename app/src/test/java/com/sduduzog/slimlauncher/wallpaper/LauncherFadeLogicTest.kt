package com.sduduzog.slimlauncher.wallpaper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.icons.IconPreferences
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class LauncherFadeLogicTest {

    @Test
    fun homeFadeScalesClockAndDockAlphas() {
        val seed = 0xFF102030.toInt()
        assertThat(WallpaperThemeLogic.homeClockScrim(seed, 100) ushr 24)
            .isEqualTo(WallpaperThemeLogic.HOME_CLOCK_ALPHA)
        assertThat(WallpaperThemeLogic.homeDockScrim(seed, 100) ushr 24)
            .isEqualTo(WallpaperThemeLogic.HOME_DOCK_ALPHA)
        assertThat(WallpaperThemeLogic.homeClockScrim(seed, 0) ushr 24).isEqualTo(0)
        assertThat(WallpaperThemeLogic.homeClockScrim(seed, 50) ushr 24)
            .isEqualTo(LauncherFadeLogic.scaleAlpha(WallpaperThemeLogic.HOME_CLOCK_ALPHA, 50))
        assertThat(LauncherFadeLogic.scaleAlpha(WallpaperThemeLogic.HOME_CLOCK_ALPHA, 50))
            .isGreaterThan(0)
    }

    @Test
    fun homeFadePersists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = context.getSharedPreferences("home_fade_pref_test", Context.MODE_PRIVATE)
        store.edit().clear().commit()
        val prefs = IconPreferences(store)
        assertThat(prefs.homeFadePercent).isEqualTo(LauncherFadeLogic.DEFAULT_HOME_FADE)
        prefs.homeFadePercent = 35
        assertThat(IconPreferences(store).homeFadePercent).isEqualTo(35)
    }
}
