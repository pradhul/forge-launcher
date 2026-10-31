package com.sduduzog.slimlauncher.icons

import android.content.SharedPreferences
import androidx.core.content.edit
import com.sduduzog.slimlauncher.wallpaper.ThemeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

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

    var themeSourceId: String?
        get() = prefs.getString(KEY_THEME_SOURCE, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit { putString(KEY_THEME_SOURCE, value) }
            notifyIconsChanged()
        }

    var themeSource: ThemeSource?
        get() = ThemeSource.fromId(themeSourceId)
        set(value) {
            themeSourceId = value?.id
        }

    var bingWallpaperEnabled: Boolean
        get() = prefs.getBoolean(KEY_BING_ENABLED, false)
        set(value) {
            prefs.edit { putBoolean(KEY_BING_ENABLED, value) }
            notifyIconsChanged()
        }

    var bingUnmeteredOnly: Boolean
        get() = prefs.getBoolean(KEY_BING_UNMETERED, true)
        set(value) {
            prefs.edit { putBoolean(KEY_BING_UNMETERED, value) }
        }

    var bingSetLockScreen: Boolean
        get() = prefs.getBoolean(KEY_BING_LOCK, false)
        set(value) {
            prefs.edit { putBoolean(KEY_BING_LOCK, value) }
        }

    var wallpaperPresent: Boolean
        get() = prefs.getBoolean(KEY_WALLPAPER_PRESENT, false)
        set(value) {
            prefs.edit { putBoolean(KEY_WALLPAPER_PRESENT, value) }
            notifyIconsChanged()
        }

    var wallpaperTheme: PackTheme?
        get() = PackTheme.fromJson(
            prefs.getString(KEY_WALLPAPER_THEME, null)?.let { raw ->
                runCatching { JSONObject(raw) }.getOrNull()
            }
        )
        set(value) {
            prefs.edit {
                if (value == null) remove(KEY_WALLPAPER_THEME)
                else putString(KEY_WALLPAPER_THEME, value.toJson().toString())
            }
            notifyIconsChanged()
        }

    var bingTitle: String
        get() = prefs.getString(KEY_BING_TITLE, "") ?: ""
        set(value) {
            prefs.edit { putString(KEY_BING_TITLE, value) }
        }

    var bingCopyright: String
        get() = prefs.getString(KEY_BING_COPYRIGHT, "") ?: ""
        set(value) {
            prefs.edit { putString(KEY_BING_COPYRIGHT, value) }
        }

    var bingStartdate: String
        get() = prefs.getString(KEY_BING_STARTDATE, "") ?: ""
        set(value) {
            prefs.edit { putString(KEY_BING_STARTDATE, value) }
        }

    var bingLastError: String
        get() = prefs.getString(KEY_BING_ERROR, "") ?: ""
        set(value) {
            prefs.edit { putString(KEY_BING_ERROR, value) }
        }

    var openKeyboardOnDrawer: Boolean
        get() = prefs.getBoolean(KEY_OPEN_KEYBOARD, false)
        set(value) {
            prefs.edit { putBoolean(KEY_OPEN_KEYBOARD, value) }
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
        const val KEY_THEME_SOURCE = "key_theme_source"
        const val KEY_BING_ENABLED = "key_bing_wallpaper_enabled"
        const val KEY_BING_UNMETERED = "key_bing_unmetered_only"
        const val KEY_BING_LOCK = "key_bing_set_lock"
        const val KEY_WALLPAPER_PRESENT = "key_wallpaper_present"
        const val KEY_WALLPAPER_THEME = "key_wallpaper_theme"
        const val KEY_BING_TITLE = "key_bing_title"
        const val KEY_BING_COPYRIGHT = "key_bing_copyright"
        const val KEY_BING_STARTDATE = "key_bing_startdate"
        const val KEY_BING_ERROR = "key_bing_last_error"
        const val KEY_OPEN_KEYBOARD = "key_open_keyboard_on_drawer"
    }
}
