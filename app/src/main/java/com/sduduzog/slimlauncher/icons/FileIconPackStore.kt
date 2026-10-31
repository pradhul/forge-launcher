package com.sduduzog.slimlauncher.icons

import org.json.JSONObject
import java.io.File
import java.util.UUID

class FileIconPackStore(private val rootDir: File) : IconPackStore {

    override fun listPacks(): List<IconPack> {
        if (!rootDir.isDirectory) return emptyList()
        return rootDir.listFiles()
            ?.mapNotNull { dir ->
                val manifest = File(dir, MANIFEST_NAME)
                if (manifest.isFile) readManifest(manifest) else null
            }
            ?.sortedByDescending { it.createdAtMillis }
            .orEmpty()
    }

    override fun getPack(id: String): IconPack? {
        val manifest = File(packDir(id), MANIFEST_NAME)
        if (!manifest.isFile) return null
        return readManifest(manifest)
    }

    override fun createPack(name: String, stylePrompt: String, createdAtMillis: Long): IconPack {
        return createPack(PackCreateSpec(name, stylePrompt), createdAtMillis)
    }

    override fun createPack(spec: PackCreateSpec, createdAtMillis: Long): IconPack {
        val trimmedName = spec.name.trim()
        val trimmedPrompt = spec.stylePrompt.trim()
        val packId = spec.fixedId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val existing = spec.fixedId?.let { getPack(it) }
        var customPath = existing?.customStylePath
        if (spec.customStyleBytes != null) {
            customPath = CUSTOM_STYLE_NAME
            val file = File(packDir(packId), customPath)
            file.parentFile?.mkdirs()
            file.writeBytes(spec.customStyleBytes)
        }
        val pack = IconPack(
            id = packId,
            name = trimmedName.ifBlank {
                existing?.name
                    ?: trimmedPrompt.ifBlank { StylePreset.byId(spec.stylePresetId).displayName }
            },
            stylePrompt = trimmedPrompt.ifBlank {
                existing?.stylePrompt
                    ?: StylePreset.byId(spec.stylePresetId).displayName
            },
            createdAtMillis = existing?.createdAtMillis ?: createdAtMillis,
            icons = existing?.icons ?: emptyMap(),
            generatorMode = spec.generatorMode,
            stylePresetId = spec.stylePresetId ?: existing?.stylePresetId,
            styleStrength = spec.styleStrength.coerceIn(0f, 1f),
            customStylePath = customPath,
            theme = existing?.theme
        )
        writeManifest(pack)
        return pack
    }

    @Synchronized
    override fun saveIcon(packId: String, packageName: String, pngBytes: ByteArray) {
        val pack = getPack(packId) ?: return
        val relativePath = "icons/${iconFileName(packageName)}"
        val file = File(packDir(packId), relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(pngBytes)
        writeManifest(pack.copy(icons = pack.icons + (packageName to relativePath)))
    }

    @Synchronized
    override fun deleteIcon(packId: String, packageName: String) {
        val pack = getPack(packId) ?: return
        val relativePath = pack.icons[packageName]
        if (relativePath != null) {
            File(packDir(packId), relativePath).delete()
        }
        writeManifest(pack.copy(icons = pack.icons - packageName))
    }

    override fun deletePack(id: String) {
        packDir(id).deleteRecursively()
    }

    override fun getIconFile(packId: String, packageName: String): File? {
        val pack = getPack(packId) ?: return null
        val relativePath = pack.icons[packageName] ?: return null
        val file = File(packDir(packId), relativePath)
        return file.takeIf { it.isFile }
    }

    override fun iconExists(packId: String, packageName: String): Boolean {
        return getIconFile(packId, packageName) != null
    }

    override fun getCustomStyleFile(packId: String): File? {
        val pack = getPack(packId) ?: return null
        val relative = pack.customStylePath ?: return null
        return File(packDir(packId), relative).takeIf { it.isFile }
    }

    @Synchronized
    override fun updatePack(pack: IconPack) {
        writeManifest(pack)
    }

    private fun packDir(id: String): File = File(rootDir, id)

    private fun writeManifest(pack: IconPack) {
        val dir = packDir(pack.id)
        dir.mkdirs()
        val tmp = File(dir, "$MANIFEST_NAME.tmp")
        tmp.writeText(toJson(pack))
        val target = File(dir, MANIFEST_NAME)
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
    }

    private fun readManifest(file: File): IconPack? {
        return try {
            val json = JSONObject(file.readText())
            val iconsJson = json.optJSONObject("icons") ?: JSONObject()
            val icons = buildMap {
                val keys = iconsJson.keys()
                while (keys.hasNext()) {
                    val packageName = keys.next()
                    put(packageName, iconsJson.getString(packageName))
                }
            }
            IconPack(
                id = json.getString("id"),
                name = json.getString("name"),
                stylePrompt = json.optString("stylePrompt"),
                createdAtMillis = json.optLong("createdAtMillis"),
                icons = icons,
                generatorMode = GenerationMode.fromStorage(json.optString("generatorMode")),
                stylePresetId = json.optString("stylePresetId").takeIf { it.isNotBlank() },
                styleStrength = json.optDouble("styleStrength", 0.7).toFloat(),
                customStylePath = json.optString("customStylePath").takeIf { it.isNotBlank() },
                theme = PackTheme.fromJson(json.optJSONObject("theme"))
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun toJson(pack: IconPack): String {
        val icons = JSONObject()
        pack.icons.forEach { (packageName, path) -> icons.put(packageName, path) }
        return JSONObject()
            .put("id", pack.id)
            .put("name", pack.name)
            .put("stylePrompt", pack.stylePrompt)
            .put("createdAtMillis", pack.createdAtMillis)
            .put("icons", icons)
            .put("generatorMode", pack.generatorMode.storageValue)
            .put("stylePresetId", pack.stylePresetId ?: "")
            .put("styleStrength", pack.styleStrength.toDouble())
            .put("customStylePath", pack.customStylePath ?: "")
            .put("theme", pack.theme?.toJson() ?: JSONObject.NULL)
            .toString()
    }

    companion object {
        const val MANIFEST_NAME = "manifest.json"
        const val CUSTOM_STYLE_NAME = "style.png"

        fun iconFileName(packageName: String): String {
            val safe = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            return "$safe.png"
        }
    }
}
