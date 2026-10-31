package com.sduduzog.slimlauncher.icons

import android.content.SharedPreferences
import androidx.core.content.edit

class IconPreferences(private val prefs: SharedPreferences) {

    var showIcons: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ICONS, true)
        set(value) {
            prefs.edit { putBoolean(KEY_SHOW_ICONS, value) }
        }

    var appliedPackId: String?
        get() = prefs.getString(KEY_APPLIED_PACK_ID, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit { putString(KEY_APPLIED_PACK_ID, value) }
        }

    fun applyPack(packId: String?) {
        appliedPackId = packId
    }

    fun revertToStock() {
        appliedPackId = null
    }

    companion object {
        const val KEY_SHOW_ICONS = "key_show_icons"
        const val KEY_APPLIED_PACK_ID = "key_applied_pack_id"
    }
}
