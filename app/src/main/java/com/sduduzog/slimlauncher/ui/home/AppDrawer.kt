package com.sduduzog.slimlauncher.ui.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sduduzog.slimlauncher.data.model.App

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
    onIconPacks: () -> Unit,
    onClose: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it }
    ) {
        AppDrawer(
            state = state,
            onQueryChange = onQueryChange,
            onLaunch = onLaunch,
            onPin = onPin,
            onUnpin = onUnpin,
            onAppInfo = onAppInfo,
            onRegenerate = onRegenerate,
            onIconPacks = onIconPacks,
            onClose = onClose
        )
    }
}

@Composable
fun AppDrawer(
    state: AppDrawerUiState,
    onQueryChange: (String) -> Unit,
    onLaunch: (App) -> Unit,
    onPin: (App) -> Unit,
    onUnpin: (App) -> Unit,
    onAppInfo: (App) -> Unit,
    onRegenerate: (App) -> Unit,
    onIconPacks: () -> Unit,
    onClose: () -> Unit
) {
    var selected by remember { mutableStateOf<DrawerAppItem?>(null) }
    var drag by remember { mutableFloatStateOf(0f) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (drag > 80f) onClose()
                            drag = 0f
                        },
                        onVerticalDrag = { _, amount -> drag += amount }
                    )
                }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "All apps",
                style = MaterialTheme.typography.headlineMedium,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                TextButton(onClick = onIconPacks) { Text("Icon packs") }
                TextButton(onClick = onClose) { Text("Close") }
            }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search apps") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.onBackground
            )
        )
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
                            onClick = { onLaunch(item.app) },
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
                        onClick = { onLaunch(item.app) },
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
