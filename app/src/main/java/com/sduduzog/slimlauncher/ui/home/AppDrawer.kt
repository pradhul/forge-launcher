package com.sduduzog.slimlauncher.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.delay

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
    val fade = tween<Float>(
        durationMillis = DrawerMotionLogic.ANIM_MS,
        easing = FastOutSlowInEasing
    )
    val slide = tween<androidx.compose.ui.unit.IntOffset>(
        durationMillis = DrawerMotionLogic.ANIM_MS,
        easing = FastOutSlowInEasing
    )
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(fade) + slideInVertically(slide) { it / 10 },
        exit = fadeOut(fade) + slideOutVertically(slide) { it / 10 }
    ) {
        Box(Modifier.fillMaxSize()) {
            state.backdrop?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(DrawerBlurLogic.tintArgb(state.theme.seed, state.drawerBlurPercent)))
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
    var drag by remember { mutableFloatStateOf(0f) }
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
    val closeOnSwipeDown = Modifier.pointerInput(gridState) {
        detectVerticalDragGestures(
            onDragEnd = {
                if (DrawerMotionLogic.shouldCloseFromDrawerSwipe(
                        drag,
                        !gridState.canScrollBackward
                    )
                ) {
                    closeDrawer()
                }
                drag = 0f
            },
            onVerticalDrag = { _, amount -> drag += amount }
        )
    }
    val closeDrawerState = rememberUpdatedState(closeDrawer)
    val closeWhenAtTop = remember(gridState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && !gridState.canScrollBackward) {
                    drag += available.y
                    if (DrawerMotionLogic.shouldCloseFromDrawerSwipe(drag, true)) {
                        closeDrawerState.value()
                        drag = 0f
                    }
                    return Offset(0f, available.y)
                }
                if (available.y < 0) drag = 0f
                return Offset.Zero
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(closeWhenAtTop)
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
                .then(closeOnSwipeDown)
        )
        Box(modifier = closeOnSwipeDown) {
            DrawerSearchField(
                visible = visible,
                state = state,
                onQueryChange = onQueryChange,
                onLaunch = launchApp
            )
        }
        Spacer(Modifier.height(12.dp))
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
                        key = { "freq:${AppDrawerLogic.itemKey(it.app)}" }
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
                items(state.apps, key = { AppDrawerLogic.itemKey(it.app) }) { item ->
                    DrawerGridCell(
                        item = item,
                        onClick = { launchApp(item.app) },
                        onLongClick = { selected = item }
                    )
                }
            }
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
        if (AppDrawerLogic.shouldShowKeyboard(state.openKeyboardOnOpen, visible)) {
            delay(KEYBOARD_AFTER_OPEN_MS)
            focusRequester.requestFocus()
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
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

private const val KEYBOARD_AFTER_OPEN_MS = 180L

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
        if (item.icon != null) {
            Image(
                bitmap = item.icon.asImageBitmap(),
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
