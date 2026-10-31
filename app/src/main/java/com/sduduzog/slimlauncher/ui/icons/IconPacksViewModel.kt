package com.sduduzog.slimlauncher.ui.icons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sduduzog.slimlauncher.icons.GenerationProgress
import com.sduduzog.slimlauncher.icons.IconPackGenerator
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.icons.PackThemeController
import com.sduduzog.slimlauncher.wallpaper.WallpaperPackCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class IconPacksUiState(
    val styledIcons: Boolean = true,
    val strength: Float = 0.7f,
    val theme: PackTheme = PackTheme.STOCK
)

@HiltViewModel
class IconPacksViewModel @Inject constructor(
    private val prefs: IconPreferences,
    private val themeController: PackThemeController,
    private val coordinator: WallpaperPackCoordinator,
    private val generator: IconPackGenerator
) : ViewModel() {

    val ui: StateFlow<IconPacksUiState> = prefs.revision.map { snapshot() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), snapshot())

    val progress: StateFlow<GenerationProgress> = generator.progress.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        generator.progress.value
    )

    fun refresh() {
        prefs.notifyIconsChanged()
    }

    fun setStyledIcons(enabled: Boolean) {
        coordinator.setStyledIcons(enabled)
    }

    fun setStrength(value: Float) {
        coordinator.setStrength(value)
    }

    fun refreshFromWallpaper() {
        prefs.wallpaperStyleHash = null
        coordinator.checkCurrentWallpaper()
    }

    fun cancelGeneration() {
        generator.cancel()
    }

    private fun snapshot(): IconPacksUiState {
        return IconPacksUiState(
            styledIcons = prefs.wallpaperStyledIcons,
            strength = prefs.wallpaperStyleStrength,
            theme = themeController.active()
        )
    }
}
