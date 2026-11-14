package com.pradhul.forge.habits.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradhul.forge.habits.DayDot
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val motion = HabitMotion.enabled()

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
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                AnimatedContent(
                    targetState = page,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = {
                        if (!motion) {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        } else if (targetState == Page.Stats) {
                            (slideInHorizontally(tween(HabitMotion.MEDIUM_MS)) { it / 6 } +
                                fadeIn(tween(HabitMotion.MEDIUM_MS))) togetherWith
                                (slideOutHorizontally(tween(HabitMotion.FAST_MS)) { -it / 6 } +
                                    fadeOut(tween(HabitMotion.FAST_MS)))
                        } else {
                            (slideInHorizontally(tween(HabitMotion.MEDIUM_MS)) { -it / 6 } +
                                fadeIn(tween(HabitMotion.MEDIUM_MS))) togetherWith
                                (slideOutHorizontally(tween(HabitMotion.FAST_MS)) { it / 6 } +
                                    fadeOut(tween(HabitMotion.FAST_MS)))
                        }.using(SizeTransform(clip = false))
                    },
                    label = "habitsPage"
                ) { current ->
                    if (current == Page.Habits) {
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
    val motion = HabitMotion.enabled()
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
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (habits.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "Tap + New habit to start tracking your day.",
                            color = palette.muted.toCompose(),
                            fontSize = 15.sp,
                            modifier = Modifier
                                .padding(24.dp)
                                .semantics { contentDescription = "empty" }
                        )
                    }
                } else {
                    items(habits, key = { it.id }) { habit ->
                        ManageRow(
                            habit = habit,
                            palette = palette,
                            modifier = Modifier.animateItem(
                                fadeInSpec = HabitMotion.fadeSpec(motion),
                                fadeOutSpec = HabitMotion.fadeSpec(motion),
                                placementSpec = HabitMotion.placementSpec(motion)
                            ),
                            onMenu = onMenu,
                            onMove = { onMove(habit.id, it) }
                        )
                    }
                    item(key = "hint") {
                        Text(
                            "Tap ⋮ to edit or delete · hold ⋮⋮ to drag and reorder",
                            color = palette.muted.toCompose(),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
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
                .habitClickable(onClick = onNew)
                .semantics { contentDescription = "New habit" }
                .padding(horizontal = 18.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun ManageRow(
    habit: Habit,
    palette: HabitPalette,
    modifier: Modifier = Modifier,
    onMenu: (RowMenu) -> Unit,
    onMove: (Int) -> Unit
) {
    val motion = HabitMotion.enabled()
    val view = LocalView.current
    var dragging by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = HabitMotion.springFloat(motion),
        label = "lift"
    )
    var acc by remember { mutableFloatStateOf(0f) }
    val rowHPx = with(androidx.compose.ui.platform.LocalDensity.current) { 56.dp.toPx() }
    val menuAnchor = remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    Row(
        modifier
            .graphicsLayer {
                scaleX = 1f + 0.03f * lift
                scaleY = 1f + 0.03f * lift
            }
            .shadow((10 * lift).dp, RoundedCornerShape(22.dp), clip = false)
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
                        onDragStart = {
                            acc = 0f
                            dragging = true
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        },
                        onDragEnd = {
                            acc = 0f
                            dragging = false
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        },
                        onDragCancel = {
                            acc = 0f
                            dragging = false
                        },
                        onDrag = { _, amount ->
                            acc += amount.y
                            if (acc > rowHPx) {
                                onMove(1)
                                acc = 0f
                            } else if (acc < -rowHPx) {
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
                .habitClickable {
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
    val motion = HabitMotion.enabled()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val isNew = state.id == null
    val sheetOffset = remember { Animatable(if (motion) 160f else 0f) }
    val scrim = remember { Animatable(if (motion) 0f else 0.45f) }
    LaunchedEffect(Unit) {
        if (motion) {
            coroutineScope {
                launch { sheetOffset.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 700f)) }
                launch { scrim.animateTo(0.45f, tween(HabitMotion.MEDIUM_MS)) }
            }
        }
    }
    LaunchedEffect(state.id) {
        if (isNew) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    val saveBg by animateColorAsState(
        targetValue = if (canSave) palette.accent.toCompose() else palette.muted.toCompose().copy(alpha = 0.2f),
        animationSpec = HabitMotion.tweenMs(HabitMotion.FAST_MS, motion),
        label = "saveBg"
    )
    val saveFg by animateColorAsState(
        targetValue = if (canSave) palette.onAccent.toCompose() else palette.muted.toCompose(),
        animationSpec = HabitMotion.tweenMs(HabitMotion.FAST_MS, motion),
        label = "saveFg"
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrim.value))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss
                )
                .semantics { contentDescription = "Dismiss editor" }
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
                .heightIn(max = maxHeight)
                .graphicsLayer { translationY = sheetOffset.value }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(palette.popup.toCompose())
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
                .padding(horizontal = 20.dp, vertical = 12.dp)
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
                if (isNew) "New habit" else "Edit habit",
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
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (canSave) onSave() else keyboard?.hide()
                    }
                ),
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
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = "Habit name" }
            )
            Spacer(Modifier.height(16.dp))
            Text("Emoji", color = palette.muted.toCompose(), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitLogic.EMOJIS.chunked(6).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { emoji ->
                            EmojiChoice(
                                emoji = emoji,
                                selected = emoji == state.emoji,
                                palette = palette,
                                modifier = Modifier.weight(1f),
                                onClick = { onEmoji(emoji) }
                            )
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
                            .habitClickable(onClick = onDelete)
                            .padding(horizontal = 28.dp, vertical = 12.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Save",
                    color = saveFg,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(saveBg)
                        .habitClickable(enabled = canSave, onClick = onSave)
                        .semantics { contentDescription = "Save habit" }
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EmojiChoice(
    emoji: String,
    selected: Boolean,
    palette: HabitPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val motion = HabitMotion.enabled()
    val selectedFrac by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = HabitMotion.popFloat(motion),
        label = "emojiSel"
    )
    Box(
        modifier
            .height(48.dp)
            .graphicsLayer {
                val pop = 1f + 0.08f * selectedFrac
                scaleX = pop
                scaleY = pop
            }
            .clip(RoundedCornerShape(14.dp))
            .background(palette.accent.toCompose().copy(alpha = 0.12f + 0.16f * selectedFrac))
            .then(
                if (selectedFrac > 0.01f) Modifier.border(
                    (1.5f * selectedFrac).dp,
                    palette.accent.toCompose().copy(alpha = selectedFrac),
                    RoundedCornerShape(14.dp)
                ) else Modifier
            )
            .habitClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 22.sp)
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
    val motion = HabitMotion.enabled()
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
            AnimatedContent(
                targetState = weekMode,
                transitionSpec = {
                    if (!motion) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else if (targetState) {
                        (slideInHorizontally(tween(HabitMotion.MEDIUM_MS)) { -it / 8 } +
                            fadeIn(tween(HabitMotion.MEDIUM_MS))) togetherWith
                            (slideOutHorizontally(tween(HabitMotion.FAST_MS)) { it / 8 } +
                                fadeOut(tween(HabitMotion.FAST_MS)))
                    } else {
                        (slideInHorizontally(tween(HabitMotion.MEDIUM_MS)) { it / 8 } +
                            fadeIn(tween(HabitMotion.MEDIUM_MS))) togetherWith
                            (slideOutHorizontally(tween(HabitMotion.FAST_MS)) { -it / 8 } +
                                fadeOut(tween(HabitMotion.FAST_MS)))
                    }
                },
                label = "weekMonth"
            ) { weekSelected ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (weekSelected) {
                        WeekCard(week, palette)
                        WeekGrid(week, palette)
                    } else {
                        MonthCard(month, palette)
                    }
                }
            }
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
    val motion = HabitMotion.enabled()
    val bg by animateColorAsState(
        targetValue = if (selected) palette.accent.toCompose().copy(alpha = 0.35f) else Color.Transparent,
        animationSpec = HabitMotion.tweenMs(HabitMotion.FAST_MS, motion),
        label = "segBg"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .habitClickable(onClick = onClick)
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
    val motion = HabitMotion.enabled()
    val percentTarget = week.percent ?: 0
    val shownPercent by animateIntAsState(
        targetValue = percentTarget,
        animationSpec = if (motion) tween(HabitMotion.MEDIUM_MS) else snap(),
        label = "weekPct"
    )
    val shownCurrent by animateIntAsState(
        targetValue = week.currentStreak,
        animationSpec = if (motion) tween(HabitMotion.MEDIUM_MS) else snap(),
        label = "currentStreak"
    )
    val shownBest by animateIntAsState(
        targetValue = week.bestStreak,
        animationSpec = if (motion) tween(HabitMotion.MEDIUM_MS) else snap(),
        label = "bestStreak"
    )
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
                    week.percent?.let { "$shownPercent%" } ?: "—",
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
            Text("🔥  $shownCurrent days", color = palette.text.toCompose(), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(20.dp))
            Text("🏆  $shownBest days", color = palette.text.toCompose(), fontWeight = FontWeight.SemiBold)
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
        week.rows.forEachIndexed { rowIndex, row ->
            WeekGridRow(row, palette, rowIndex)
        }
        if (week.rows.isEmpty()) {
            Text("No habits this week", color = palette.muted.toCompose(), fontSize = 14.sp)
        }
    }
}

@Composable
private fun WeekGridRow(row: HabitWeekRow, palette: HabitPalette, rowIndex: Int) {
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
        row.dots.forEachIndexed { col, dot ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                StaggerDot(dot, palette, index = rowIndex * 7 + col)
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
private fun StaggerDot(dot: DayDot, palette: HabitPalette, index: Int) {
    val motion = HabitMotion.enabled()
    val appear = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(index, motion) {
        if (motion) {
            appear.snapTo(0f)
            delay(HabitMotion.staggerDelayMs(index, true).toLong())
            appear.animateTo(1f, tween(HabitMotion.FAST_MS))
        } else {
            appear.snapTo(1f)
        }
    }
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
            .graphicsLayer {
                scaleX = 0.65f + 0.35f * appear.value
                scaleY = 0.65f + 0.35f * appear.value
                alpha = appear.value
            }
            .clip(CircleShape)
            .background(fill)
            .then(if (border.alpha > 0f) Modifier.border(1.2.dp, border, CircleShape) else Modifier)
    )
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
        cells.chunked(7).forEachIndexed { weekIndex, week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                week.forEachIndexed { col, day ->
                    Box(Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day != null) HeatCell(day, palette, index = weekIndex * 7 + col)
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HeatCell(day: MonthDay, palette: HabitPalette, index: Int) {
    val motion = HabitMotion.enabled()
    val appear = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(index, motion) {
        if (motion) {
            appear.snapTo(0f)
            delay(HabitMotion.staggerDelayMs(index, true).toLong())
            appear.animateTo(1f, tween(HabitMotion.FAST_MS))
        } else {
            appear.snapTo(1f)
        }
    }
    val bg = when {
        day.future -> Color.Transparent
        day.ratio == null -> palette.muted.toCompose().copy(alpha = 0.12f)
        else -> heatColor(day.ratio, palette, empty = false)
    }
    Box(
        Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = 0.82f + 0.18f * appear.value
                scaleY = 0.82f + 0.18f * appear.value
                alpha = appear.value
            }
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
                .habitClickable(onClick = onClose)
                .semantics { contentDescription = "Back" }
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
        Text(title, color = palette.text.toCompose(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}
