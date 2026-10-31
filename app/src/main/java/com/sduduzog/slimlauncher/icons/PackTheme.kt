package com.sduduzog.slimlauncher.icons

import org.json.JSONObject

data class PackTheme(
    val background: Int,
    val backgroundEnd: Int,
    val onBackground: Int,
    val accent: Int,
    val onAccent: Int
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("background", background)
            .put("backgroundEnd", backgroundEnd)
            .put("onBackground", onBackground)
            .put("accent", accent)
            .put("onAccent", onAccent)
    }

    companion object {
        val STOCK = PackTheme(
            background = 0xFFFFFFFF.toInt(),
            backgroundEnd = 0xFFF5F5F5.toInt(),
            onBackground = 0xFF080808.toInt(),
            accent = 0xFF080808.toInt(),
            onAccent = 0xFFFFFFFF.toInt()
        )

        fun fromJson(json: JSONObject?): PackTheme? {
            if (json == null) return null
            return try {
                PackTheme(
                    background = json.getInt("background"),
                    backgroundEnd = json.getInt("backgroundEnd"),
                    onBackground = json.getInt("onBackground"),
                    accent = json.getInt("accent"),
                    onAccent = json.getInt("onAccent")
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

object PackThemeLogic {
    fun fromSwatches(
        dominant: Int?,
        darkMuted: Int?,
        vibrant: Int?,
        muted: Int?,
        lightVibrant: Int? = null
    ): PackTheme {
        val seed = darkMuted ?: dominant ?: muted ?: 0xFF121212.toInt()
        val background = ContrastPicker.ensureDarkBackground(seed)
        val endSeed = dominant ?: darkMuted ?: muted ?: background
        val backgroundEnd = ContrastPicker.ensureDarkBackground(endSeed, maxLuminance = 0.28)
        val accent = vibrant ?: lightVibrant ?: muted ?: 0xFF39F3FF.toInt()
        val onBackground = ContrastPicker.pickReadable(
            background,
            intArrayOf(0xFFF7F7F7.toInt(), ContrastPicker.WHITE, ContrastPicker.BLACK)
        )
        val onAccent = ContrastPicker.pickReadable(accent)
        return PackTheme(
            background = background,
            backgroundEnd = if (backgroundEnd == background) {
                ContrastPicker.ensureDarkBackground(accent, maxLuminance = 0.18)
            } else {
                backgroundEnd
            },
            onBackground = onBackground,
            accent = accent,
            onAccent = onAccent
        )
    }

    fun resolve(matchBackground: Boolean, packTheme: PackTheme?): PackTheme {
        if (!matchBackground || packTheme == null) return PackTheme.STOCK
        return packTheme
    }
}
