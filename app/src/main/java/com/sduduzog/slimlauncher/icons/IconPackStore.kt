package com.sduduzog.slimlauncher.icons

import java.io.File

interface IconPackStore {
    fun listPacks(): List<IconPack>
    fun getPack(id: String): IconPack?
    fun createPack(name: String, stylePrompt: String, createdAtMillis: Long = System.currentTimeMillis()): IconPack
    fun saveIcon(packId: String, packageName: String, pngBytes: ByteArray)
    fun deleteIcon(packId: String, packageName: String)
    fun deletePack(id: String)
    fun getIconFile(packId: String, packageName: String): File?
    fun iconExists(packId: String, packageName: String): Boolean
}
