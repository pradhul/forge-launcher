package com.sduduzog.slimlauncher.icons

/** How wallpaper theming is applied to app icons. */
enum class IconStyle(val storageValue: String) {
    THEMED("themed"),
    TINTED("tinted"),
    ORIGINAL("original"),
    ARTISTIC("artistic");

    companion object {
        val DEFAULT = THEMED
        fun fromStorage(value: String?, legacyArtistic: Boolean = false): IconStyle {
            values().firstOrNull { it.storageValue == value }?.let { return it }
            return if (legacyArtistic) ARTISTIC else DEFAULT
        }
    }
}
