package web.qr

/** A QR code (ISO/IEC 18004) for a short text: byte mode, error correction level M, the smallest version 1 to 10
 *  that fits, the mask with the lowest penalty. Imports the standard library only, so it can move to a module of
 *  its own (GH-243). */
class QrCode private constructor(val version: Int, val mask: Int, private val modules: Array<BooleanArray>) {

    val size: Int get() = modules.size

    /** Whether the module at column [x], row [y] is dark; outside the code is light. */
    fun isDark(x: Int, y: Int): Boolean = x in 0 until size && y in 0 until size && modules[y][x]

    companion object {

        /** The most bytes of text a code holds (version 10 at M). */
        val MAX_BYTES: Int = capacity(QrTables.MAX_VERSION)

        /** The smallest version that holds [byteCount] bytes of text; throws [IllegalArgumentException] if none does. */
        fun versionFor(byteCount: Int): Int =
            (QrTables.MIN_VERSION..QrTables.MAX_VERSION).firstOrNull { byteCount <= capacity(it) }
                ?: throw IllegalArgumentException("a QR code here holds at most $MAX_BYTES bytes, got $byteCount")

        fun encode(text: String): QrCode {
            val bytes = text.encodeToByteArray()
            val version = versionFor(bytes.size)
            val codewords = interleave(version, dataCodewords(version, bytes))
            val grid = Grid(version)
            grid.drawFunctionPatterns()
            grid.drawCodewords(codewords)
            val mask = (0 until QrMasks.COUNT).minByOrNull { candidate ->
                grid.applyMask(candidate)
                QrMasks.penalty(grid.modules).also { grid.applyMask(candidate) }
            }!!
            grid.applyMask(mask)
            grid.drawFormat(QrMasks.formatBits(mask))
            if (version >= 7) grid.drawVersion(QrMasks.versionBits(version))
            return QrCode(version, mask, grid.modules)
        }

        private fun countBits(version: Int) = if (version < 10) 8 else 16

        private fun capacity(version: Int) = (QrTables.dataCodewords(version) * 8 - 4 - countBits(version)) / 8

        private fun dataCodewords(version: Int, text: ByteArray): ByteArray {
            val bits = ArrayList<Boolean>()
            fun put(value: Int, width: Int) {
                for (i in width - 1 downTo 0) bits += (value ushr i) and 1 == 1
            }
            put(0b0100, 4)
            put(text.size, countBits(version))
            for (b in text) put(b.toInt() and 0xFF, 8)
            val capacityBits = QrTables.dataCodewords(version) * 8
            put(0, minOf(4, capacityBits - bits.size))
            while (bits.size % 8 != 0) bits += false
            var pad = 0xEC
            while (bits.size < capacityBits) {
                put(pad, 8)
                pad = pad xor (0xEC xor 0x11)
            }
            return ByteArray(bits.size / 8) { i -> (0 until 8).fold(0) { acc, j -> (acc shl 1) or if (bits[i * 8 + j]) 1 else 0 }.toByte() }
        }

        /** Splits [data] into the version's blocks, adds each one's check bytes and interleaves them. */
        private fun interleave(version: Int, data: ByteArray): ByteArray {
            val blocks = QrTables.blocks(version)
            val ecc = QrTables.eccPerBlock(version)
            val shortBlocks = blocks - QrTables.codewords(version) % blocks
            val shortLength = QrTables.codewords(version) / blocks - ecc
            var offset = 0
            val dataBlocks = List(blocks) { i ->
                val length = shortLength + if (i < shortBlocks) 0 else 1
                data.copyOfRange(offset, offset + length).also { offset += length }
            }
            val checks = dataBlocks.map { ReedSolomon.remainder(it, ecc) }
            val out = ArrayList<Byte>()
            for (i in 0..shortLength) for (block in dataBlocks) if (i < block.size) out += block[i]
            for (i in 0 until ecc) for (check in checks) out += check[i]
            return out.toByteArray()
        }
    }

    /** The modules under construction, with which of them are function patterns. */
    private class Grid(val version: Int) {
        val size = QrTables.size(version)
        val modules = Array(size) { BooleanArray(size) }
        private val isFunction = Array(size) { BooleanArray(size) }

        private fun set(x: Int, y: Int, dark: Boolean) {
            modules[y][x] = dark
            isFunction[y][x] = true
        }

        fun drawFunctionPatterns() {
            for (i in 0 until size) {
                set(6, i, i % 2 == 0)
                set(i, 6, i % 2 == 0)
            }
            drawFinder(3, 3)
            drawFinder(size - 4, 3)
            drawFinder(3, size - 4)
            val centres = QrTables.alignmentCentres(version)
            for (cy in centres) for (cx in centres) {
                val onFinder = (cx == 6 && cy == 6) || (cx == 6 && cy == size - 7) || (cx == size - 7 && cy == 6)
                if (!onFinder) drawAlignment(cx, cy)
            }
            // The format and version words come after the mask is chosen: the penalty scores the modules without them.
            drawFormat(0)
            if (version >= 7) drawVersion(0)
        }

        private fun drawFinder(cx: Int, cy: Int) {
            for (dy in -4..4) for (dx in -4..4) {
                val x = cx + dx
                val y = cy + dy
                if (x in 0 until size && y in 0 until size) {
                    val distance = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy))
                    set(x, y, distance != 2 && distance != 4)
                }
            }
        }

        private fun drawAlignment(cx: Int, cy: Int) {
            for (dy in -2..2) for (dx in -2..2) set(cx + dx, cy + dy, maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) != 1)
        }

        fun drawFormat(bits: Int) {
            fun bit(i: Int) = (bits ushr i) and 1 == 1
            for (i in 0..5) set(8, i, bit(i))
            set(8, 7, bit(6))
            set(8, 8, bit(7))
            set(7, 8, bit(8))
            for (i in 9..14) set(14 - i, 8, bit(i))
            for (i in 0..7) set(size - 1 - i, 8, bit(i))
            for (i in 8..14) set(8, size - 15 + i, bit(i))
            set(8, size - 8, true) // the always-dark module
        }

        fun drawVersion(bits: Int) {
            for (i in 0 until 18) {
                val dark = (bits ushr i) and 1 == 1
                val a = size - 11 + i % 3
                val b = i / 3
                set(a, b, dark)
                set(b, a, dark)
            }
        }

        /** Fills the modules that are not function patterns, two columns at a time from the bottom right, zigzagging up and down. */
        fun drawCodewords(codewords: ByteArray) {
            var i = 0
            var right = size - 1
            while (right >= 1) {
                if (right == 6) right = 5
                for (vertical in 0 until size) {
                    for (j in 0..1) {
                        val x = right - j
                        val upward = (right + 1) and 2 == 0
                        val y = if (upward) size - 1 - vertical else vertical
                        if (!isFunction[y][x] && i < codewords.size * 8) {
                            modules[y][x] = (codewords[i ushr 3].toInt() ushr (7 - (i and 7))) and 1 == 1
                            i++
                        }
                    }
                }
                right -= 2
            }
        }

        /** Flips the data modules [mask] selects; applying it twice undoes it. */
        fun applyMask(mask: Int) {
            for (y in 0 until size) for (x in 0 until size) {
                if (!isFunction[y][x] && QrMasks.flips(mask, x, y)) modules[y][x] = !modules[y][x]
            }
        }
    }
}
