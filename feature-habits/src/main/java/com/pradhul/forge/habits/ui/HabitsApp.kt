package com.pradhul.forge.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradhul.forge.habits.DotState
import com.pradhul.forge.habits.Habit
import com.pradhul.forge.habits.HabitLogic
import com.pradhul.forge.habits.HabitPalette
import com.pradhul.forge.habits.HabitRepository
import com.pradhul.forge.habits.HabitSnapshot
import com.pradhul.forge.habits.HabitWeekRow
import com.pradhul.forge.habits.MonthDay
import com.pradhul.forge.habits.WeekStats
import java.util.TimeZone

private enum class Page { Habits, Stats }

data class EditorState(
    val id: String?,
    val name: String,
    val emoji: String
)

@Composable
fun HabitsApp(
    palette: HabitPalette,
    repo: HabitRepository,
    onClose: () -> Unit
) {
    val snapshot by repo.state.collectAsState()
    var page by remember { mutableStateOf(Page.Habits) }
    var weekMode by remember { mutableStateOf(true) }
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var menu by remember { mutableStateOf<RowMenu?>(null) }
    var confirm by remember { mutableStateOf<String?>(null) }
    val today = repo.today()

    HabitsTheme(palette) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(palette.background.toCompose(), palette.backgroundEnd.toCompose())
                    )
                )
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (page == Page.Habits) {
                        ManageScreen(
                            snapshot = snapshot,
                            palette = palette,
                            onClose = onClose,
                            onNew = { editor = EditorState(null, "", HabitLogic.DEFAULT_EMOJI) },
                            onMenu = { menu = it },
                            onMove = { id, delta -> repo.move(id, delta) }
                        )
                    } else {
                        StatsScreen(
                            snapshot = snapshot,
                            today = today,
                            zone = repo.zone,
                            palette = palette,
                            weekMode = weekMode,
                            onClose = onClose,
                            onWeekMode = { weekMode = it }
                        )
                    }
                }
                BottomTabs(
                    statsSelected = page == Page.Stats,
                    palette = palette,
                    onHabits = { page = Page.Habits },
                    onStats = { page = Page.Stats }
                )
            }
            menu?.let { current ->
                FrostedRowMenu(
                    menu = current,
                    palette = palette,
                    onEdit = {
                        val habit = snapshot.habits.firstOrNull { it.id == current.habitId }
                        menu = null
                        if (habit != null) editor = EditorState(habit.id, habit.name, habit.emoji)
                    },
                    onDelete = {
                        confirm = current.habitId
                        menu = null
                    },
                    onDismiss = { menu = null }
                )
            }
            editor?.let { state ->
                EditSheet(
                    state = state,
                    palette = palette,
                    onName = { editor = state.copy(name = it.take(HabitLogic.NAME_MAX)) },
                    onEmoji = { editor = state.copy(emoji = it) },
                    onSave = {
                        val ok = if (state.id == null) repo.add(state.name, state.emoji)
                        else repo.edit(state.id, state.name, state.emoji)
                        if (ok != null) editor = null
                    },
                    onDelete = { confirm = state.id },
                    onDismiss = { editor = null }
                )
            }
            confirm?.let { id ->
                ConfirmDelete(
                    palette = palette,
                    onConfirm = {
                        repo.delete(id)
                        confirm = null
                        editor = null
                    },
                    onDismiss = { confirm = null }
                )
            }
        }
    }
}

@Composable
internal fun ManageScreen(
    snapshot: HabitSnapshot,
    palette: HabitPalette,
    onClose: () -> Unit,
    onNew: () -> Unit,
    onMenu: (RowMenu) -> Unit,
    onMove: (String, Int) -> Unit
) {
    val habits = snapshot.activeHabits()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar("Habits", palette, onClose)
            Text(
                if (habits.isEmpty()) "No habits yet · every habit repeats daily"
                else "${habits.size} habit${if (habits.size == 1) "" else "s"} · every habit repeats daily",
                color = palette.muted.toCompose(),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (habits.isEmpty()) {
                    Text(
                        "Tap + New habit to start tracking your day.",
                        color = palette.muted.toCompose(),
                        fontSize = 15.sp,
                        modifier = Modifier
                            .padding(24.dp)
                            .semantics { contentDescription = "empty" }
                    )
                } else {
                    habits.forEach { habit ->
                        ManageRow(habit, palette, onMenu) { onMove(habit.id, it) }
                    }
                    Text(
                        "Tap ⋮ to edit or delete · hold ⋮⋮ to drag and reorder",
                        color = palette.muted.toCompose(),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 80.dp)
                    )
                }
            }
        }
        Text(
            "+  New habit",
            color = palette.onAccent.toCompose(),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.accent.toCompose())
                .clickable(onClick = onNew)
                .semantics { contentDescription = "New habit" }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun ManageRow(
    habit: Habit,
    palette: HabitPalette,
    onMenu: (RowMenu) -> Unit,
    onMove: (Int) -> Unit
) {
    val density = LocalDensity.current
    val rowH = with(density) { 56.dp.toPx() }
    var acc by remember { mutableFloatStateOf(0f) }
    val menuAnchor = remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.card.toCompose())
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .pointerInput(habit.id) {
                    detectDragGestures(
                        onDragStart = { acc = 0f },
                        onDragEnd = { acc = 0f },
                        onDragCancel = { acc = 0f },
                        onDrag = { _, amount ->
                            acc += amount.y
                            if (acc > rowH) {
                                onMove(1)
                                acc = 0f
                            } else if (acc < -rowH) {
                                onMove(-1)
                                acc = 0f
                            }
                        }
                    )
                }
                .padding(4.dp)
        ) {
            DragHandle(palette.muted.toCompose())
        }
        Spacer(Modifier.width(8.dp))
        EmojiBadge(habit.emoji, palette)
        Spacer(Modifier.width(12.dp))
        Text(
            habit.name,
            color = palette.text.toCompose(),
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "⋮",
            color = palette.muted.toCompose(),
            fontSize = 22.sp,
            modifier = Modifier
                .onGloballyPositioned { menuAnchor.value = it.positionInRoot() }
                .clickable {
                    onMenu(RowMenu(habit.id, menuAnchor.value.x, menuAnchor.value.y + 20f))
                }
                .semantics { contentDescription = "Menu ${habit.name}" }
                .padding(8.dp)
        )
    }
}

@Composable
internal fun EditSheet(
    state: EditorState,
    palette: HabitPalette,
    onName: (String) -> Unit,
    onEmoji: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val canSave = HabitLogic.canSave(state.name)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                )
                .semantics { contentDescription = "Dismiss editor" },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(palette.popup.toCompose())
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.muted.toCompose().copy(alpha = 0.4f))
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (state.id == null) "New habit" else "Edit habit",
                color = palette.text.toCompose(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Text("Name", color = palette.muted.toCompose(), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            TextField(
                value = state.name,
                onValueChange = onName,
                singleLine = true,
                leadingIcon = { Text(state.emoji, fontSize = 20.sp) },
                trailingIcon = {
                    Text(
                        "${state.name.length}/${HabitLogic.NAME_MAX}",
                        color = palette.muted.toCompose(),
                        fontSize = 12.sp
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = palette.text.toCompose(),
                    unfocusedTextColor = palette.text.toCompose(),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = palette.accent.toCompose(),
                    focusedIndicatorColor = palette.accent.toCompose(),
                    unfocusedIndicatorColor = palette.accent.toCompose().copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Habit name" }
            )
            Spacer(Modifier.height(16.dp))
            Text("Emoji", color = palette.muted.toCompose(), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            val emojis = HabitLogic.EMOJIS
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                emojis.chunked(6).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { emoji ->
                            val selected = emoji == state.emoji
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(palette.accent.toCompose().copy(alpha = if (selected) 0.28f else 0.12f))
                                    .then(
                                        if (selected) Modifier.border(
                                            1.5.dp,
                                            palette.accent.toCompose(),
                                            RoundedCornerShape(14.dp)
                                        ) else Modifier
                                    )
                                    .clickable { onEmoji(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.id != null) {
                    Text(
                        "Delete",
                        color = Color(0xFFFF8A9A),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0xFFFF8A9A).copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 28.dp, vertical = 12.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Save",
                    color = if (canSave) palette.onAccent.toCompose() else palette.muted.toCompose(),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (canSave) palette.accent.toCompose()
                            else palette.muted.toCompose().copy(alpha = 0.2f)
                        )
                        .clickable(enabled = canSave, onClick = onSave)
                        .semantics { contentDescription = "Save habit" }
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
internal fun StatsScreen(
    snapshot: HabitSnapshot,
    today: String,
    zone: TimeZone,
    palette: HabitPalette,
    weekMode: Boolean,
    onClose: () -> Unit,
    onWeekMode: (Boolean) -> Unit
) {
    val week = HabitLogic.weekStats(snapshot, today, zone)
    val month = HabitLogic.monthStats(snapshot, today, zone)
    Column(Modifier.fillMaxSize()) {
        TopBar("Statistics", palette, onClose)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WeekMonthToggle(weekMode, palette, onWeekMode)
            if (weekMode) WeekCard(week, palette) else MonthCard(month, palette)
            if (weekMode) WeekGrid(week, palette)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WeekMonthToggle(weekMode: Boolean, palette: HabitPalette, onWeekMode: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.card.toCompose())
            .padding(4.dp)
    ) {
        Segment("✓  Week", weekMode, palette, Modifier.weight(1f)) { onWeekMode(true) }
        Segment("Month", !weekMode, palette, Modifier.weight(1f)) { onWeekMode(false) }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, palette: HabitPalette, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) palette.accent.toCompose().copy(alpha = 0.35f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) palette.text.toCompose() else palette.muted.toCompose(),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun WeekCard(week: WeekStats, palette: HabitPalette) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.card.toCompose())
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    week.percent?.let { "$it%" } ?: "—",
                    color = palette.text.toCompose(),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "This week · ${week.rangeLabel}",
                    color = palette.muted.toCompose(),
                    fontSize = 13.sp
                )
            }
            week.points?.let { pts ->
                val up = pts >= 0
                val bg = if (up) palette.success.toCompose() else palette.danger.toCompose()
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${if (up) "▲" else "▼"} ${kotlin.math.abs(pts)} pts",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    week.lastPercent?.let {
                        Text("vs $it% last week", color = palette.muted.toCompose(), fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row {
            Text("🔥  ${week.currentStreak} days", color = palette.text.toCompose(), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(20.dp))
            Text("🏆  ${week.bestStreak} days", color = palette.text.toCompose(), fontWeight = FontWeight.SemiBold)
        }
        Row {
            Text("Current streak", color = palette.muted.toCompose(), fontSize = 12.sp, modifier = Modifier.padding(start = 28.dp))
            Spacer(Modifier.width(28.dp))
            Text("Best streak", color = palette.muted.toCompose(), fontSize = 12.sp)
        }
    }
}

@Composable
private fun WeekGrid(week: WeekStats, palette: HabitPalette) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.card.toCompose())
            .padding(16.dp)
    ) {
        Row(Modifier.padding(start = 132.dp, end = 36.dp, bottom = 6.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    it,
                    color = palette.muted.toCompose(),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
            }
            Text("%", color = palette.muted.toCompose(), fontSize = 11.sp, modifier = Modifier.width(36.dp))
        }
        week.rows.forEach { row ->
            WeekGridRow(row, palette)
        }
        if (week.rows.isEmpty()) {
            Text("No habits this week", color = palette.muted.toCompose(), fontSize = 14.sp)
        }
    }
}

@Composable
private fun WeekGridRow(row: HabitWeekRow, palette: HabitPalette) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(row.emoji, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            row.name,
            color = palette.text.toCompose(),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(100.dp)
        )
        row.dots.forEach { dot ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val fill = when (dot.state) {
                    DotState.DONE, DotState.TODAY_DONE -> palette.accent.toCompose()
                    DotState.MISSED, DotState.TODAY_MISSED -> Color.Transparent
                    DotState.EMPTY, DotState.FUTURE -> palette.muted.toCompose().copy(alpha = 0.18f)
                }
                val border = when (dot.state) {
                    DotState.TODAY_DONE, DotState.TODAY_MISSED -> palette.text.toCompose()
                    DotState.MISSED -> palette.accent.toCompose().copy(alpha = 0.55f)
                    else -> Color.Transparent
                }
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(fill)
                        .then(if (border.alpha > 0f) Modifier.border(1.2.dp, border, CircleShape) else Modifier)
                )
            }
        }
        Text(
            "${row.percent}%",
            color = palette.muted.toCompose(),
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun MonthCard(month: com.pradhul.forge.habits.MonthStats, palette: HabitPalette) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.card.toCompose())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(month.title, color = palette.text.toCompose(), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Less", color = palette.muted.toCompose(), fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            (0..4).forEach { step ->
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(heatColor(step / 4.0, palette, empty = step == 0))
                )
            }
            Spacer(Modifier.width(4.dp))
            Text("More", color = palette.muted.toCompose(), fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(
                    it,
                    color = palette.muted.toCompose(),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val cells = List(month.leadingBlanks) { null } + month.days
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day != null) HeatCell(day, palette)
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HeatCell(day: MonthDay, palette: HabitPalette) {
    val bg = when {
        day.future -> Color.Transparent
        day.ratio == null -> palette.muted.toCompose().copy(alpha = 0.12f)
        else -> heatColor(day.ratio, palette, empty = false)
    }
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (day.today) Modifier.border(1.5.dp, palette.text.toCompose(), RoundedCornerShape(10.dp))
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${day.dayOfMonth}",
            color = if (day.future) palette.muted.toCompose().copy(alpha = 0.35f) else palette.text.toCompose(),
            fontSize = 13.sp
        )
    }
}

private fun heatColor(ratio: Double, palette: HabitPalette, empty: Boolean): Color {
    if (empty) return palette.muted.toCompose().copy(alpha = 0.12f)
    val t = ratio.coerceIn(0.0, 1.0).toFloat()
    return palette.accent.toCompose().copy(alpha = 0.18f + 0.72f * t)
}

@Composable
internal fun TopBar(title: String, palette: HabitPalette, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "‹",
            color = palette.text.toCompose(),
            fontSize = 28.sp,
            modifier = Modifier
                .clickable(onClick = onClose)
                .semantics { contentDescription = "Back" }
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Text(title, color = palette.text.toCompose(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
