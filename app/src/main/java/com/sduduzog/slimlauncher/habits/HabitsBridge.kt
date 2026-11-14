package com.sduduzog.slimlauncher.habits

import com.pradhul.forge.habits.HabitPalette
import com.sduduzog.slimlauncher.icons.PackTheme

object HabitsBridge {
    fun palette(theme: PackTheme): HabitPalette = HabitPalette(
        background = theme.background,
        backgroundEnd = theme.backgroundEnd,
        onBackground = theme.onBackground,
        accent = theme.accent,
        onAccent = theme.onAccent,
        darkSurface = theme.darkSurface
    )
}
