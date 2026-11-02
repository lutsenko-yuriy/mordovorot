package storage

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** GH-237: the bit stream a packed save is made of. */
class SaveBitsTest {

    private fun reader(build: BitWriter.() -> Unit) = BitReader(BitWriter().apply(build).toByteArray())

    @Test
    fun `bitsFor is the bits to tell count values apart`() {
        assertEquals(1, bitsFor(2))
        assertEquals(2, bitsFor(3))
        assertEquals(2, bitsFor(4))
        assertEquals(3, bitsFor(5))
        assertEquals(3, bitsFor(8))
        assertEquals(4, bitsFor(9))
        assertEquals(7, bitsFor(81))
        assertEquals(8, bitsFor(144))
        assertEquals(0, bitsFor(1))
    }

    @Test
    fun `fixed width fields round-trip across byte boundaries`() {
        val r = reader { put(5, 3); put(0, 1); put(1, 1); put(200, 8); put(3, 2); put(0, 0); put(1023, 10) }

        assertEquals(5, r.get(3))
        assertEquals(0, r.get(1))
        assertEquals(1, r.get(1))
        assertEquals(200, r.get(8))
        assertEquals(3, r.get(2))
        assertEquals(0, r.get(0))
        assertEquals(1023, r.get(10))
        assertTrue(r.atEnd())
    }

    @Test
    fun `fields are written most significant bit first and padded with zeros`() {
        val bytes = BitWriter().apply { put(2, 4); put(1, 1) }.toByteArray()

        assertContentEquals(byteArrayOf(0b0010_1000), bytes)
    }

    @Test
    fun `varints round-trip, small ones in one byte`() {
        for (v in listOf(0, 1, 127, 128, 300, 16384, 2_097_152, Int.MAX_VALUE)) {
            assertEquals(v, reader { putVarint(v) }.getVarint(), "$v")
        }
        assertEquals(1, BitWriter().apply { putVarint(127) }.toByteArray().size)
        assertEquals(2, BitWriter().apply { putVarint(128) }.toByteArray().size)
        assertEquals(5, BitWriter().apply { putVarint(Int.MAX_VALUE) }.toByteArray().size)
    }

    @Test
    fun `a varint above Int MAX_VALUE or with six groups is rejected`() {
        val tooBig = BitReader(byteArrayOf(-1, -1, -1, -1, 0x0F))
        assertFailsWith<BitFormatException> { tooBig.getVarint() }

        val sixGroups = BitReader(byteArrayOf(-128, -128, -128, -128, -128, 0))
        assertFailsWith<BitFormatException> { sixGroups.getVarint() }
    }

    @Test
    fun `reading past the end throws`() {
        val r = BitReader(byteArrayOf(0x7F))

        r.get(8)
        assertFailsWith<BitFormatException> { r.get(1) }
        assertFailsWith<BitFormatException> { BitReader(byteArrayOf()).getVarint() }
    }

    @Test
    fun `atEnd is true only when under a byte of zero bits is left`() {
        val r = BitReader(byteArrayOf(0b1010_0000.toByte()))
        r.get(3)
        assertTrue(r.atEnd())

        val dirty = BitReader(byteArrayOf(0b1010_0001.toByte()))
        dirty.get(3)
        assertFalse(dirty.atEnd())

        val extra = BitReader(byteArrayOf(0b1010_0000.toByte(), 0))
        extra.get(3)
        assertFalse(extra.atEnd())
    }

    @Test
    fun `a permutation round-trips, in fewer bits than a plain tile list`() {
        val random = Random(237)
        for (n in listOf(4, 9, 16, 25, 36, 49, 64, 81)) {
            val perm = IntArray(n) { it }.also { it.shuffle(random) }
            for (p in listOf(IntArray(n) { it }, IntArray(n) { n - 1 - it }, perm)) {
                val bytes = BitWriter().apply { putPermutation(p) }.toByteArray()
                assertContentEquals(p, BitReader(bytes).getPermutation(n), "n=$n")
                assertTrue(bytes.size * 8 < n * bitsFor(n) + 8 || n < 9, "n=$n takes ${bytes.size} bytes")
            }
        }
    }

    @Test
    fun `a Lehmer digit out of its range is rejected`() {
        // Three tiles: the first digit has 2 bits and range 0-2, so 3 (binary 11) is out of range.
        assertFailsWith<BitFormatException> { BitReader(byteArrayOf(0b1100_0000.toByte())).getPermutation(3) }
    }

    @Test
    fun `the last tile of a permutation costs no bits`() {
        // [2, 0, 3, 1]: digits 2 (2 bits), 0 (2 bits), 1 (1 bit), 0 (0 bits) = 10 00 1 -> 0b1000_1000.
        assertContentEquals(byteArrayOf(0b1000_1000.toByte()), BitWriter().apply { putPermutation(intArrayOf(2, 0, 3, 1)) }.toByteArray())
    }
}
