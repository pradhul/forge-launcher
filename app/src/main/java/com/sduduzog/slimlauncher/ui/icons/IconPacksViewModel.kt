package com.sduduzog.slimlauncher.ui.icons

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.icons.ApiKeyStore
import com.sduduzog.slimlauncher.icons.GenerationMode
import com.sduduzog.slimlauncher.icons.GenerationProgress
import com.sduduzog.slimlauncher.icons.IconApplyLogic
import com.sduduzog.slimlauncher.icons.IconPack
import com.sduduzog.slimlauncher.icons.IconPackGenerator
import com.sduduzog.slimlauncher.icons.IconPackStore
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.InstalledAppsQuery
import com.sduduzog.slimlauncher.icons.PackCreateSpec
import com.sduduzog.slimlauncher.icons.PackTheme
import com.sduduzog.slimlauncher.icons.PackThemeController
import com.sduduzog.slimlauncher.icons.PackThemeExtractor
import com.sduduzog.slimlauncher.icons.StylePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class IconPacksUiState(
    val packs: List<IconPack> = emptyList(),
    val appliedPackId: String? = null,
    val hasApiKey: Boolean = false,
    val keyDraft: String = "",
    val keySaved: Boolean = false,
    val createName: String = "",
    val createPrompt: String = "",
    val createMode: GenerationMode = GenerationMode.ON_DEVICE,
    val createPresetId: String = StylePreset.ALL.first().id,
    val createStrength: Float = StylePreset.ALL.first().defaultStrength,
    val customStyleBytes: ByteArray? = null,
    val selectedPackId: String? = null,
    val installedApps: List<App> = emptyList(),
    val showCreate: Boolean = false,
    val confirmDeleteId: String? = null,
    val matchBackground: Boolean = true,
    val theme: PackTheme = PackTheme.STOCK
) {
    val selectedPack: IconPack? get() = packs.firstOrNull { it.id == selectedPackId }
    val canStartCreate: Boolean
        get() = when (createMode) {
            GenerationMode.ON_DEVICE -> {
                createPresetId != StylePreset.CUSTOM_ID || customStyleBytes != null
            }
            GenerationMode.GEMINI -> createPrompt.isNotBlank() && hasApiKey
        }
}

@HiltViewModel
class IconPacksViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: IconPackStore,
    private val prefs: IconPreferences,
    private val apiKeyStore: ApiKeyStore,
    private val generator: IconPackGenerator,
    private val installedAppsQuery: InstalledAppsQuery,
    private val themeExtractor: PackThemeExtractor,
    private val themeController: PackThemeController
) : ViewModel() {

    private val _ui = MutableStateFlow(IconPacksUiState())
    val ui: StateFlow<IconPacksUiState> = _ui.asStateFlow()

    val progress: StateFlow<GenerationProgress> = generator.progress.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        generator.progress.value
    )

    init {
        refresh()
    }

    fun refresh() {
        _ui.update {
            it.copy(
                packs = store.listPacks(),
                appliedPackId = prefs.appliedPackId,
                hasApiKey = apiKeyStore.hasKey(),
                installedApps = installedAppsQuery.load(),
                matchBackground = prefs.matchBackgroundToPack,
                theme = themeController.active()
            )
        }
    }

    fun onKeyDraftChange(value: String) {
        _ui.update { it.copy(keyDraft = value, keySaved = false) }
    }

    fun saveApiKey() {
        val key = _ui.value.keyDraft.trim()
        if (key.isBlank()) return
        apiKeyStore.save(key)
        _ui.update { it.copy(keyDraft = "", hasApiKey = true, keySaved = true) }
    }

    fun clearApiKey() {
        apiKeyStore.clear()
        _ui.update { it.copy(hasApiKey = false, keyDraft = "", keySaved = false) }
    }

    fun onCreateNameChange(value: String) {
        _ui.update { it.copy(createName = value) }
    }

    fun onCreatePromptChange(value: String) {
        _ui.update { it.copy(createPrompt = value) }
    }

    fun onCreateModeChange(mode: GenerationMode) {
        _ui.update { it.copy(createMode = mode) }
    }

    fun onPresetSelected(presetId: String) {
        val preset = StylePreset.byId(presetId) ?: StylePreset.CUSTOM
        _ui.update {
            it.copy(
                createPresetId = presetId,
                createStrength = preset.defaultStrength,
                createName = it.createName.ifBlank { preset.displayName }
            )
        }
    }

    fun onStrengthChange(value: Float) {
        _ui.update { it.copy(createStrength = value.coerceIn(0.2f, 1f)) }
    }

    fun onCustomStylePicked(uri: Uri) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        _ui.update {
            it.copy(
                customStyleBytes = bytes,
                createPresetId = StylePreset.CUSTOM_ID,
                createName = it.createName.ifBlank { "My photo" }
            )
        }
    }

    fun showCreate(show: Boolean) {
        _ui.update { it.copy(showCreate = show) }
    }

    fun createPack() {
        val state = _ui.value
        if (!state.canStartCreate || generator.isRunning) return
        val spec = PackCreateSpec(
            name = state.createName,
            stylePrompt = if (state.createMode == GenerationMode.GEMINI) {
                state.createPrompt
            } else {
                StylePreset.byId(state.createPresetId)?.displayName
                    ?: "Custom style"
            },
            generatorMode = state.createMode,
            stylePresetId = if (state.createMode == GenerationMode.ON_DEVICE) {
                state.createPresetId
            } else {
                null
            },
            styleStrength = state.createStrength,
            customStyleBytes = state.customStyleBytes.takeIf {
                state.createMode == GenerationMode.ON_DEVICE &&
                    state.createPresetId == StylePreset.CUSTOM_ID
            }
        )
        val pack = store.createPack(spec)
        themeExtractor.ensureTheme(pack.id)
        prefs.applyPack(pack.id)
        generator.startPack(pack, state.installedApps)
        _ui.update {
            it.copy(
                showCreate = false,
                createName = "",
                createPrompt = "",
                selectedPackId = pack.id
            )
        }
        refresh()
    }

    fun applyPack(packId: String) {
        val pack = themeExtractor.ensureTheme(packId) ?: store.getPack(packId)
        prefs.applyPack(packId)
        if (pack != null) {
            generator.fillMissing(pack, _ui.value.installedApps)
        }
        refresh()
    }

    fun useStockIcons() {
        prefs.revertToStock()
        refresh()
    }

    fun setMatchBackground(enabled: Boolean) {
        prefs.matchBackgroundToPack = enabled
        refresh()
    }

    fun requestDelete(packId: String) {
        _ui.update { it.copy(confirmDeleteId = packId) }
    }

    fun dismissDelete() {
        _ui.update { it.copy(confirmDeleteId = null) }
    }

    fun confirmDelete() {
        val packId = _ui.value.confirmDeleteId ?: return
        store.deletePack(packId)
        prefs.appliedPackId = IconApplyLogic.packIdAfterDelete(prefs.appliedPackId, packId)
        _ui.update {
            it.copy(
                confirmDeleteId = null,
                selectedPackId = it.selectedPackId.takeUnless { id -> id == packId }
            )
        }
        refresh()
    }

    fun openPack(packId: String) {
        _ui.update { it.copy(selectedPackId = packId, showCreate = false) }
        refresh()
    }

    fun closePack() {
        _ui.update { it.copy(selectedPackId = null) }
    }

    fun regenerate(app: App) {
        val pack = _ui.value.selectedPack ?: return
        if (generator.isRunning) return
        if (pack.generatorMode == GenerationMode.GEMINI && !apiKeyStore.hasKey()) return
        generator.regenerate(pack.id, app)
    }

    fun cancelGeneration() {
        generator.cancel()
    }
}
