package com.sduduzog.slimlauncher.ui.icons

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.TypedValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sduduzog.slimlauncher.R
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.icons.GenerationMode
import com.sduduzog.slimlauncher.icons.GenerationProgress
import com.sduduzog.slimlauncher.icons.IconPack
import com.sduduzog.slimlauncher.icons.StylePreset

@Composable
fun ForgeTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = remember {
        val value = TypedValue()
        context.theme.resolveAttribute(android.R.attr.colorBackground, value, true)
        val background = Color(value.data)
        context.theme.resolveAttribute(android.R.attr.colorForeground, value, true)
        val foreground = Color(value.data)
        val luminance =
            (0.299f * background.red) + (0.587f * background.green) + (0.114f * background.blue)
        val scheme = if (luminance < 0.5f) {
            darkColorScheme(
                background = background,
                surface = background,
                onBackground = foreground,
                onSurface = foreground,
                primary = foreground,
                onPrimary = background,
                outline = foreground.copy(alpha = 0.5f)
            )
        } else {
            lightColorScheme(
                background = background,
                surface = background,
                onBackground = foreground,
                onSurface = foreground,
                primary = foreground,
                onPrimary = background,
                outline = foreground.copy(alpha = 0.5f)
            )
        }
        scheme
    }
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}

@Composable
fun IconPacksRoute(viewModel: IconPacksViewModel) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(viewModel::onCustomStylePicked)
    }
    IconPacksScreen(
        state = state,
        progress = progress,
        onKeyDraftChange = viewModel::onKeyDraftChange,
        onSaveKey = viewModel::saveApiKey,
        onClearKey = viewModel::clearApiKey,
        onShowCreate = viewModel::showCreate,
        onCreateNameChange = viewModel::onCreateNameChange,
        onCreatePromptChange = viewModel::onCreatePromptChange,
        onCreateModeChange = viewModel::onCreateModeChange,
        onPresetSelected = viewModel::onPresetSelected,
        onStrengthChange = viewModel::onStrengthChange,
        onPickCustomStyle = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onCreatePack = viewModel::createPack,
        onApply = viewModel::applyPack,
        onUseStock = viewModel::useStockIcons,
        onOpen = viewModel::openPack,
        onClosePack = viewModel::closePack,
        onRequestDelete = viewModel::requestDelete,
        onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onRegenerate = viewModel::regenerate,
        onCancel = viewModel::cancelGeneration,
        onRefresh = viewModel::refresh
    )
}

@Composable
fun IconPacksScreen(
    state: IconPacksUiState,
    progress: GenerationProgress,
    onKeyDraftChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onClearKey: () -> Unit,
    onShowCreate: (Boolean) -> Unit,
    onCreateNameChange: (String) -> Unit,
    onCreatePromptChange: (String) -> Unit,
    onCreateModeChange: (GenerationMode) -> Unit,
    onPresetSelected: (String) -> Unit,
    onStrengthChange: (Float) -> Unit,
    onPickCustomStyle: () -> Unit,
    onCreatePack: () -> Unit,
    onApply: (String) -> Unit,
    onUseStock: () -> Unit,
    onOpen: (String) -> Unit,
    onClosePack: () -> Unit,
    onRequestDelete: (String) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onRegenerate: (App) -> Unit,
    onCancel: () -> Unit,
    onRefresh: () -> Unit
) {
    LaunchedEffect(progress.completed, progress.cancelled, progress.succeeded, progress.failed) {
        if (progress.completed || progress.cancelled) {
            onRefresh()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = if (state.selectedPack != null) state.selectedPack!!.name else "Icon packs",
            style = MaterialTheme.typography.headlineMedium,
            fontSize = 32.sp
        )
        Spacer(Modifier.height(16.dp))
        GenerationBanner(progress, onCancel)
        when {
            state.showCreate -> CreatePackForm(
                state = state,
                generating = progress.running,
                onNameChange = onCreateNameChange,
                onPromptChange = onCreatePromptChange,
                onModeChange = onCreateModeChange,
                onPresetSelected = onPresetSelected,
                onStrengthChange = onStrengthChange,
                onPickCustomStyle = onPickCustomStyle,
                onCreate = onCreatePack,
                onCancel = { onShowCreate(false) }
            )
            state.selectedPack != null -> PackDetail(
                pack = state.selectedPack!!,
                apps = state.installedApps,
                applied = state.appliedPackId == state.selectedPack!!.id,
                hasApiKey = state.hasApiKey,
                generating = progress.running,
                onApply = { onApply(state.selectedPack!!.id) },
                onDelete = { onRequestDelete(state.selectedPack!!.id) },
                onBack = onClosePack,
                onRegenerate = onRegenerate
            )
            else -> PackList(
                state = state,
                generating = progress.running,
                onKeyDraftChange = onKeyDraftChange,
                onSaveKey = onSaveKey,
                onClearKey = onClearKey,
                onShowCreate = { onShowCreate(true) },
                onApply = onApply,
                onUseStock = onUseStock,
                onOpen = onOpen,
                onDelete = onRequestDelete
            )
        }
    }
    if (state.confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete pack?") },
            text = { Text("This removes generated icons from the device. Stock icons are unchanged.") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = onDismissDelete) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GenerationBanner(progress: GenerationProgress, onCancel: () -> Unit) {
    if (!progress.running && progress.lastError == null && !progress.cancelled) {
        return
    }
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        if (progress.running) {
            val total = progress.total.coerceAtLeast(1)
            Text("Generating ${progress.packName}".trim())
            Text("${progress.currentIndex.coerceAtMost(total)} / ${progress.total}  ${progress.currentAppName}")
            LinearProgressIndicator(
                progress = { (progress.currentIndex + 1).toFloat() / total },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            Text("Succeeded ${progress.succeeded}   Failed ${progress.failed}")
            TextButton(onClick = onCancel) { Text("Cancel") }
        } else if (progress.cancelled) {
            Text("Generation cancelled. ${progress.succeeded} icons kept.")
        }
        progress.lastError?.let { Text(it) }
    }
}

@Composable
private fun PackList(
    state: IconPacksUiState,
    generating: Boolean,
    onKeyDraftChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onClearKey: () -> Unit,
    onShowCreate: () -> Unit,
    onApply: (String) -> Unit,
    onUseStock: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Google AI Studio API key", fontSize = 18.sp)
        Text(stringResource(R.string.icon_packs_api_key_hint), fontSize = 14.sp)
        TextButton(onClick = { openApiKeyPage(context) }) {
            Text(stringResource(R.string.icon_packs_get_api_key))
        }
        OutlinedTextField(
            value = state.keyDraft,
            onValueChange = onKeyDraftChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            placeholder = {
                Text(if (state.hasApiKey) "Key saved on device" else "Paste API key")
            },
            colors = fieldColors()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onSaveKey, enabled = state.keyDraft.isNotBlank()) {
                Text("Save key")
            }
            if (state.hasApiKey) {
                TextButton(onClick = onClearKey) { Text("Clear key") }
            }
        }
        if (state.keySaved) {
            Text("Key saved. It is stored only on this device.")
        }
        TextButton(onClick = onShowCreate, enabled = !generating) {
            Text("Create pack")
        }
        TextButton(onClick = onUseStock) {
            Text(if (state.appliedPackId == null) "Using stock icons" else "Use stock icons")
        }
        state.packs.forEach { pack ->
            PackRow(
                pack = pack,
                applied = pack.id == state.appliedPackId,
                generating = generating,
                onApply = { onApply(pack.id) },
                onOpen = { onOpen(pack.id) },
                onDelete = { onDelete(pack.id) }
            )
        }
        if (state.packs.isEmpty()) {
            Text("No packs yet. Create one with an on-device style — or Gemini if you have a paid key.")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PackRow(
    pack: IconPack,
    applied: Boolean,
    generating: Boolean,
    onApply: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp)
    ) {
        Text(if (applied) "${pack.name}  ·  Applied" else pack.name, fontSize = 20.sp)
        Text(
            "${pack.stylePrompt.ifBlank { "No style" }}  ·  ${pack.iconCount} icons  ·  ${if (pack.generatorMode == GenerationMode.GEMINI) "Gemini" else "On-device"}",
            fontSize = 14.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onApply, enabled = !applied && !generating) { Text("Apply") }
            TextButton(onClick = onOpen) { Text("Open") }
            TextButton(onClick = onDelete, enabled = !generating) { Text("Delete") }
        }
    }
}

@Composable
private fun CreatePackForm(
    state: IconPacksUiState,
    generating: Boolean,
    onNameChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onModeChange: (GenerationMode) -> Unit,
    onPresetSelected: (String) -> Unit,
    onStrengthChange: (Float) -> Unit,
    onPickCustomStyle: () -> Unit,
    onCreate: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Mode", fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onModeChange(GenerationMode.ON_DEVICE) }) {
                Text(if (state.createMode == GenerationMode.ON_DEVICE) "On-device styles ✓" else "On-device styles")
            }
            TextButton(onClick = { onModeChange(GenerationMode.GEMINI) }) {
                Text(if (state.createMode == GenerationMode.GEMINI) "Gemini ✓" else "Gemini")
            }
        }
        OutlinedTextField(
            value = state.createName,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Pack name") },
            placeholder = { Text("Neon nights") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = fieldColors()
        )
        if (state.createMode == GenerationMode.ON_DEVICE) {
            Text("Choose a style. Neural style transfer runs on this phone — no API key.")
            StylePresetGrid(
                selectedId = state.createPresetId,
                onPresetSelected = onPresetSelected
            )
            TextButton(onClick = onPickCustomStyle) { Text("Use my photo") }
            if (state.createPresetId == StylePreset.CUSTOM_ID) {
                val preview = remember(state.customStyleBytes) {
                    state.customStyleBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                }
                if (preview != null) {
                    Image(
                        bitmap = preview.asImageBitmap(),
                        contentDescription = "Custom style",
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Pick a photo from the gallery to use as the style.")
                }
            }
            Text("Style strength  ${(state.createStrength * 100).toInt()}%")
            Slider(
                value = state.createStrength,
                onValueChange = onStrengthChange,
                valueRange = 0.2f..1f
            )
        } else {
            OutlinedTextField(
                value = state.createPrompt,
                onValueChange = onPromptChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("Style prompt") },
                placeholder = { Text("neon cyberpunk") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = fieldColors()
            )
            Text("Gemini needs a paid AI Studio key. Free-tier keys have no image quota.")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onCreate,
                enabled = state.canStartCreate && !generating
            ) { Text("Start") }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun StylePresetGrid(
    selectedId: String,
    onPresetSelected: (String) -> Unit
) {
    val context = LocalContext.current
    StylePreset.ALL.chunked(3).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { preset ->
                val bitmap = remember(preset.id) {
                    context.assets.open(preset.assetPath).use { BitmapFactory.decodeStream(it) }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPresetSelected(preset.id) }
                        .then(
                            if (preset.id == selectedId) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground)
                            } else {
                                Modifier
                            }
                        )
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = preset.displayName,
                            modifier = Modifier.size(64.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(preset.displayName, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun PackDetail(
    pack: IconPack,
    apps: List<App>,
    applied: Boolean,
    hasApiKey: Boolean,
    generating: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onRegenerate: (App) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(pack.stylePrompt.ifBlank { "No style prompt" })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onApply, enabled = !applied && !generating) {
                Text(if (applied) "Applied" else "Apply")
            }
            TextButton(onClick = onDelete, enabled = !generating) { Text("Delete") }
            TextButton(onClick = onBack) { Text("Back") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(apps, key = { "${it.packageName}:${it.userSerial}" }) { app ->
                val hasIcon = pack.icons.containsKey(app.packageName)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasIcon) app.appName else "${app.appName}  ·  stock",
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp
                    )
                    TextButton(
                        onClick = { onRegenerate(app) },
                        enabled = !generating && (
                            pack.generatorMode == GenerationMode.ON_DEVICE || hasApiKey
                            )
                    ) { Text("Regenerate") }
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onBackground,
    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.onBackground,
    unfocusedLabelColor = MaterialTheme.colorScheme.onBackground,
    cursorColor = MaterialTheme.colorScheme.onBackground,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
)

private fun openApiKeyPage(context: Context) {
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse(context.getString(R.string.google_ai_studio_api_key_url))
    )
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No browser available; the pasted-key field still works.
    }
}
