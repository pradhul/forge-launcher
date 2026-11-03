package com.sduduzog.slimlauncher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
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
    onClose: () -> Unit
) {
    val motion = remember {
        tween<Float>(
            durationMillis = DrawerMotionLogic.ANIM_MS,
            easing = FastOutSlowInEasing
        )
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        progress.animateTo(if (visible) 1f else 0f, motion)
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
                translationY = (1f - value) * 32f
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
    onClose: () -> Unit
) {
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
                    closeDrawerState.value()
                }
                if (!change.pressed) {
                    if (!closed && DrawerGestureLogic.shouldCloseOnRelease(
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
    Column(
        modifier = Modifier
            .fillMaxSize()
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
                            onLongClick = { selected = item }
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
                        onLongClick = { selected = item }
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
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val iconImage = remember(item.icon) { item.icon?.asImageBitmap() }
        if (iconImage != null) {
            Image(
                bitmap = iconImage,
                contentDescription = item.app.appName,
                modifier = Modifier.size(AppDrawerGrid.ICON_DP.dp)
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
