package com.sduduzog.slimlauncher.ui.icons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sduduzog.slimlauncher.data.model.App
import com.sduduzog.slimlauncher.icons.ApiKeyStore
import com.sduduzog.slimlauncher.icons.GenerationProgress
import com.sduduzog.slimlauncher.icons.IconApplyLogic
import com.sduduzog.slimlauncher.icons.IconPack
import com.sduduzog.slimlauncher.icons.IconPackGenerator
import com.sduduzog.slimlauncher.icons.IconPackStore
import com.sduduzog.slimlauncher.icons.IconPreferences
import com.sduduzog.slimlauncher.icons.InstalledAppsQuery
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val selectedPackId: String? = null,
    val installedApps: List<App> = emptyList(),
    val showCreate: Boolean = false,
    val confirmDeleteId: String? = null
) {
    val selectedPack: IconPack? get() = packs.firstOrNull { it.id == selectedPackId }
}

@HiltViewModel
class IconPacksViewModel @Inject constructor(
    private val store: IconPackStore,
    private val prefs: IconPreferences,
    private val apiKeyStore: ApiKeyStore,
    private val generator: IconPackGenerator,
    private val installedAppsQuery: InstalledAppsQuery
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
                installedApps = installedAppsQuery.load()
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

    fun showCreate(show: Boolean) {
        _ui.update { it.copy(showCreate = show) }
    }

    fun createPack() {
        val state = _ui.value
        if (state.createPrompt.isBlank() || generator.isRunning) return
        if (!apiKeyStore.hasKey()) return
        val pack = store.createPack(state.createName, state.createPrompt)
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
        prefs.applyPack(packId)
        refresh()
    }

    fun useStockIcons() {
        prefs.revertToStock()
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
        val packId = _ui.value.selectedPackId ?: return
        if (!apiKeyStore.hasKey() || generator.isRunning) return
        generator.regenerate(packId, app)
    }

    fun cancelGeneration() {
        generator.cancel()
    }
}
