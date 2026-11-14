package com.pradhul.forge.habits.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradhul.forge.habits.HabitLogic
import com.pradhul.forge.habits.HabitPalette
import com.pradhul.forge.habits.HabitRepository
import com.pradhul.forge.habits.TodayRow
import com.pradhul.forge.habits.TodayView

class HabitsWidgetView(
    context: Context,
    palette: HabitPalette,
    private val onOpen: () -> Unit
) : AbstractComposeView(context) {
    private val paletteState = mutableStateOf(palette)

    fun setPalette(next: HabitPalette) {
        paletteState.value = next
    }

    @Composable
    override fun Content() {
        val repo = HabitRepository.get(context)
        val snapshot by repo.state.collectAsState()
        val palette = paletteState.value
        val today = repo.today()
        val view = HabitLogic.todayView(snapshot, today, repo.zone)
        HabitsTheme(palette) {
            HabitsWidgetContent(
                view = view,
                palette = palette,
                onOpen = onOpen,
                onToggle = { repo.toggle(it) }
            )
        }
    }
}

@Composable
fun HabitsWidgetContent(
    view: TodayView,
    palette: HabitPalette,
    onOpen: () -> Unit,
    onToggle: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(view.done, view.total, palette)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Habits · Today",
                    color = palette.text.toCompose(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (view.total == 0) view.dateLabel
                    else "${view.dateLabel} · ${view.done} of ${view.total} done",
                    color = palette.muted.toCompose(),
                    fontSize = 12.sp
                )
            }
            Text(
                "Open ›",
                color = palette.text.toCompose(),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        if (view.rows.isEmpty()) {
            Text(
                "No habits yet",
                color = palette.muted.toCompose(),
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                view.rows.forEach { row ->
                    HabitTodayRow(row, palette) { onToggle(row.id) }
                }
            }
        }
    }
}

@Composable
private fun HabitTodayRow(row: TodayRow, palette: HabitPalette, onToggle: () -> Unit) {
    val dim = if (row.done) 0.55f else 1f
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .alpha(dim),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiBadge(row.emoji, palette)
        Spacer(Modifier.width(12.dp))
        Text(
            row.name,
            color = palette.text.toCompose(),
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (row.done) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )
        HabitSwitch(row.done, palette, onToggle)
    }
}
