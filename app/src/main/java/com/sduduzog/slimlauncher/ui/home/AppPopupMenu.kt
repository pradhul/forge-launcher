package com.sduduzog.slimlauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.data.model.App
import kotlin.math.roundToInt

data class AppPopupState(
    val app: App,
    val onHome: Boolean,
    val canUninstall: Boolean,
    val shortcuts: List<AppShortcut>,
    val anchor: Offset
)

private val CardFill = Color(0xE6211B2E)
private val Ink = Color(0xFFF4F1FA)
private val Line = Color(0x22FFFFFF)

@Composable
fun AppPopupOverlay(
    state: AppPopupState,
    onAction: (AppPopupAction) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val items = remember(state.shortcuts, state.onHome, state.canUninstall) {
        AppPopupLogic.items(state.shortcuts, state.onHome, state.canUninstall)
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            )
    ) {
        val popupW = with(density) { AppPopupLogic.WIDTH_DP.dp.toPx() }
        val popupH = with(density) { (AppPopupLogic.ROW_DP * items.size).dp.toPx() }
        val margin = with(density) { 16.dp.toPx() }
        val gap = with(density) { AppPopupLogic.GAP_DP.dp.toPx() }
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()
        val (x, y) = AppPopupLogic.place(
            state.anchor.x, state.anchor.y, popupW, popupH, screenW, screenH, margin, gap
        )
        AppPopupCard(
            items = items,
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}
                ),
            onAction = onAction
        )
    }
}

@Composable
private fun AppPopupCard(
    items: List<AppPopupItem>,
    modifier: Modifier,
    onAction: (AppPopupAction) -> Unit
) {
    val shape = RoundedCornerShape(AppPopupLogic.RADIUS_DP.dp)
    Column(
        modifier = modifier
            .width(AppPopupLogic.WIDTH_DP.dp)
            .clip(shape)
            .background(CardFill, shape)
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(thickness = 0.6.dp, color = Line)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppPopupLogic.ROW_DP.dp)
                    .clickable { onAction(item.action) }
                    .padding(horizontal = 18.dp)
            ) {
                Icon(
                    painter = painterResource(iconRes(item.icon)),
                    contentDescription = item.label,
                    tint = Ink,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = item.label,
                    color = Ink,
                    fontSize = 16.sp
                )
            }
        }
    }
}

private fun iconRes(icon: AppPopupIcon): Int = when (icon) {
    AppPopupIcon.PRIVATE -> R.drawable.ic_popup_private
    AppPopupIcon.NEW_TAB -> R.drawable.ic_popup_new
    AppPopupIcon.SHORTCUT -> R.drawable.ic_popup_shortcut
    AppPopupIcon.INFO -> R.drawable.ic_popup_info
    AppPopupIcon.ADD_HOME -> R.drawable.ic_popup_add
    AppPopupIcon.REMOVE_HOME -> R.drawable.ic_popup_remove
    AppPopupIcon.UNINSTALL -> R.drawable.ic_popup_uninstall
}
