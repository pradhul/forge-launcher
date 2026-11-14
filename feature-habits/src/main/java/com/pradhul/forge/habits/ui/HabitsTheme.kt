package com.pradhul.forge.habits.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.pradhul.forge.habits.HabitPalette

val LocalHabitPalette = staticCompositionLocalOf { HabitPalette.FALLBACK }

fun Int.toCompose(): Color = Color(this)

@Composable
fun HabitsTheme(palette: HabitPalette, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHabitPalette provides palette, content = content)
}
