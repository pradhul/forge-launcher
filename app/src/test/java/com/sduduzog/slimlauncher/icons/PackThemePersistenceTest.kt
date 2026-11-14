package com.sduduzog.slimlauncher.icons

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PackThemePersistenceTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun packThemeSurvivesManifestReload() {
        val root = folder.newFolder("icon_packs")
        val store = FileIconPackStore(root)
        val theme = PackTheme(
            background = 0xFF0B0614.toInt(),
            backgroundEnd = 0xFF1A0433.toInt(),
            onBackground = 0xFFF4F7FF.toInt(),
            accent = 0xFF39F3FF.toInt(),
            onAccent = 0xFF0B0614.toInt()
        )
        val pack = store.createPack(
            PackCreateSpec(name = "Neon", stylePrompt = "Neon", stylePresetId = "neon")
        )
        store.updatePack(pack.copy(theme = theme))

        val reloaded = FileIconPackStore(root).getPack(pack.id)
        assertThat(reloaded).isNotNull()
        assertThat(reloaded!!.theme).isEqualTo(theme)
        assertThat(reloaded.stylePresetId).isEqualTo("neon")
    }

    @Test
    fun matchBackgroundDefaultsOnAndRevertingPackRestoresStockTheme() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = IconPreferences(
            context.getSharedPreferences("theme_persist_test", Context.MODE_PRIVATE)
        )
        assertThat(prefs.matchBackgroundToPack).isTrue()

        val packTheme = PackTheme(
            background = 0xFF141418.toInt(),
            backgroundEnd = 0xFF000000.toInt(),
            onBackground = 0xFFE0E0E0.toInt(),
            accent = 0xFFFFFFFF.toInt(),
            onAccent = 0xFF141418.toInt()
        )
        prefs.applyPack("pack-pixel")
        val whileApplied = PackThemeLogic.resolve(prefs.matchBackgroundToPack, packTheme)
        assertThat(whileApplied).isEqualTo(packTheme)

        prefs.revertToStock()
        assertThat(prefs.appliedPackId).isNull()
        assertThat(PackThemeLogic.resolve(prefs.matchBackgroundToPack, null))
            .isEqualTo(PackTheme.STOCK)

        prefs.matchBackgroundToPack = false
        assertThat(prefs.matchBackgroundToPack).isFalse()
        assertThat(PackThemeLogic.resolve(prefs.matchBackgroundToPack, packTheme))
            .isEqualTo(PackTheme.STOCK)
    }
}
