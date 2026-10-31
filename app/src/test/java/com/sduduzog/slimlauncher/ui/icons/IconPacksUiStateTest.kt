package com.sduduzog.slimlauncher.ui.icons

import com.google.common.truth.Truth.assertThat
import com.sduduzog.slimlauncher.icons.GenerationMode
import com.sduduzog.slimlauncher.icons.StylePreset
import org.junit.Test

class IconPacksUiStateTest {

    @Test
    fun defaultModeIsOnDeviceAndNeedsNoKey() {
        val state = IconPacksUiState()
        assertThat(state.createMode).isEqualTo(GenerationMode.ON_DEVICE)
        assertThat(state.canStartCreate).isTrue()
        assertThat(state.createPresetId).isEqualTo(StylePreset.ALL.first().id)
    }

    @Test
    fun customPresetRequiresAPhoto() {
        val missing = IconPacksUiState(createPresetId = StylePreset.CUSTOM_ID)
        assertThat(missing.canStartCreate).isFalse()
        val ready = missing.copy(customStyleBytes = byteArrayOf(1, 2, 3))
        assertThat(ready.canStartCreate).isTrue()
    }

    @Test
    fun geminiRequiresPromptAndKey() {
        val empty = IconPacksUiState(createMode = GenerationMode.GEMINI)
        assertThat(empty.canStartCreate).isFalse()
        val keyed = empty.copy(hasApiKey = true, createPrompt = "neon")
        assertThat(keyed.canStartCreate).isTrue()
    }
}
