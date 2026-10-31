package com.sduduzog.slimlauncher.icons

enum class BackplateShape { CIRCLE, SQUIRCLE }

enum class GenerationMode(val storageValue: String) {
    ON_DEVICE("on_device"),
    GEMINI("gemini");

    companion object {
        fun fromStorage(value: String?): GenerationMode {
            return if (value == GEMINI.storageValue) GEMINI else ON_DEVICE
        }
    }
}

data class StylePreset(
    val id: String,
    val displayName: String,
    val assetPath: String,
    val backplate: BackplateShape,
    val backplateColor: Int,
    val defaultStrength: Float,
    val postProcess: PostProcessSpec
) {
    companion object {
        const val CUSTOM_ID = "custom"
        const val WALLPAPER_ID = "wallpaper"

        val WALLPAPER = StylePreset(
            id = WALLPAPER_ID,
            displayName = "Wallpaper",
            assetPath = "",
            backplate = BackplateShape.SQUIRCLE,
            backplateColor = 0xFF111111.toInt(),
            defaultStrength = 0.7f,
            postProcess = PostProcessSpec()
        )

        val CUSTOM = WALLPAPER.copy(
            id = CUSTOM_ID,
            displayName = "Custom"
        )

        fun byId(id: String?): StylePreset {
            return when (id) {
                CUSTOM_ID -> CUSTOM
                else -> WALLPAPER
            }
        }
    }
}

data class PostProcessSpec(
    val palette: IntArray? = null,
    val paletteMix: Float = 0f,
    val duotoneShadows: Int? = null,
    val duotoneHighlights: Int? = null,
    val duotoneMix: Float = 0f,
    val edgeGlowColor: Int? = null,
    val edgeGlowStrength: Float = 0f,
    val posterizeLevels: Int? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PostProcessSpec) return false
        return palette.contentEquals(other.palette) &&
            paletteMix == other.paletteMix &&
            duotoneShadows == other.duotoneShadows &&
            duotoneHighlights == other.duotoneHighlights &&
            duotoneMix == other.duotoneMix &&
            edgeGlowColor == other.edgeGlowColor &&
            edgeGlowStrength == other.edgeGlowStrength &&
            posterizeLevels == other.posterizeLevels
    }

    override fun hashCode(): Int {
        var result = palette.contentHashCode()
        result = 31 * result + paletteMix.hashCode()
        result = 31 * result + (duotoneShadows ?: 0)
        result = 31 * result + (duotoneHighlights ?: 0)
        result = 31 * result + duotoneMix.hashCode()
        result = 31 * result + (edgeGlowColor ?: 0)
        result = 31 * result + edgeGlowStrength.hashCode()
        result = 31 * result + (posterizeLevels ?: 0)
        return result
    }
}

data class PackCreateSpec(
    val name: String,
    val stylePrompt: String,
    val generatorMode: GenerationMode = GenerationMode.ON_DEVICE,
    val stylePresetId: String? = null,
    val styleStrength: Float = 0.7f,
    val customStyleBytes: ByteArray? = null,
    val fixedId: String? = null
)
