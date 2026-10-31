package com.sduduzog.slimlauncher.ui.wallpaper

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WallpaperThemeScreen(
    state: WallpaperThemeUiState,
    onBingEnabled: (Boolean) -> Unit,
    onUnmeteredOnly: (Boolean) -> Unit,
    onStyledIcons: (Boolean) -> Unit,
    onStrength: (Float) -> Unit,
    onRefreshNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Wallpaper & theme", style = MaterialTheme.typography.headlineMedium, fontSize = 32.sp)
        ToggleRow("Daily Bing wallpaper", state.bingEnabled, onBingEnabled)
        ToggleRow("Unmetered network only", state.unmeteredOnly, onUnmeteredOnly)
        TextButton(onClick = onRefreshNow) { Text("Refresh now") }
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
        ToggleRow("Wallpaper-styled icons", state.styledIcons, onStyledIcons)
        Text("Style strength  ${(state.strength * 100).toInt()}%", fontSize = 16.sp)
        Slider(
            value = state.strength,
            onValueChange = onStrength,
            valueRange = 0.2f..1f
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
