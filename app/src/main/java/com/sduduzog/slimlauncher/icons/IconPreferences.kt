package com.sduduzog.slimlauncher.icons

import android.content.SharedPreferences
import androidx.core.content.edit
import com.sduduzog.slimlauncher.wallpaper.ChromeFadePrefs
import com.sduduzog.slimlauncher.wallpaper.LauncherFadeLogic
import com.sduduzog.slimlauncher.wallpaper.ThemeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

class IconPreferences(private val prefs: SharedPreferences) {

    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    private val _chrome = MutableStateFlow(readChrome())
    val chrome: StateFlow<ChromeFadePrefs> = _chrome.asStateFlow()

    var showIcons: Boolean
        get() = true
        set(@Suppress("UNUSED_PARAMETER") value) {
            // Toggle Icons was removed. Icons are always drawn.
        }

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

    var wallpaperStyledIcons: Boolean
        get() = prefs.getBoolean(KEY_WALLPAPER_STYLED_ICONS, true)
        set(value) {
            prefs.edit { putBoolean(KEY_WALLPAPER_STYLED_ICONS, value) }
            notifyIconsChanged()
        }

    var wallpaperStyleStrength: Float
        get() = prefs.getFloat(KEY_WALLPAPER_STYLE_STRENGTH, 0.7f).coerceIn(0.2f, 1f)
        set(value) {
            prefs.edit { putFloat(KEY_WALLPAPER_STYLE_STRENGTH, value.coerceIn(0.2f, 1f)) }
            notifyIconsChanged()
        }

    var wallpaperStyleHash: String?
        get() = prefs.getString(KEY_WALLPAPER_STYLE_HASH, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit { putString(KEY_WALLPAPER_STYLE_HASH, value) }
        }

    var lastAppliedPaletteKey: String?
        get() = prefs.getString(KEY_LAST_PALETTE, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit {
                if (value.isNullOrBlank()) remove(KEY_LAST_PALETTE)
                else putString(KEY_LAST_PALETTE, value)
            }
        }

    var homeFadePercent: Int
        get() = readPercent(KEY_HOME_FADE, LauncherFadeLogic.DEFAULT_HOME_FADE)
        set(value) {
            prefs.edit { putInt(KEY_HOME_FADE, LauncherFadeLogic.clampPercent(value)) }
            publishChrome()
        }

    var drawerBlurPercent: Int
        get() = readPercent(KEY_DRAWER_BLUR, LauncherFadeLogic.DEFAULT_DRAWER_BLUR)
        set(value) {
            prefs.edit { putInt(KEY_DRAWER_BLUR, LauncherFadeLogic.clampPercent(value)) }
            publishChrome()
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

    private fun readPercent(key: String, default: Int): Int {
        return LauncherFadeLogic.clampPercent(prefs.getInt(key, default))
    }

    private fun readChrome(): ChromeFadePrefs {
        return ChromeFadePrefs(
            homeFadePercent = homeFadePercent,
            drawerBlurPercent = drawerBlurPercent
        )
    }

    private fun publishChrome() {
        _chrome.value = readChrome()
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
        const val KEY_WALLPAPER_STYLED_ICONS = "key_wallpaper_styled_icons"
        const val KEY_WALLPAPER_STYLE_STRENGTH = "key_wallpaper_style_strength"
        const val KEY_WALLPAPER_STYLE_HASH = "key_wallpaper_style_hash"
        const val KEY_LAST_PALETTE = "key_last_applied_palette"
        const val KEY_HOME_FADE = "key_home_fade_percent"
        const val KEY_DRAWER_BLUR = "key_drawer_blur_percent"
    }
}
