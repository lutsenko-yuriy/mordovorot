package web.qr

/** The per-version numbers of ISO/IEC 18004 at error correction level M, versions 1 to 10 (a 10-version cap,
 *  213 bytes, is plenty for a board link). */
internal object QrTables {
    const val MIN_VERSION = 1
    const val MAX_VERSION = 10

    private val ECC_PER_BLOCK = intArrayOf(10, 16, 26, 18, 24, 16, 18, 22, 22, 26)
    private val BLOCKS = intArrayOf(1, 1, 1, 2, 2, 4, 4, 4, 5, 5)

    fun size(version: Int) = version * 4 + 17

    fun eccPerBlock(version: Int) = ECC_PER_BLOCK[version - 1]

    fun blocks(version: Int) = BLOCKS[version - 1]

    /** Data and check bytes together: the modules left after the function patterns, in whole bytes. */
    fun codewords(version: Int): Int {
        var modules = (16 * version + 128) * version + 64
        if (version >= 2) {
            val aligns = version / 7 + 2
            modules -= (25 * aligns - 10) * aligns - 55
            if (version >= 7) modules -= 36
        }
        return modules / 8
    }

    fun dataCodewords(version: Int) = codewords(version) - eccPerBlock(version) * blocks(version)

    /** Where the alignment squares' centres sit, along one axis. */
    fun alignmentCentres(version: Int): List<Int> {
        if (version == 1) return emptyList()
        val count = version / 7 + 2
        val last = size(version) - 7
        val step = (version * 8 + count * 3 + 5) / (count * 4 - 4) * 2
        return listOf(6) + (count - 2 downTo 0).map { last - it * step }
    }
}
