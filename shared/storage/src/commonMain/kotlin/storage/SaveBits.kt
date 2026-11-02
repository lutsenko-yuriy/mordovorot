package storage

/** What a packed save's bit stream can't be read as (GH-237); `PackedSaveFormat` turns it into a [SaveFileFormatException]. */
internal class BitFormatException(message: String) : RuntimeException(message)

/** The bits to tell [count] values apart: 0 for one value, 1 for two, 2 for three or four, 3 for up to eight... */
internal fun bitsFor(count: Int): Int = if (count <= 1) 0 else 32 - (count - 1).countLeadingZeroBits()

/** Writes fields most significant bit first, to a growing byte array padded with zero bits (GH-237). All the
 *  arithmetic stays in `Int`, which Kotlin/JS emulates as 32 bits. */
internal class BitWriter {

    private var bytes = ByteArray(64)
    private var bitCount = 0

    /** [value] in [width] bits (up to 16). */
    fun put(value: Int, width: Int) {
        for (i in width - 1 downTo 0) {
            if (bitCount / 8 == bytes.size) bytes = bytes.copyOf(bytes.size * 2)
            if ((value ushr i) and 1 == 1) bytes[bitCount / 8] = (bytes[bitCount / 8].toInt() or (0x80 ushr (bitCount % 8))).toByte()
            bitCount++
        }
    }

    /** A non-negative number, 7 bits at a time and a continuation bit: 1 byte up to 127, 5 up to [Int.MAX_VALUE]. */
    fun putVarint(value: Int) {
        var rest = value
        while (rest >= 0x80) {
            put((rest and 0x7F) or 0x80, 8)
            rest = rest ushr 7
        }
        put(rest, 8)
    }

    /** [permutation] of 0 until n as its Lehmer code: for each position, how many unused values are smaller, in
     *  the bits its range (n - position values) takes, so the last tile costs none. */
    fun putPermutation(permutation: IntArray) {
        val n = permutation.size
        val used = BooleanArray(n)
        for (i in 0 until n) {
            val value = permutation[i]
            var smaller = 0
            for (v in 0 until value) if (!used[v]) smaller++
            used[value] = true
            put(smaller, bitsFor(n - i))
        }
    }

    fun toByteArray(): ByteArray = bytes.copyOf((bitCount + 7) / 8)
}

/** Reads what [BitWriter] wrote; throws [BitFormatException] for a stream that doesn't make sense (GH-237). */
internal class BitReader(private val bytes: ByteArray) {

    private var position = 0

    fun get(width: Int): Int {
        if (position + width > bytes.size * 8) throw BitFormatException("the packed data ends early")
        var value = 0
        repeat(width) {
            value = (value shl 1) or ((bytes[position / 8].toInt() ushr (7 - position % 8)) and 1)
            position++
        }
        return value
    }

    fun getVarint(): Int {
        var value = 0
        for (group in 0 until MAX_VARINT_GROUPS) {
            val byte = get(8)
            val bits = byte and 0x7F
            // The fifth group has room for 3 bits only: more would not fit an Int.
            if (group == MAX_VARINT_GROUPS - 1 && (bits > 0x07 || byte and 0x80 != 0)) throw BitFormatException("a number in the packed data is too big")
            value = value or (bits shl (7 * group))
            if (byte and 0x80 == 0) return value
        }
        throw BitFormatException("a number in the packed data is too big")
    }

    /** A permutation of 0 until [n]; a digit above its range is [BitFormatException]. */
    fun getPermutation(n: Int): IntArray {
        val free = MutableList(n) { it }
        return IntArray(n) { i ->
            val digit = get(bitsFor(n - i))
            if (digit >= n - i) throw BitFormatException("an arrangement in the packed data is not valid")
            free.removeAt(digit)
        }
    }

    /** Whether all that is left is under a byte of zero padding. */
    fun atEnd(): Boolean {
        val left = bytes.size * 8 - position
        if (left >= 8) return false
        return left == 0 || (bytes.last().toInt() and ((1 shl left) - 1)) == 0
    }

    private companion object {
        const val MAX_VARINT_GROUPS = 5
    }
}
