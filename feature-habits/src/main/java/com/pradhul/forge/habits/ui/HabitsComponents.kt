package com.pradhul.forge.habits.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradhul.forge.habits.HabitPalette
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun ProgressRing(done: Int, total: Int, palette: HabitPalette, modifier: Modifier = Modifier) {
    val motion = HabitMotion.enabled()
    val target = if (total <= 0) 0f else done.toFloat() / total
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = HabitMotion.springFloat(motion),
        label = "ring"
    )
    val allDone = total > 0 && done == total
    val pulse = remember { Animatable(1f) }
    val glow = remember { Animatable(0f) }
    var wasComplete by remember { mutableStateOf(allDone) }
    LaunchedEffect(allDone, motion) {
        if (allDone && !wasComplete && motion) {
            coroutineScope {
                launch {
                    pulse.snapTo(1f)
                    pulse.animateTo(1.1f, spring(dampingRatio = 0.5f, stiffness = 900f))
                    pulse.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 800f))
                }
                launch {
                    glow.snapTo(0.5f)
                    glow.animateTo(0f, tween(HabitMotion.SLOW_MS))
                }
            }
        }
        wasComplete = allDone
    }
    val track = palette.muted.toCompose().copy(alpha = 0.25f)
    val sweep = palette.accent.toCompose()
    Box(
        modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(40.dp)) {
            val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            if (glow.value > 0f) {
                drawCircle(
                    color = sweep.copy(alpha = glow.value * 0.45f),
                    radius = size.minDimension / 2f
                )
            }
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = Size(size.minDimension, size.minDimension)
            )
            if (progress > 0f) {
                drawArc(
                    color = sweep,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = stroke,
                    size = Size(size.minDimension, size.minDimension)
                )
            }
        }
        AnimatedContent(
            targetState = done to total,
            transitionSpec = {
                if (!motion) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    (slideInVertically { it / 2 } + fadeIn(tween(HabitMotion.FAST_MS))) togetherWith
                        (slideOutVertically { -it / 2 } + fadeOut(tween(HabitMotion.FAST_MS)))
                }
            },
            label = "ringCount"
        ) { (shownDone, shownTotal) ->
            Text(
                text = "$shownDone/$shownTotal",
                color = palette.text.toCompose(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun HabitSwitch(
    checked: Boolean,
    palette: HabitPalette,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val motion = HabitMotion.enabled()
    val view = LocalView.current
    val fraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = HabitMotion.thumbSpring(motion),
        label = "thumb"
    )
    val track by animateColorAsState(
        targetValue = if (checked) palette.accent.toCompose() else palette.muted.toCompose().copy(alpha = 0.28f),
        animationSpec = HabitMotion.tweenMs(HabitMotion.FAST_MS, motion),
        label = "track"
    )
    val thumb by animateColorAsState(
        targetValue = if (checked) palette.onAccent.toCompose() else Color.White.copy(alpha = 0.85f),
        animationSpec = HabitMotion.tweenMs(HabitMotion.FAST_MS, motion),
        label = "thumbColor"
    )
    Box(
        modifier
            .size(48.dp, 28.dp)
            .habitClickable {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onToggle()
            }
            .clip(RoundedCornerShape(50))
            .background(track)
            .semantics { contentDescription = if (checked) "Done" else "Not done" }
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .offset { IntOffset((20.dp.toPx() * fraction).roundToInt(), 0) }
                .size(22.dp)
                .clip(CircleShape)
                .background(thumb),
            contentAlignment = Alignment.Center
        ) {
            if (fraction > 0.5f) {
                Text("✓", color = palette.accent.toCompose(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun DragHandle(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp, 22.dp).semantics { contentDescription = "Reorder" }) {
        val r = 2.2.dp.toPx()
        val dx = size.width / 3f
        val dy = size.height / 4f
        for (col in 0..1) {
            for (row in 0..2) {
                drawCircle(
                    color = color,
                    radius = r,
                    center = Offset(dx + col * dx, dy + row * dy)
                )
            }
        }
    }
}

@Composable
internal fun BottomTabs(
    statsSelected: Boolean,
    palette: HabitPalette,
    onHabits: () -> Unit,
    onStats: () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(shape)
            .background(palette.card.toCompose())
            .padding(6.dp)
    ) {
        TabChip("Habits", !statsSelected, "☰", palette, Modifier.weight(1f), onHabits)
        TabChip("Stats", statsSelected, "▦", palette, Modifier.weight(1f), onStats)
    }
}

@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    glyph: String,
    palette: HabitPalette,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val motion = HabitMotion.enabled()
    val bg by animateColorAsState(
        targetValue = if (selected) palette.accent.toCompose().copy(alpha = 0.28f) else Color.Transparent,
        animationSpec = HabitMotion.tweenMs(HabitMotion.FAST_MS, motion),
        label = "tabBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) palette.text.toCompose() else palette.muted.toCompose(),
        animationSpec = HabitMotion.tweenMs(HabitMotion.FAST_MS, motion),
        label = "tabFg"
    )
    Row(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .habitClickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        Text(glyph, color = fg, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
    }
}

internal data class RowMenu(
    val habitId: String,
    val x: Float,
    val y: Float
)

@Composable
internal fun FrostedRowMenu(
    menu: RowMenu,
    palette: HabitPalette,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val motion = HabitMotion.enabled()
    val appear = remember { Animatable(if (motion) 0.94f else 1f) }
    val alpha = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (motion) {
            coroutineScope {
                launch { appear.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 900f)) }
                launch { alpha.animateTo(1f, tween(HabitMotion.FAST_MS)) }
            }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
    ) {
        val popupW = with(density) { 168.dp.toPx() }
        Column(
            Modifier
                .offset {
                    IntOffset(
                        (menu.x - popupW + with(density) { 12.dp.toPx() }).roundToInt(),
                        menu.y.roundToInt()
                    )
                }
                .graphicsLayer {
                    scaleX = appear.value
                    scaleY = appear.value
                    this.alpha = alpha.value
                }
                .width(168.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(palette.popup.toCompose())
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
        ) {
            MenuRow("✎  Edit", palette.text.toCompose(), onEdit)
            HorizontalDivider(thickness = 0.6.dp, color = Color.White.copy(alpha = 0.12f))
            MenuRow("⌫  Delete", Color(0xFFFF8A9A), onDelete)
        }
    }
}

@Composable
private fun MenuRow(label: String, color: Color, onClick: () -> Unit) {
    Text(
        text = label,
        color = color,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .habitClickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    )
}

@Composable
internal fun ConfirmDelete(
    palette: HabitPalette,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val motion = HabitMotion.enabled()
    val scale = remember { Animatable(if (motion) 0.92f else 1f) }
    val alpha = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (motion) {
            coroutineScope {
                launch { scale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 800f)) }
                launch { alpha.animateTo(1f, tween(HabitMotion.FAST_MS)) }
            }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .padding(32.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
                .clip(RoundedCornerShape(24.dp))
                .background(palette.popup.toCompose())
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                )
                .padding(22.dp)
        ) {
            Text("Delete this habit?", color = palette.text.toCompose(), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "It will leave the list and stats from today on. Earlier days keep their history.",
                color = palette.muted.toCompose(),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(18.dp))
            Row {
                Text(
                    "Cancel",
                    color = palette.muted.toCompose(),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .habitClickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Delete",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE85A73))
                        .habitClickable(onClick = onConfirm)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
internal fun EmojiBadge(emoji: String, palette: HabitPalette, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.accent.toCompose().copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 18.sp)
    }
}
