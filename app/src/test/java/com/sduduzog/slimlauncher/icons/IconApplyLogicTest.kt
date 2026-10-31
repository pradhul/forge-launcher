package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IconApplyLogicTest {

    private val pack = IconPack(
        id = "pack-1",
        name = "Neon",
        stylePrompt = "neon cyberpunk",
        createdAtMillis = 1L,
        icons = mapOf("com.example.mail" to "icons/com.example.mail.png")
    )

    @Test
    fun hiddenWhenIconsDisabled() {
        val resolved = IconApplyLogic.resolve(
            showIcons = false,
            appliedPack = pack,
            packageName = "com.example.mail"
        )
        assertThat(resolved).isEqualTo(ResolvedIcon.Hidden)
    }

    @Test
    fun packIconWhenPresent() {
        val resolved = IconApplyLogic.resolve(
            showIcons = true,
            appliedPack = pack,
            packageName = "com.example.mail"
        )
        assertThat(resolved).isEqualTo(
            ResolvedIcon.Pack("pack-1", "icons/com.example.mail.png")
        )
    }

    @Test
    fun packFallbackWhenPackMissingApp() {
        val resolved = IconApplyLogic.resolve(
            showIcons = true,
            appliedPack = pack,
            packageName = "com.android.stk"
        )
        assertThat(resolved).isEqualTo(ResolvedIcon.PackFallback("pack-1"))
    }

    @Test
    fun stockWhenNoPackApplied() {
        val resolved = IconApplyLogic.resolve(
            showIcons = true,
            appliedPack = null,
            packageName = "com.example.mail"
        )
        assertThat(resolved).isEqualTo(ResolvedIcon.Stock)
    }

    @Test
    fun appliedPackLookup() {
        assertThat(IconApplyLogic.appliedPackOrNull("pack-1", listOf(pack))).isEqualTo(pack)
        assertThat(IconApplyLogic.appliedPackOrNull("missing", listOf(pack))).isNull()
        assertThat(IconApplyLogic.appliedPackOrNull(null, listOf(pack))).isNull()
        assertThat(IconApplyLogic.appliedPackOrNull("", listOf(pack))).isNull()
    }

    @Test
    fun deletingAppliedPackRevertsToStock() {
        assertThat(IconApplyLogic.packIdAfterDelete("pack-1", "pack-1")).isNull()
        assertThat(IconApplyLogic.packIdAfterDelete("pack-1", "other")).isEqualTo("pack-1")
        assertThat(IconApplyLogic.packIdAfterDelete(null, "pack-1")).isNull()
    }
}
