package web.qr

/** The eight masks, their scoring and the BCH-protected format and version words (ISO/IEC 18004). */
internal object QrMasks {
    const val COUNT = 8

    /** Whether mask [mask] flips the module at column [x], row [y]. */
    fun flips(mask: Int, x: Int, y: Int): Boolean = when (mask) {
        0 -> (x + y) % 2 == 0
        1 -> y % 2 == 0
        2 -> x % 3 == 0
        3 -> (x + y) % 3 == 0
        4 -> (x / 3 + y / 2) % 2 == 0
        5 -> x * y % 2 + x * y % 3 == 0
        6 -> (x * y % 2 + x * y % 3) % 2 == 0
        else -> ((x + y) % 2 + x * y % 3) % 2 == 0
    }

    /** The 15 format bits for level M and [mask]: 2 + 3 data bits, 10 BCH bits, xor 0x5412. */
    fun formatBits(mask: Int): Int {
        val data = (0 shl 3) or mask // level M is 00
        var remainder = data
        repeat(10) { remainder = (remainder shl 1) xor ((remainder ushr 9) * 0x537) }
        return ((data shl 10) or remainder) xor 0x5412
    }

    /** The 18 version bits (versions 7 and up): 6 data bits, 12 BCH bits. */
    fun versionBits(version: Int): Int {
        var remainder = version
        repeat(12) { remainder = (remainder shl 1) xor ((remainder ushr 11) * 0x1F25) }
        return (version shl 12) or remainder
    }

    /** The mask's penalty over [modules] (size x size, row-major; true is dark): lower scans better. */
    fun penalty(modules: Array<BooleanArray>): Int {
        val size = modules.size
        var score = 0
        // Rule 1: five or more in a row or column of one colour, 3 + (run - 5) each.
        for (transpose in listOf(false, true)) {
            for (a in 0 until size) {
                var run = 1
                for (b in 1 until size) {
                    if (at(modules, a, b, transpose) == at(modules, a, b - 1, transpose)) {
                        run++
                    } else {
                        if (run >= 5) score += run - 2
                        run = 1
                    }
                }
                if (run >= 5) score += run - 2
            }
        }
        // Rule 2: 2x2 blocks of one colour, 3 each.
        for (y in 0 until size - 1) for (x in 0 until size - 1) {
            val c = modules[y][x]
            if (modules[y][x + 1] == c && modules[y + 1][x] == c && modules[y + 1][x + 1] == c) score += 3
        }
        // Rule 3: dark-light-dark-dark-dark-light-dark with four light modules before or after, or at the edge, 40 each.
        for (transpose in listOf(false, true)) {
            for (a in 0 until size) score += finderLikeCount(BooleanArray(size) { at(modules, a, it, transpose) })
        }
        // Rule 4: 10 for each full 5% the dark share is away from half.
        val dark = modules.sumOf { row -> row.count { it } }
        score += (kotlin.math.abs(dark * 100 - size * size * 50) / (size * size * 5)) * 10
        return score
    }

    private val FINDER = booleanArrayOf(true, false, true, true, true, false, true)

    private fun finderLikeCount(line: BooleanArray): Int {
        var count = 0
        var start = 0
        while (start <= line.size - FINDER.size) {
            if (FINDER.indices.any { line[start + it] != FINDER[it] }) {
                start++
                continue
            }
            val after = start + FINDER.size
            val lightBefore = (maxOf(start - 4, 0) until start).none { line[it] }
            val lightAfter = (after until minOf(after + 4, line.size)).none { line[it] }
            if (start == 0 || start == line.size - FINDER.size || lightBefore || lightAfter) {
                count += 40
                start = after
            } else {
                start += 4
            }
        }
        return count
    }

    private fun at(modules: Array<BooleanArray>, a: Int, b: Int, transpose: Boolean) = if (transpose) modules[b][a] else modules[a][b]
}
