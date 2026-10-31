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

        val ALL: List<StylePreset> = listOf(
            StylePreset(
                id = "neon",
                displayName = "Neon cyberpunk",
                assetPath = "style_transfer/styles/neon.png",
                backplate = BackplateShape.SQUIRCLE,
                backplateColor = 0xFF0B0614.toInt(),
                defaultStrength = 0.72f,
                postProcess = PostProcessSpec(
                    duotoneShadows = 0xFF1A0433.toInt(),
                    duotoneHighlights = 0xFF39F3FF.toInt(),
                    duotoneMix = 0.45f,
                    edgeGlowColor = 0xFFFF2BD6.toInt(),
                    edgeGlowStrength = 0.85f,
                    posterizeLevels = null
                )
            ),
            StylePreset(
                id = "watercolor",
                displayName = "Watercolor",
                assetPath = "style_transfer/styles/watercolor.png",
                backplate = BackplateShape.CIRCLE,
                backplateColor = 0xFFEDE6DC.toInt(),
                defaultStrength = 0.65f,
                postProcess = PostProcessSpec(
                    palette = intArrayOf(
                        0xFFF4EDE4.toInt(), 0xFFD9B8A3.toInt(),
                        0xFF7EA8C5.toInt(), 0xFFC56B5B.toInt()
                    ),
                    paletteMix = 0.35f
                )
            ),
            StylePreset(
                id = "pastel",
                displayName = "Pastel",
                assetPath = "style_transfer/styles/pastel.png",
                backplate = BackplateShape.CIRCLE,
                backplateColor = 0xFFFFF3F6.toInt(),
                defaultStrength = 0.6f,
                postProcess = PostProcessSpec(
                    palette = intArrayOf(
                        0xFFFFB7C5.toInt(), 0xFFBAE1FF.toInt(),
                        0xFFCAE7B9.toInt(), 0xFFFFDFBA.toInt(),
                        0xFFDDC6F6.toInt()
                    ),
                    paletteMix = 0.4f
                )
            ),
            StylePreset(
                id = "pixel",
                displayName = "Retro pixel",
                assetPath = "style_transfer/styles/pixel.png",
                backplate = BackplateShape.SQUIRCLE,
                backplateColor = 0xFF141418.toInt(),
                defaultStrength = 0.7f,
                postProcess = PostProcessSpec(
                    posterizeLevels = 5,
                    edgeGlowColor = 0xFFFFFFFF.toInt(),
                    edgeGlowStrength = 0.25f
                )
            ),
            StylePreset(
                id = "ink",
                displayName = "Monochrome ink",
                assetPath = "style_transfer/styles/ink.png",
                backplate = BackplateShape.CIRCLE,
                backplateColor = 0xFFF5F2E6.toInt(),
                defaultStrength = 0.68f,
                postProcess = PostProcessSpec(
                    duotoneShadows = 0xFF141210.toInt(),
                    duotoneHighlights = 0xFFF5F2E6.toInt(),
                    duotoneMix = 0.85f
                )
            ),
            StylePreset(
                id = "gold",
                displayName = "Gold metallic",
                assetPath = "style_transfer/styles/gold.png",
                backplate = BackplateShape.SQUIRCLE,
                backplateColor = 0xFF2A1808.toInt(),
                defaultStrength = 0.7f,
                postProcess = PostProcessSpec(
                    duotoneShadows = 0xFF3D2208.toInt(),
                    duotoneHighlights = 0xFFFFE08A.toInt(),
                    duotoneMix = 0.7f
                )
            )
        )

        fun byId(id: String?): StylePreset? = ALL.firstOrNull { it.id == id }

        val CUSTOM = StylePreset(
            id = CUSTOM_ID,
            displayName = "My photo",
            assetPath = "",
            backplate = BackplateShape.SQUIRCLE,
            backplateColor = 0xFF111111.toInt(),
            defaultStrength = 0.7f,
            postProcess = PostProcessSpec()
        )
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
    val customStyleBytes: ByteArray? = null
)
