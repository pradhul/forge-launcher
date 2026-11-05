package com.sduduzog.slimlauncher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.blur
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.border
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.wallpaper.DrawerBlurLogic
import com.sduduzog.slimlauncher.wallpaper.DrawerMotionLogic

@Composable
fun AppDrawerOverlay(
    visible: Boolean,
    state: AppDrawerUiState,
    onQueryChange: (String) -> Unit,
    onLaunch: (App) -> Unit,
    onPin: (App) -> Unit,
    onUnpin: (App) -> Unit,
    onAppInfo: (App) -> Unit,
    onRegenerate: (App) -> Unit,
    onUninstall: (App) -> Unit = {},
    onShortcuts: (App) -> List<AppShortcut> = { emptyList() },
    onLaunchShortcut: (AppShortcut) -> Unit = {},
    onClose: () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        progress.animateTo(
            if (visible) 1f else 0f,
            tween(
                durationMillis = DrawerMotionLogic.ANIM_MS,
                easing = if (visible) LinearOutSlowInEasing else FastOutLinearInEasing
            )
        )
    }
    val bitmap = state.backdrop
    val backdropImage = remember(bitmap) { bitmap?.asImageBitmap() }
    val mode = remember(state.crossWindowBlur, bitmap != null) {
        DrawerBlurLogic.backdropMode(
            sdkInt = android.os.Build.VERSION.SDK_INT,
            crossWindowEnabled = state.crossWindowBlur,
            hasBitmap = bitmap != null
        )
    }
    val scrim = remember(bitmap, state.theme.seed, state.drawerBlurPercent) {
        Color(
            if (bitmap == null && state.drawerBlurPercent > 0) {
                DrawerBlurLogic.fallbackScrimArgb(state.theme.seed, state.drawerBlurPercent)
            } else {
                DrawerBlurLogic.tintArgb(state.theme.seed, state.drawerBlurPercent)
            }
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val value = progress.value
                this.alpha = value
                translationY = (1f - value) * 8f
            }
    ) {
        if (DrawerBlurLogic.shouldDrawBitmapBackdrop(mode, bitmap != null, state.drawerBlurPercent)) {
            backdropImage?.let { image ->
                Image(
                    bitmap = image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim)
        )
        AppDrawer(
            visible = visible,
            state = state,
            onQueryChange = onQueryChange,
            onLaunch = onLaunch,
            onPin = onPin,
            onUnpin = onUnpin,
            onAppInfo = onAppInfo,
            onRegenerate = onRegenerate,
            onUninstall = onUninstall,
            onShortcuts = onShortcuts,
            onLaunchShortcut = onLaunchShortcut,
            onClose = onClose
        )
    }
}

@Composable
fun AppDrawer(
    visible: Boolean = true,
    state: AppDrawerUiState,
    onQueryChange: (String) -> Unit,
    onLaunch: (App) -> Unit,
    onPin: (App) -> Unit,
    onUnpin: (App) -> Unit,
    onAppInfo: (App) -> Unit,
    onRegenerate: (App) -> Unit,
    onUninstall: (App) -> Unit = {},
    onShortcuts: (App) -> List<AppShortcut> = { emptyList() },
    onLaunchShortcut: (AppShortcut) -> Unit = {},
    onClose: () -> Unit
) {
    var ring by remember { mutableStateOf<RingState?>(null) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    val ringSlop = with(LocalDensity.current) { 16.dp.toPx() }
    var selected by remember { mutableStateOf<DrawerAppItem?>(null) }
    val gridState = rememberLazyGridState()
    val keyboard = LocalSoftwareKeyboardController.current
    val launchApp: (App) -> Unit = { app ->
        keyboard?.hide()
        onLaunch(app)
    }
    val closeDrawer: () -> Unit = {
        keyboard?.hide()
        onClose()
    }
    val closeDrawerState = rememberUpdatedState(closeDrawer)
    // Observe (never consume) every gesture on the drawer: a single swipe down
    // that starts with the list at the top closes immediately.
    val swipeToClose = Modifier.pointerInput(gridState) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val startedAtTop = !gridState.canScrollBackward
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            var closed = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                tracker.addPosition(change.uptimeMillis, change.position)
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                if (!closed && DrawerGestureLogic.shouldCloseOnMove(dy, dx, startedAtTop, density)) {
                    closed = true
                    if (ring == null) closeDrawerState.value()
                }
                if (!change.pressed) {
                    if (!closed && ring == null && DrawerGestureLogic.shouldCloseOnRelease(
                            dy, dx, tracker.calculateVelocity().y, startedAtTop, density
                        )
                    ) {
                        closeDrawerState.value()
                    }
                    break
                }
            }
        }
    }
    // Swallow downward overscroll at the top so the stretch effect never
    // eats the close gesture.
    val noTopOverscroll = remember(gridState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (available.y > 0 && !gridState.canScrollBackward) Offset(0f, available.y) else Offset.Zero
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOrigin = it.positionInRoot() }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (ring != null) Modifier.blur(18.dp) else Modifier)
            .then(swipeToClose)
            .nestedScroll(noTopOverscroll)
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
                .size(width = 48.dp, height = 4.dp)
                .background(
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                    MaterialTheme.shapes.small
                )
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        if (state.apps.isEmpty()) {
            Text(
                if (state.query.isBlank()) "No launchable apps found." else "No apps match that search.",
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(AppDrawerGrid.PHONE_COLUMNS),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (state.frequent.isNotEmpty()) {
                    item(
                        key = "frequent-header",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Text(
                            "Frequent",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(
                        state.frequent,
                        key = { "freq:${AppDrawerLogic.itemKey(it.app)}" },
                        contentType = { "app" }
                    ) { item ->
                        DrawerGridCell(
                            item = item,
                            onClick = { launchApp(item.app) },
                            onRingStart = { center ->
                                keyboard?.hide()
                                ring = RingState(
                                    item, center - rootOrigin, null, RingHit.Center,
                                    shortcuts = onShortcuts(item.app)
                                )
                            },
                            onRingDrag = { pos -> ring = ring?.withPointer(pos - rootOrigin, ringSlop) },
                            onRingEnd = { ring = ring?.copy(released = true) }
                        )
                    }
                    item(
                        key = "frequent-spacer",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Spacer(Modifier.height(4.dp))
                    }
                }
                items(
                    state.apps,
                    key = { AppDrawerLogic.itemKey(it.app) },
                    contentType = { "app" }
                ) { item ->
                    DrawerGridCell(
                        item = item,
                        onClick = { launchApp(item.app) },
                        onRingStart = { center ->
                            keyboard?.hide()
                            ring = RingState(
                                item, center - rootOrigin, null, RingHit.Center,
                                shortcuts = onShortcuts(item.app)
                            )
                        },
                        onRingDrag = { pos -> ring = ring?.withPointer(pos - rootOrigin, ringSlop) },
                        onRingEnd = { ring = ring?.copy(released = true) }
                    )
                }
            }
        }
        }
        Spacer(Modifier.height(12.dp))
        // Search sits at the bottom, right above the keyboard.
        DrawerSearchField(
            visible = visible,
            state = state,
            onQueryChange = onQueryChange,
            onLaunch = launchApp
        )
    }
    ring?.let { current ->
        RingMenuOverlay(
            state = current,
            onUpdate = { ring = it },
            onAction = { option ->
                val app = current.item.app
                ring = null
                when (option) {
                    is RingOption.Builtin -> when (option.action) {
                        RingAction.APP_INFO -> onAppInfo(app)
                        RingAction.HOME -> if (current.item.pinned) onUnpin(app) else onPin(app)
                        RingAction.UNINSTALL -> onUninstall(app)
                    }
                    is RingOption.Shortcut -> onLaunchShortcut(option.shortcut)
                }
            },
            onDismiss = { ring = null }
        )
    }
    }
    selected?.let { item ->
        AppActionDialog(
            item = item,
            canRegenerate = state.canRegenerate,
            onDismiss = { selected = null },
            onPin = {
                onPin(item.app)
                selected = null
            },
            onUnpin = {
                onUnpin(item.app)
                selected = null
            },
            onAppInfo = {
                onAppInfo(item.app)
                selected = null
            },
            onRegenerate = {
                onRegenerate(item.app)
                selected = null
            }
        )
    }
}

@Composable
private fun DrawerSearchField(
    visible: Boolean,
    state: AppDrawerUiState,
    onQueryChange: (String) -> Unit,
    onLaunch: (App) -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val onBackground = state.theme.onBackground
    val container = Color(DrawerSearchStyle.containerColor(state.theme.background, state.theme.accent))
    val text = Color(DrawerSearchStyle.textColor(onBackground))
    val placeholder = Color(DrawerSearchStyle.placeholderColor(onBackground))
    val icon = Color(DrawerSearchStyle.iconColor(onBackground))
    val cursor = Color(DrawerSearchStyle.cursorColor(state.theme.accent))
    LaunchedEffect(visible, state.openKeyboardOnOpen) {
        if (DrawerWindowLogic.shouldHideIme(visible)) {
            keyboard?.hide()
            return@LaunchedEffect
        }
        if (!AppDrawerLogic.shouldShowKeyboard(state.openKeyboardOnOpen, visible)) {
            return@LaunchedEffect
        }
        delay(DrawerMotionLogic.ANIM_MS.toLong())
        if (!DrawerWindowLogic.shouldRequestIme(
                settingOn = state.openKeyboardOnOpen,
                drawerVisible = visible,
                animationSettled = true
            )
        ) {
            return@LaunchedEffect
        }
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }
    val submitSingleMatch = {
        AppDrawerLogic.imeLaunchTarget(state.apps.map { it.app })?.let(onLaunch)
    }
    TextField(
        value = state.query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        shape = CircleShape,
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                tint = icon
            )
        },
        placeholder = { Text("Search apps", color = placeholder) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Search
        ),
        keyboardActions = KeyboardActions(
            onSearch = { submitSingleMatch() },
            onGo = { submitSingleMatch() },
            onDone = { submitSingleMatch() }
        ),
        colors = TextFieldDefaults.colors(
            focusedTextColor = text,
            unfocusedTextColor = text,
            cursorColor = cursor,
            focusedContainerColor = container,
            unfocusedContainerColor = container,
            disabledContainerColor = container,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedLeadingIconColor = icon,
            unfocusedLeadingIconColor = icon,
            focusedPlaceholderColor = placeholder,
            unfocusedPlaceholderColor = placeholder
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerGridCell(
    item: DrawerAppItem,
    onClick: () -> Unit,
    onRingStart: (Offset) -> Unit,
    onRingDrag: (Offset) -> Unit,
    onRingEnd: () -> Unit
) {
    var iconCenter by remember { mutableStateOf(Offset.Zero) }
    var cellOrigin by remember { mutableStateOf(Offset.Zero) }
    var cellCenter by remember { mutableStateOf(Offset.Zero) }
    val haptics = LocalHapticFeedback.current
    val start by rememberUpdatedState(onRingStart)
    val dragTo by rememberUpdatedState(onRingDrag)
    val end by rememberUpdatedState(onRingEnd)
    val onClickState by rememberUpdatedState(onClick)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                cellOrigin = it.positionInRoot()
                cellCenter = it.boundsInRoot().center
            }
            .pointerInput(item.app) {
                // One handler for tap + long-press + drag-select, so nothing else
                // (clickable / grid scroll) can steal the long-press.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val slop = viewConfiguration.touchSlop
                    var outcome = CellPressLogic.Outcome.PENDING
                    val timedOut = withTimeoutOrNull(CellPressLogic.LONG_PRESS_MS) {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val c = ev.changes.firstOrNull { it.id == down.id }
                            if (c == null) { outcome = CellPressLogic.Outcome.SCROLL; break }
                            val moved = (c.position - down.position).getDistance()
                            outcome = CellPressLogic.classify(
                                c.uptimeMillis - down.uptimeMillis, moved, !c.pressed, slop
                            )
                            if (c.isConsumed && c.pressed) outcome = CellPressLogic.Outcome.SCROLL
                            if (outcome != CellPressLogic.Outcome.PENDING) {
                                if (outcome == CellPressLogic.Outcome.TAP) c.consume()
                                break
                            }
                        }
                        true
                    } == null
                    if (timedOut) outcome = CellPressLogic.Outcome.LONG_PRESS
                    when (outcome) {
                        CellPressLogic.Outcome.TAP -> onClickState()
                        CellPressLogic.Outcome.LONG_PRESS -> {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            start(if (iconCenter != Offset.Zero) iconCenter else cellCenter)
                            dragTo(cellOrigin + down.position)
                            while (true) {
                                val ev = awaitPointerEvent()
                                val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                                c.consume()
                                dragTo(cellOrigin + c.position)
                                if (!c.pressed) break
                            }
                            end() // release after long-press never launches
                        }
                        else -> Unit
                    }
                }
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val iconImage = remember(item.icon) { item.icon?.asImageBitmap() }
        if (iconImage != null) {
            Image(
                bitmap = iconImage,
                contentDescription = item.app.appName,
                modifier = Modifier
                    .size(AppDrawerGrid.ICON_DP.dp)
                    .onGloballyPositioned {
                        val b = it.boundsInRoot()
                        iconCenter = b.center
                    }
            )
        } else {
            Box(
                modifier = Modifier
                    .size(AppDrawerGrid.ICON_DP.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.app.appName,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth()
        )
        if (item.pinned) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        MaterialTheme.shapes.small
                    )
            )
        } else {
            Spacer(Modifier.height(7.dp))
        }
    }
}

@Composable
private fun AppActionDialog(
    item: DrawerAppItem,
    canRegenerate: Boolean,
    onDismiss: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
    onAppInfo: () -> Unit,
    onRegenerate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.app.appName) },
        text = {
            Column {
                if (item.pinned) {
                    TextButton(onClick = onUnpin) { Text("Remove from home") }
                } else {
                    TextButton(onClick = onPin) { Text("Add to home") }
                }
                TextButton(onClick = onAppInfo) { Text("App info") }
                if (canRegenerate) {
                    TextButton(onClick = onRegenerate) { Text("Regenerate icon") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

data class RingState(
    val item: DrawerAppItem,
    val center: Offset,
    val pointer: Offset?,
    val hit: RingHit,
    val released: Boolean = false,
    val moved: Boolean = false,
    val press: Offset? = null,
    val shortcuts: List<AppShortcut> = emptyList()
)

/** Update the drag pointer; remembers the press point and whether the finger really moved. */
fun RingState.withPointer(p: Offset, slopPx: Float, released: Boolean = this.released): RingState {
    val pressAt = press ?: p
    val m = RingMenuLogic.movedFrom(RingPoint(pressAt.x, pressAt.y), p.x, p.y, slopPx, moved)
    return copy(pointer = p, press = pressAt, moved = m, released = released)
}

/**
 * Radial long-press menu. Drag from the icon to an option and release to run
 * it; release on the icon keeps the ring open for a tap; Cancel or tapping
 * outside dismisses.
 */
@Composable
fun RingMenuOverlay(
    state: RingState,
    onUpdate: (RingState) -> Unit,
    onAction: (RingOption) -> Unit,
    onDismiss: () -> Unit
) {
    // Lay out only once the real container size is known (never 0x0).
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        if (RingMenuLogic.canLayout(w.toFloat(), h.toFloat())) {
            RingMenuContent(state, w.toFloat(), h.toFloat(), onUpdate, onAction, onDismiss)
        }
    }
}

@Composable
private fun RingMenuContent(
    state: RingState,
    widthPx: Float,
    heightPx: Float,
    onUpdate: (RingState) -> Unit,
    onAction: (RingOption) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val radius = with(density) { 132.dp.toPx() }
    val bubble = 64.dp
    val bubblePx = with(density) { bubble.toPx() }
    val hitPx = with(density) { 52.dp.toPx() }
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val container = MaterialTheme.colorScheme.primaryContainer
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val items = remember(state.shortcuts) { AppShortcutLogic.ringOptions(state.shortcuts) }
    val layout = remember(state.center, widthPx, heightPx, items) {
        RingMenuLogic.layout(
            state.center.x, state.center.y, radius,
            widthPx, heightPx,
            with(density) { 8.dp.toPx() }, with(density) { 150.dp.toPx() },
            bubble = bubblePx, label = with(density) { 28.dp.toPx() },
            cancelW = with(density) { 132.dp.toPx() }, cancelH = with(density) { 48.dp.toPx() },
            items = items
        )
    }
    val hit = state.pointer?.let {
        RingMenuLogic.hitTest(layout, state.center.x, state.center.y, it.x, it.y, hitPx)
    } ?: RingHit.Center
    // Drag-release resolution.
    LaunchedEffect(state.released) {
        if (!state.released) return@LaunchedEffect
        when (RingMenuLogic.onRelease(hit, state.moved)) {
            RingMenuLogic.Release.ACTION -> onAction((hit as RingHit.Option).option)
            RingMenuLogic.Release.DISMISS -> onDismiss()
            RingMenuLogic.Release.KEEP_OPEN ->
                onUpdate(state.copy(released = false, pointer = null, press = null, moved = false))
        }
    }
    val dismissTap by rememberUpdatedState(onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(layout) {
                detectTapGestures { p ->
                    when (val h = RingMenuLogic.hitTest(layout, state.center.x, state.center.y, p.x, p.y, hitPx)) {
                        is RingHit.Option -> onAction(h.option)
                        RingHit.Center -> Unit
                        else -> dismissTap()
                    }
                }
            }
    ) {
        state.pointer?.let { p ->
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = state.center,
                    end = p,
                    strokeWidth = 6.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
        val iconImage = remember(state.item.icon) { state.item.icon?.asImageBitmap() }
        val iconPx = with(density) { (AppDrawerGrid.ICON_DP * 1.2f).dp.toPx() }
        iconImage?.let {
            Image(
                bitmap = it,
                contentDescription = state.item.app.appName,
                modifier = Modifier
                    .offset { IntOffset((state.center.x - iconPx / 2).roundToInt(), (state.center.y - iconPx / 2).roundToInt()) }
                    .size((AppDrawerGrid.ICON_DP * 1.2f).dp)
            )
        }
        Text(
            RingMenuLogic.HINT,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.offset {
                IntOffset(
                    (layout.hint.x - with(density) { 64.dp.toPx() }).roundToInt(),
                    (layout.hint.y - with(density) { 10.dp.toPx() }).roundToInt()
                )
            }.size(width = 128.dp, height = 20.dp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        for ((option, p) in layout.options) {
            val hot = hit is RingHit.Option && hit.option == option
            val label = AppShortcutLogic.label(option, state.item.pinned)
            val glyph = AppShortcutLogic.glyph(option, state.item.pinned)
            val scale = if (hot) 1.18f else 1f
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset {
                    IntOffset((p.x - bubblePx).roundToInt(), (p.y - bubblePx / 2).roundToInt())
                }.size(width = bubble * 2, height = bubble + 36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(bubble)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .background(if (hot) accent else container, CircleShape)
                        .border(2.dp, accent, CircleShape)
                ) {
                    Text(glyph, fontSize = if (option is RingOption.Shortcut) 20.sp else 24.sp, color = if (hot) onAccent else onContainer)
                }
                Text(
                    label,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        val cancelHot = hit == RingHit.Cancel
        val cw = with(density) { 132.dp.toPx() }
        val ch = with(density) { 48.dp.toPx() }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset((layout.cancel.x - cw / 2).roundToInt(), (layout.cancel.y - ch / 2).roundToInt()) }
                .size(132.dp, 48.dp)
                .background(
                    if (cancelHot) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.18f),
                    CircleShape
                )
                .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape)
        ) {
            Text("×  Cancel", color = if (cancelHot) Color.Black else Color.White, fontSize = 15.sp)
        }
    }
}
