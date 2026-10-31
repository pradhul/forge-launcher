package com.sduduzog.slimlauncher.icons

data class IconPack(
    val id: String,
    val name: String,
    val stylePrompt: String,
    val createdAtMillis: Long,
    val icons: Map<String, String> = emptyMap(),
    val generatorMode: GenerationMode = GenerationMode.ON_DEVICE,
    val stylePresetId: String? = null,
    val styleStrength: Float = 0.7f,
    val customStylePath: String? = null,
    val theme: PackTheme? = null
) {
    val iconCount: Int get() = icons.size
}
