package com.sduduzog.slimlauncher.ui.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IconPacksUiStateTest {

    @Test
    fun wallpaperStyledIconsDefaultOn() {
        val state = IconPacksUiState()
        assertThat(state.styledIcons).isTrue()
        assertThat(state.strength).isEqualTo(0.7f)
    }

    @Test
    fun stockModeTurnsStyledIconsOff() {
        val state = IconPacksUiState(styledIcons = false)
        assertThat(state.styledIcons).isFalse()
    }
}
