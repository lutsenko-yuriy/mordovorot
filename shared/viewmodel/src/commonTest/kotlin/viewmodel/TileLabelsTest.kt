package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TileLabelsTest {

    private fun labels(tile: Int, side: Int) = TileLabels.entries.map { it.label(tile, side) }

    @Test
    fun `tile 6 on 4x4 reads 7, 6, 23, 12, G and BC`() {
        assertEquals(listOf("7", "6", "23", "12", "G", "BC"), labels(6, 4))
    }

    @Test
    fun `the first tile on each size`() {
        for (side in 3..5) assertEquals(listOf("1", "0", "11", "00", "A", "AA"), labels(0, side))
    }

    @Test
    fun `the middle and last tiles on 3x3`() {
        assertEquals(listOf("5", "4", "22", "11", "E", "BB"), labels(4, 3))
        assertEquals(listOf("9", "8", "33", "22", "I", "CC"), labels(8, 3))
    }

    @Test
    fun `the middle and last tiles on 5x5`() {
        assertEquals(listOf("13", "12", "33", "22", "M", "CC"), labels(12, 5))
        assertEquals(listOf("25", "24", "55", "44", "Y", "EE"), labels(24, 5))
    }

    @Test
    fun `the last tile on 4x4`() {
        assertEquals(listOf("16", "15", "44", "33", "P", "DD"), labels(15, 4))
    }

    @Test
    fun `the sample row is the solved board's first row`() {
        assertEquals(listOf("1", "2", "3", "4"), TileLabels.NUMBERS.sampleRow(4))
        assertEquals(listOf("00", "01", "02"), TileLabels.PAIRS_FROM_0.sampleRow(3))
        assertEquals(listOf("AA", "AB", "AC", "AD", "AE"), TileLabels.LETTER_PAIRS.sampleRow(5))
    }

    @Test
    fun `wire names round-trip, and an unknown or missing one gives null`() {
        for (labels in TileLabels.entries) assertEquals(labels, TileLabels.fromWireName(labels.wireName))
        assertEquals(
            listOf("numbers", "numbers_from_0", "pairs", "pairs_from_0", "letters", "letter_pairs"),
            TileLabels.entries.map { it.wireName },
        )
        assertNull(TileLabels.fromWireName("emoji"))
        assertNull(TileLabels.fromWireName(null))
    }

    @Test
    fun `numbers are the default`() {
        assertEquals(TileLabels.NUMBERS, TileLabels.DEFAULT)
    }

    @Test
    fun `a label past the scheme's side limit throws`() {
        assertFailsWith<IllegalArgumentException> { TileLabels.LETTERS.label(0, 6) }
        assertFailsWith<IllegalArgumentException> { TileLabels.PAIRS.label(0, 10) }
        assertEquals("100", TileLabels.NUMBERS.label(99, 10))
    }

    @Test
    fun `fits says whether a scheme labels a side (GH-160)`() {
        assertTrue(TileLabels.LETTERS.fits(5))
        assertFalse(TileLabels.LETTERS.fits(6))
        assertTrue(TileLabels.LETTER_PAIRS.fits(6))
        assertTrue(TileLabels.NUMBERS.fits(6))
    }
}
