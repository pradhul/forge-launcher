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
    fun defaultFadeKeepsV9DrawerAlphas() {
        val gradient = WallpaperThemeLogic.drawerGradient(0xFF102030.toInt())
        assertThat(gradient[0] ushr 24).isEqualTo(WallpaperThemeLogic.DRAWER_TOP_ALPHA)
        assertThat(gradient[1] ushr 24).isEqualTo(WallpaperThemeLogic.DRAWER_BOTTOM_ALPHA)
    }

    @Test
    fun zeroFadeIsFullyTransparent() {
        val gradient = WallpaperThemeLogic.drawerGradient(0xFF102030.toInt(), fadePercent = 0)
        assertThat(gradient[0] ushr 24).isEqualTo(0)
        assertThat(gradient[1] ushr 24).isEqualTo(0)
        assertThat(WallpaperThemeLogic.homeClockScrim(0xFF102030.toInt(), 0) ushr 24).isEqualTo(0)
        assertThat(WallpaperThemeLogic.homeDockScrim(0xFF102030.toInt(), 0) ushr 24).isEqualTo(0)
    }

    @Test
    fun halfFadeScalesV9Alphas() {
        assertThat(LauncherFadeLogic.scaleAlpha(WallpaperThemeLogic.DRAWER_TOP_ALPHA, 50))
            .isEqualTo(0x46)
        assertThat(LauncherFadeLogic.scaleAlpha(WallpaperThemeLogic.DRAWER_BOTTOM_ALPHA, 50))
            .isEqualTo(0x66)
        val gradient = WallpaperThemeLogic.drawerGradient(0xFF102030.toInt(), fadePercent = 50)
        assertThat(gradient[0] ushr 24).isEqualTo(0x46)
        assertThat(gradient[1] ushr 24).isEqualTo(0x66)
    }

    @Test
    fun blurDefaultIsCurrentTwelveDpAndZeroDisables() {
        assertThat(LauncherFadeLogic.blurRadiusDp(LauncherFadeLogic.DEFAULT_DRAWER_BLUR))
            .isEqualTo(12f)
        assertThat(LauncherFadeLogic.blurRadiusDp(0)).isEqualTo(0f)
        assertThat(LauncherFadeLogic.blurRadiusDp(100)).isEqualTo(24f)
        assertThat(LauncherFadeLogic.blurSupported(31)).isTrue()
        assertThat(LauncherFadeLogic.blurSupported(30)).isFalse()
    }

    @Test
    fun fadeAndBlurPercentsPersist() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = context.getSharedPreferences("fade_pref_test", Context.MODE_PRIVATE)
        store.edit().clear().commit()
        val prefs = IconPreferences(store)
        assertThat(prefs.drawerFadePercent).isEqualTo(LauncherFadeLogic.DEFAULT_DRAWER_FADE)
        assertThat(prefs.homeFadePercent).isEqualTo(LauncherFadeLogic.DEFAULT_HOME_FADE)
        assertThat(prefs.drawerBlurPercent).isEqualTo(LauncherFadeLogic.DEFAULT_DRAWER_BLUR)

        prefs.drawerFadePercent = 25
        prefs.homeFadePercent = 40
        prefs.drawerBlurPercent = 80
        val reloaded = IconPreferences(store)
        assertThat(reloaded.drawerFadePercent).isEqualTo(25)
        assertThat(reloaded.homeFadePercent).isEqualTo(40)
        assertThat(reloaded.drawerBlurPercent).isEqualTo(80)
        assertThat(reloaded.chrome.value.drawerFadePercent).isEqualTo(25)
    }

    @Test
    fun percentIsClamped() {
        assertThat(LauncherFadeLogic.clampPercent(-8)).isEqualTo(0)
        assertThat(LauncherFadeLogic.clampPercent(140)).isEqualTo(100)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = context.getSharedPreferences("fade_clamp_test", Context.MODE_PRIVATE)
        store.edit().clear().commit()
        val prefs = IconPreferences(store)
        prefs.drawerFadePercent = 250
        assertThat(prefs.drawerFadePercent).isEqualTo(100)
        prefs.drawerFadePercent = -3
        assertThat(prefs.drawerFadePercent).isEqualTo(0)
    }
}
