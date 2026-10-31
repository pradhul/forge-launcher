package com.sduduzog.slimlauncher.ui.icons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sduduzog.slimlauncher.icons.GenerationProgress
import com.sduduzog.slimlauncher.icons.PackTheme

@Composable
fun ForgeTheme(
    theme: PackTheme = PackTheme.STOCK,
    translucent: Boolean = false,
    drawerScrim: Boolean = false,
    content: @Composable () -> Unit
) {
    val bgAlpha = when {
        drawerScrim -> 0f
        translucent -> 0.60f
        else -> 1f
    }
    val endAlpha = when {
        drawerScrim -> 0f
        translucent -> 0.40f
        else -> 1f
    }
    val background = Color(theme.background).copy(alpha = bgAlpha)
    val backgroundEnd = Color(theme.backgroundEnd).copy(alpha = endAlpha)
    val foreground = Color(theme.onBackground)
    val accent = Color(theme.accent)
    val onAccent = Color(theme.onAccent)
    val luminance =
        (0.299f * Color(theme.background).red) +
            (0.587f * Color(theme.background).green) +
            (0.114f * Color(theme.background).blue)
    val colors = remember(theme, translucent, drawerScrim) {
        val scheme = if (luminance < 0.5f) {
            darkColorScheme(
                background = background,
                surface = background,
                onBackground = foreground,
                onSurface = foreground,
                primary = accent,
                onPrimary = onAccent,
                outline = foreground.copy(alpha = 0.5f),
                secondary = accent,
                onSecondary = onAccent
            )
        } else {
            lightColorScheme(
                background = background,
                surface = background,
                onBackground = foreground,
                onSurface = foreground,
                primary = accent,
                onPrimary = onAccent,
                outline = foreground.copy(alpha = 0.5f),
                secondary = accent,
                onSecondary = onAccent
            )
        }
        scheme
    }
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography) {
        CompositionLocalProvider(LocalContentColor provides foreground) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!drawerScrim) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(background, backgroundEnd)))
                    )
                }
                content()
            }
        }
    }
}

@Composable
fun IconPacksRoute(viewModel: IconPacksViewModel) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    IconPacksScreen(
        state = state,
        progress = progress,
        onStyledIcons = viewModel::setStyledIcons,
        onStrength = viewModel::setStrength,
        onRefresh = viewModel::refreshFromWallpaper,
        onCancel = viewModel::cancelGeneration
    )
}

@Composable
fun IconPacksScreen(
    state: IconPacksUiState,
    progress: GenerationProgress,
    onStyledIcons: (Boolean) -> Unit,
    onStrength: (Float) -> Unit,
    onRefresh: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Icon packs", style = MaterialTheme.typography.headlineMedium, fontSize = 32.sp)
        Text(
            "Icons are styled from the current wallpaper using on-device Magenta style transfer.",
            fontSize = 14.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Wallpaper-styled icons", fontSize = 16.sp, modifier = Modifier.weight(1f))
            Switch(checked = state.styledIcons, onCheckedChange = onStyledIcons)
        }
        Text("Style strength  ${(state.strength * 100).toInt()}%", fontSize = 16.sp)
        Slider(
            value = state.strength,
            onValueChange = onStrength,
            valueRange = 0.2f..1f
        )
        TextButton(onClick = onRefresh, enabled = state.styledIcons && !progress.running) {
            Text("Restyle from wallpaper")
        }
        if (progress.running) {
            Text("Updating ${progress.currentIndex.coerceAtMost(progress.total)} / ${progress.total}")
            LinearProgressIndicator(
                progress = { (progress.currentIndex + 1).toFloat() / progress.total.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = onCancel) { Text("Cancel") }
        } else if (progress.completed) {
            Text("Styled ${progress.succeeded} icons.")
        }
        progress.lastError?.let { Text(it) }
        Spacer(Modifier.height(12.dp))
    }
}
