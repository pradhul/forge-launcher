package com.sduduzog.slimlauncher.icons

data class IconPack(
    val id: String,
    val name: String,
    val stylePrompt: String,
    val createdAtMillis: Long,
    val icons: Map<String, String> = emptyMap()
) {
    val iconCount: Int get() = icons.size
}
