package com.pradhul.forge.habits

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import com.pradhul.forge.habits.ui.HabitsActivity
import com.pradhul.forge.habits.ui.HabitsWidgetView

/** Android entry points for :app. The module does not import launcher classes. */
object HabitsEntry {
    fun repository(context: Context): HabitRepository = HabitRepository.get(context)

    fun widget(context: Context, palette: HabitPalette, onOpen: () -> Unit): View {
        return HabitsWidgetView(context, palette, onOpen)
    }

    fun open(context: Context, palette: HabitPalette) {
        HabitThemeHolder.palette = palette
        val intent = Intent(context, HabitsActivity::class.java)
            .putExtra(HabitsActivity.EXTRA_PALETTE, palette.toJson())
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

object HabitThemeHolder {
    @Volatile
    var palette: HabitPalette = HabitPalette.FALLBACK
}
