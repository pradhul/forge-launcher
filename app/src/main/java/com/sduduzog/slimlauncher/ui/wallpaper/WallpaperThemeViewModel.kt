package com.sduduzog.slimlauncher.ui.wallpaper

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.icons.PackThemeController
import com.sduduzog.slimlauncher.wallpaper.BingWallpaperScheduler
import com.sduduzog.slimlauncher.wallpaper.ThemeSource
import com.sduduzog.slimlauncher.wallpaper.ThemeSourceLogic
import com.sduduzog.slimlauncher.wallpaper.WallpaperThemeMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class WallpaperThemeUiState(
    val bingEnabled: Boolean = false,
    val unmeteredOnly: Boolean = true,
    val setLockScreen: Boolean = false,
    val title: String = "",
    val copyright: String = "",
    val lastError: String = "",
    val themeSource: ThemeSource = ThemeSource.FROM_ICON_PACK,
    val theme: PackTheme = PackTheme.STOCK
)

@HiltViewModel
class WallpaperThemeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: IconPreferences,
    private val themeController: PackThemeController,
    private val monitor: WallpaperThemeMonitor
) : ViewModel() {

    val ui: StateFlow<WallpaperThemeUiState> = prefs.revision.map { snapshot() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), snapshot())

    fun setBingEnabled(enabled: Boolean) {
        prefs.bingWallpaperEnabled = enabled
        BingWallpaperScheduler.sync(context, prefs)
        if (enabled) {
            BingWallpaperScheduler.refreshNow(context, prefs.bingUnmeteredOnly)
        }
    }

    fun setUnmeteredOnly(enabled: Boolean) {
        prefs.bingUnmeteredOnly = enabled
        BingWallpaperScheduler.sync(context, prefs)
        prefs.notifyIconsChanged()
    }

    fun setLockScreen(enabled: Boolean) {
        prefs.bingSetLockScreen = enabled
        prefs.notifyIconsChanged()
    }

    fun setThemeSource(source: ThemeSource) {
        prefs.themeSource = source
    }

    fun refreshNow() {
        prefs.bingLastError = ""
        BingWallpaperScheduler.refreshNow(context, prefs.bingUnmeteredOnly)
        prefs.notifyIconsChanged()
    }

    fun refreshFromSystem() {
        monitor.refreshFromSystem()
    }

    private fun snapshot(): WallpaperThemeUiState {
        return WallpaperThemeUiState(
            bingEnabled = prefs.bingWallpaperEnabled,
            unmeteredOnly = prefs.bingUnmeteredOnly,
            setLockScreen = prefs.bingSetLockScreen,
            title = prefs.bingTitle,
            copyright = prefs.bingCopyright,
            lastError = prefs.bingLastError,
            themeSource = ThemeSourceLogic.selected(
                explicitId = prefs.themeSourceId,
                bingEnabled = prefs.bingWallpaperEnabled,
                wallpaperSet = prefs.wallpaperPresent
            ),
            theme = themeController.active()
        )
    }
}
