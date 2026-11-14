package com.pradhul.forge.habits.ui

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.text.font.FontWeight
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
                    .habitClickable(onClick = onOpen)
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
    val motion = HabitMotion.enabled()
    val dim by animateFloatAsState(
        targetValue = if (row.done) 0.55f else 1f,
        animationSpec = HabitMotion.tweenMs(HabitMotion.MEDIUM_MS, motion),
        label = "dim"
    )
    val strike by animateFloatAsState(
        targetValue = if (row.done) 1f else 0f,
        animationSpec = HabitMotion.tweenMs(HabitMotion.MEDIUM_MS, motion),
        label = "strike"
    )
    val emojiScale = remember { Animatable(1f) }
    var wasDone by remember { mutableStateOf(row.done) }
    LaunchedEffect(row.done, motion) {
        if (row.done && !wasDone && motion) {
            emojiScale.snapTo(1f)
            emojiScale.animateTo(1.14f, spring(dampingRatio = 0.45f, stiffness = 900f))
            emojiScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 800f))
        } else if (!row.done) {
            emojiScale.snapTo(1f)
        }
        wasDone = row.done
    }
    val strikeColor = palette.text.toCompose().copy(alpha = 0.55f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .graphicsLayer { alpha = dim },
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiBadge(
            row.emoji,
            palette,
            Modifier.graphicsLayer {
                scaleX = emojiScale.value
                scaleY = emojiScale.value
            }
        )
        Spacer(Modifier.width(12.dp))
        Text(
            row.name,
            color = palette.text.toCompose(),
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .drawWithContent {
                    drawContent()
                    if (strike > 0f) {
                        val y = size.height * 0.52f
                        drawLine(
                            color = strikeColor,
                            start = Offset(0f, y),
                            end = Offset(size.width * strike, y),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
        )
        HabitSwitch(row.done, palette, onToggle)
    }
}
