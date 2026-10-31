package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class IconPackStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun store(): FileIconPackStore = FileIconPackStore(folder.newFolder("icon_packs"))

    @Test
    fun createListAndReadPack() {
        val store = store()
        val pack = store.createPack("Neon", "neon cyberpunk", createdAtMillis = 100)

        val listed = store.listPacks()
        assertThat(listed).hasSize(1)
        assertThat(listed[0].id).isEqualTo(pack.id)
        assertThat(listed[0].name).isEqualTo("Neon")
        assertThat(listed[0].stylePrompt).isEqualTo("neon cyberpunk")
        assertThat(listed[0].iconCount).isEqualTo(0)
        assertThat(store.getPack(pack.id)).isEqualTo(pack)
    }

    @Test
    fun saveIconThenResolveFile() {
        val store = store()
        val pack = store.createPack("Ink", "ink wash", createdAtMillis = 1)
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        store.saveIcon(pack.id, "com.example.mail", png)

        val updated = store.getPack(pack.id)
        assertThat(updated!!.icons).containsKey("com.example.mail")
        assertThat(store.iconExists(pack.id, "com.example.mail")).isTrue()
        val file = store.getIconFile(pack.id, "com.example.mail")
        assertThat(file).isNotNull()
        assertThat(file!!.readBytes()).isEqualTo(png)
        assertThat(file.name).isEqualTo("com.example.mail.png")
    }

    @Test
    fun missingIconFallsBack() {
        val store = store()
        val pack = store.createPack("Partial", "glass", createdAtMillis = 2)
        store.saveIcon(pack.id, "com.example.phone", byteArrayOf(1, 2, 3))

        assertThat(store.getIconFile(pack.id, "com.example.missing")).isNull()
        assertThat(store.iconExists(pack.id, "com.example.missing")).isFalse()
    }

    @Test
    fun deletePackRemovesFiles() {
        val store = store()
        val pack = store.createPack("Temp", "temp", createdAtMillis = 3)
        store.saveIcon(pack.id, "com.example.app", byteArrayOf(9))

        store.deletePack(pack.id)

        assertThat(store.listPacks()).isEmpty()
        assertThat(store.getPack(pack.id)).isNull()
        assertThat(store.getIconFile(pack.id, "com.example.app")).isNull()
    }

    @Test
    fun deleteIconLeavesThePack() {
        val store = store()
        val pack = store.createPack("Edit", "edit", createdAtMillis = 4)
        store.saveIcon(pack.id, "com.example.one", byteArrayOf(1))
        store.saveIcon(pack.id, "com.example.two", byteArrayOf(2))

        store.deleteIcon(pack.id, "com.example.one")

        val updated = store.getPack(pack.id)!!
        assertThat(updated.icons).doesNotContainKey("com.example.one")
        assertThat(updated.icons).containsKey("com.example.two")
        assertThat(store.getIconFile(pack.id, "com.example.one")).isNull()
    }

    @Test
    fun listSortsNewestFirst() {
        val store = store()
        val older = store.createPack("Older", "a", createdAtMillis = 10)
        val newer = store.createPack("Newer", "b", createdAtMillis = 20)

        assertThat(store.listPacks().map { it.id }).containsExactly(newer.id, older.id).inOrder()
    }

    @Test
    fun blankNameUsesPrompt() {
        val store = store()
        val pack = store.createPack("  ", "sketch", createdAtMillis = 5)
        assertThat(pack.name).isEqualTo("sketch")
    }

    @Test
    fun iconFileNameSanitizesUnsafeCharacters() {
        assertThat(FileIconPackStore.iconFileName("com.foo/bar:baz"))
            .isEqualTo("com.foo_bar_baz.png")
        assertThat(FileIconPackStore.iconFileName("com.ok.app"))
            .isEqualTo("com.ok.app.png")
    }
}
