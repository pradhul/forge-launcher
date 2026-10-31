package com.sduduzog.slimlauncher.icons

/**
 * Pure apply/fallback rules for launcher icons.
 *
 * Hidden when the user turns icons off. An applied pack wins when it has a PNG
 * for that package; otherwise the stock system icon is used.
 */
object IconApplyLogic {

    fun resolve(
        showIcons: Boolean,
        appliedPack: IconPack?,
        packageName: String
    ): ResolvedIcon {
        if (!showIcons) return ResolvedIcon.Hidden
        if (appliedPack != null) {
            val relativePath = appliedPack.icons[packageName]
            if (!relativePath.isNullOrBlank()) {
                return ResolvedIcon.Pack(appliedPack.id, relativePath)
            }
        }
        return ResolvedIcon.Stock
    }

    fun appliedPackOrNull(
        appliedPackId: String?,
        packs: List<IconPack>
    ): IconPack? {
        if (appliedPackId.isNullOrBlank()) return null
        return packs.firstOrNull { it.id == appliedPackId }
    }

    fun packIdAfterDelete(appliedPackId: String?, deletedPackId: String): String? {
        return if (appliedPackId == deletedPackId) null else appliedPackId
    }
}

sealed class ResolvedIcon {
    data object Hidden : ResolvedIcon()
    data object Stock : ResolvedIcon()
    data class Pack(val packId: String, val relativePath: String) : ResolvedIcon()
}
