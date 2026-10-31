package com.sduduzog.slimlauncher.icons

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class IconPreferences(private val prefs: SharedPreferences) {

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    var showIcons: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ICONS, true)
        set(value) {
            prefs.edit { putBoolean(KEY_SHOW_ICONS, value) }
            notifyIconsChanged()
        }

    // Icons are always drawn. The old toggle key is ignored so a previous
    // "Toggle Icons" tap cannot leave the launcher blank after an update.

    var appliedPackId: String?
        get() = prefs.getString(KEY_APPLIED_PACK_ID, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit { putString(KEY_APPLIED_PACK_ID, value) }
            notifyIconsChanged()
        }

    var homeDefaultsSeeded: Boolean
        get() = prefs.getBoolean(KEY_HOME_DEFAULTS_SEEDED, false)
        set(value) {
            prefs.edit { putBoolean(KEY_HOME_DEFAULTS_SEEDED, value) }
        }

    var matchBackgroundToPack: Boolean
        get() = prefs.getBoolean(KEY_MATCH_BACKGROUND, true)
        set(value) {
            prefs.edit { putBoolean(KEY_MATCH_BACKGROUND, value) }
            notifyIconsChanged()
        }

    fun applyPack(packId: String?) {
        appliedPackId = packId
    }

    fun revertToStock() {
        appliedPackId = null
    }

    fun notifyIconsChanged() {
        _revision.update { it + 1 }
    }

    companion object {
        const val KEY_SHOW_ICONS = "key_show_icons"
        const val KEY_APPLIED_PACK_ID = "key_applied_pack_id"
        const val KEY_HOME_DEFAULTS_SEEDED = "key_home_defaults_seeded"
        const val KEY_MATCH_BACKGROUND = "key_match_background_to_pack"
    }
}
