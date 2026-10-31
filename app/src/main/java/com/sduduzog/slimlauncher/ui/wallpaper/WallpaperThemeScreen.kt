package com.sduduzog.slimlauncher.ui.wallpaper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sduduzog.slimlauncher.ui.settings.SettingsAction
import com.sduduzog.slimlauncher.ui.settings.SettingsToggle

@Composable
fun WallpaperThemeScreen(
    state: WallpaperThemeUiState,
    onBingEnabled: (Boolean) -> Unit,
    onUnmeteredOnly: (Boolean) -> Unit,
    onStyledIcons: (Boolean) -> Unit,
    onStrength: (Float) -> Unit,
    onRefreshNow: () -> Unit,
    onApplyTheme: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Wallpaper & theme", style = MaterialTheme.typography.headlineMedium, fontSize = 32.sp)
        SettingsAction(
            title = "Apply theme from wallpaper",
            description = "Read the current wallpaper colours and restyle the launcher now.",
            onClick = onApplyTheme
        )
        SettingsToggle(
            title = "Daily Bing wallpaper",
            description = "Download Bing's photo of the day as your wallpaper.",
            checked = state.bingEnabled,
            onChecked = onBingEnabled
        )
        SettingsToggle(
            title = "Unmetered network only",
            description = "Only download the daily photo on Wi-Fi or other unmetered networks.",
            checked = state.unmeteredOnly,
            onChecked = onUnmeteredOnly
        )
        SettingsAction(
            title = "Refresh now",
            description = "Fetch today's Bing photo now.",
            onClick = onRefreshNow
        )
        val caption = when {
            state.title.isNotBlank() && state.copyright.isNotBlank() ->
                "${state.title}\n${state.copyright}"
            state.copyright.isNotBlank() -> state.copyright
            state.title.isNotBlank() -> state.title
            else -> "No Bing image applied yet."
        }
        Text(caption, fontSize = 14.sp)
        if (state.lastError.isNotBlank()) {
            Text("Last error: ${state.lastError}", fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("Icons follow the current wallpaper automatically.", fontSize = 14.sp)
        SettingsToggle(
            title = "Wallpaper-styled icons",
            description = "Recolour icons so they match the current wallpaper palette.",
            checked = state.styledIcons,
            onChecked = onStyledIcons
        )
        Text("Style strength  ${(state.strength * 100).toInt()}%", fontSize = 16.sp)
        Text(
            "How strongly icons pick up the wallpaper colours.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Slider(
            value = state.strength,
            onValueChange = onStrength,
            valueRange = 0.2f..1f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
