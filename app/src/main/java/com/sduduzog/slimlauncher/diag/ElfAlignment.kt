package com.sduduzog.slimlauncher.diag

/**
 * ELF64 little-endian PT_LOAD alignment for 16 KB page-size devices.
 * Android 15+ (Pixel 10a) requires p_align >= 0x4000 on every LOAD segment.
 */
object ElfAlignment {
    const val MIN_P_ALIGN = 0x4000L
    const val PT_LOAD = 1
    const val ELFCLASS64 = 2
    const val ELFDATA2LSB = 1
    const val PHDR64_SIZE = 56

    data class LoadSegment(
        val align: Long,
        val offset: Long,
        val filesz: Long
    )

    fun hasElfMagic(bytes: ByteArray): Boolean {
        return bytes.size >= 16 &&
            bytes[0] == 0x7f.toByte() &&
            bytes[1] == 'E'.code.toByte() &&
            bytes[2] == 'L'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()
    }

    fun loadSegments(bytes: ByteArray): List<LoadSegment> {
        require(hasElfMagic(bytes)) { "not ELF" }
        require(bytes[4].toInt() == ELFCLASS64) { "not ELF64" }
        require(bytes[5].toInt() == ELFDATA2LSB) { "not little-endian" }
        val phoff = u64(bytes, 32)
        val phentsize = u16(bytes, 54)
        val phnum = u16(bytes, 56)
        require(phentsize >= PHDR64_SIZE) { "phdr too small" }
        val out = ArrayList<LoadSegment>(phnum)
        for (index in 0 until phnum) {
            val base = Math.addExact(phoff.toInt(), index * phentsize)
            val type = u32(bytes, base)
            if (type != PT_LOAD) continue
            out.add(
                LoadSegment(
                    offset = u64(bytes, base + 8),
                    filesz = u64(bytes, base + 32),
                    align = u64(bytes, base + 48)
                )
            )
        }
        return out
    }

    fun unalignedLoads(bytes: ByteArray): List<LoadSegment> {
        return loadSegments(bytes).filter { it.align < MIN_P_ALIGN }
    }

    fun isAligned(bytes: ByteArray): Boolean = unalignedLoads(bytes).isEmpty()

    fun reportSo(name: String, bytes: ByteArray): String {
        val loads = loadSegments(bytes)
        val bad = loads.filter { it.align < MIN_P_ALIGN }
        val minAlign = loads.minOfOrNull { it.align } ?: 0L
        return if (bad.isEmpty()) {
            "OK $name loads=${loads.size} min_p_align=0x${minAlign.toString(16)}"
        } else {
            "FAIL $name unaligned=" +
                bad.joinToString { "0x${it.align.toString(16)}" }
        }
    }

    fun u16(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    fun u32(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    fun u64(bytes: ByteArray, offset: Int): Long {
        return (u32(bytes, offset).toLong() and 0xFFFFFFFFL) or
            ((u32(bytes, offset + 4).toLong() and 0xFFFFFFFFL) shl 32)
    }

    fun putU16(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }

    fun putU32(bytes: ByteArray, offset: Int, value: Int) {
        bytes[offset] = (value and 0xFF).toByte()
        bytes[offset + 1] = ((value ushr 8) and 0xFF).toByte()
        bytes[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        bytes[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    fun putU64(bytes: ByteArray, offset: Int, value: Long) {
        putU32(bytes, offset, (value and 0xFFFFFFFFL).toInt())
        putU32(bytes, offset + 4, ((value ushr 32) and 0xFFFFFFFFL).toInt())
    }

    /** Minimal ELF64 with [aligns] PT_LOAD p_align values, for tests. */
    fun syntheticElf64(vararg aligns: Long): ByteArray {
        val phoff = 64
        val bytes = ByteArray(phoff + aligns.size * PHDR64_SIZE)
        bytes[0] = 0x7f
        bytes[1] = 'E'.code.toByte()
        bytes[2] = 'L'.code.toByte()
        bytes[3] = 'F'.code.toByte()
        bytes[4] = ELFCLASS64.toByte()
        bytes[5] = ELFDATA2LSB.toByte()
        putU64(bytes, 32, phoff.toLong())
        putU16(bytes, 54, PHDR64_SIZE)
        putU16(bytes, 56, aligns.size)
        aligns.forEachIndexed { index, align ->
            val base = phoff + index * PHDR64_SIZE
            putU32(bytes, base, PT_LOAD)
            putU64(bytes, base + 8, 0)
            putU64(bytes, base + 32, 0)
            putU64(bytes, base + 48, align)
        }
        return bytes
    }
}
