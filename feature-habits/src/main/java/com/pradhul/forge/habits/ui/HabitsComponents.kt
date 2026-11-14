package com.pradhul.forge.habits.ui

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pradhul.forge.habits.HabitPalette
import kotlin.math.roundToInt

@Composable
internal fun ProgressRing(done: Int, total: Int, palette: HabitPalette, modifier: Modifier = Modifier) {
    val progress = if (total <= 0) 0f else done.toFloat() / total
    val track = palette.muted.toCompose().copy(alpha = 0.25f)
    val sweep = palette.accent.toCompose()
    Box(modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(40.dp)) {
            val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
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
        Text(
            text = "$done/$total",
            color = palette.text.toCompose(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun HabitSwitch(
    checked: Boolean,
    palette: HabitPalette,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = if (checked) palette.accent.toCompose() else palette.muted.toCompose().copy(alpha = 0.28f)
    val thumb = if (checked) palette.onAccent.toCompose() else Color.White.copy(alpha = 0.85f)
    Box(
        modifier
            .size(48.dp, 28.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .semantics { contentDescription = if (checked) "Done" else "Not done" }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(thumb),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
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
    val bg = if (selected) palette.accent.toCompose().copy(alpha = 0.28f) else Color.Transparent
    val fg = if (selected) palette.text.toCompose() else palette.muted.toCompose()
    Row(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(bg)
            .clickable(onClick = onClick)
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
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    )
}

@Composable
internal fun ConfirmDelete(
    palette: HabitPalette,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
                        .clickable(onClick = onDismiss)
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
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
internal fun EmojiBadge(emoji: String, palette: HabitPalette) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.accent.toCompose().copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 18.sp)
    }
}
