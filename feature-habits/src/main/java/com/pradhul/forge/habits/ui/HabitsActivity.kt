package com.pradhul.forge.habits.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import com.pradhul.forge.habits.HabitPalette
import com.pradhul.forge.habits.HabitRepository
import com.pradhul.forge.habits.HabitThemeHolder

class HabitsActivity : ComponentActivity() {
    private var palette by mutableStateOf(HabitThemeHolder.palette)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        palette = HabitPalette.fromJson(intent.getStringExtra(EXTRA_PALETTE)).also {
            HabitThemeHolder.palette = it
        }
        val repo = HabitRepository.get(this)
        setContent {
            HabitsApp(
                palette = palette,
                repo = repo,
                onClose = { finish() }
            )
        }
    }

    companion object {
        const val EXTRA_PALETTE = "palette"
    }
}
