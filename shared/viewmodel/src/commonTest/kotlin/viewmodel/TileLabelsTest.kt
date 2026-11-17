package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import storage.SaveFileCodec

class TileLabelsTest {

    private fun labels(tile: Int, side: Int) = TileLabels.entries.map { it.label(tile, side) }

    /** GH-237: the packed save format knows these names by position; an unknown name is written out in full, which is longer.
     *  GH-313: the Cyrillic names are such names, so they take the escape and still round-trip. */
    @Test
    fun `the Latin schemes have a code in the packed format, the Cyrillic ones fall into its escape`() {
        val board = intArrayOf(1, 0, 2, 3)
        val plain = SaveFileCodec.encode(2, board, 0, null).length
        val cyrillic = setOf(TileLabels.CYRILLIC_LETTERS, TileLabels.CYRILLIC_LETTER_PAIRS)

        for (scheme in TileLabels.entries) {
            val text = SaveFileCodec.encode(2, board, 0, null, scheme.wireName)
            if (scheme in cyrillic) assertTrue(text.length > plain, scheme.wireName) else assertEquals(plain, text.length, scheme.wireName)
            assertEquals(scheme.wireName, SaveFileCodec.decode("t", text).labels)
        }
    }

    @Test
    fun `tile 6 on 4x4 reads 7, 6, 23, 12, G and BC`() {
        assertEquals(listOf("7", "6", "23", "12", "G", "BC", "Ж", "БВ"), labels(6, 4))
    }

    @Test
    fun `the first tile on each size`() {
        for (side in 3..5) assertEquals(listOf("1", "0", "11", "00", "A", "AA", "А", "АА"), labels(0, side))
    }

    @Test
    fun `the middle and last tiles on 3x3`() {
        assertEquals(listOf("5", "4", "22", "11", "E", "BB", "Д", "ББ"), labels(4, 3))
        assertEquals(listOf("9", "8", "33", "22", "I", "CC", "И", "ВВ"), labels(8, 3))
    }

    @Test
    fun `the middle and last tiles on 5x5`() {
        assertEquals(listOf("13", "12", "33", "22", "M", "CC", "Н", "ВВ"), labels(12, 5))
        assertEquals(listOf("25", "24", "55", "44", "Y", "EE", "Щ", "ДД"), labels(24, 5))
    }

    @Test
    fun `the last tile on 4x4`() {
        assertEquals(listOf("16", "15", "44", "33", "P", "DD", "Р", "ГГ"), labels(15, 4))
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
            listOf("numbers", "numbers_from_0", "pairs", "pairs_from_0", "letters", "letter_pairs", "cyrillic_letters", "cyrillic_letter_pairs"),
            TileLabels.entries.map { it.wireName },
        )
        assertNull(TileLabels.fromWireName("emoji"))
        assertNull(TileLabels.fromWireName(null))
    }

    @Test
    fun `the Cyrillic schemes read the 28 letters (GH-313)`() {
        assertEquals(28, TileLabels.CYRILLIC_ALPHABET.length)
        assertEquals(listOf("А", "Б", "В", "Г"), TileLabels.CYRILLIC_LETTERS.sampleRow(4))
        assertEquals(listOf("АА", "АБ", "АВ", "АГ"), TileLabels.CYRILLIC_LETTER_PAIRS.sampleRow(4))
        assertEquals("ИИ", TileLabels.CYRILLIC_LETTER_PAIRS.label(80, 9))
        assertEquals("ИА", TileLabels.CYRILLIC_LETTER_PAIRS.label(72, 9))
    }

    @Test
    fun `the Cyrillic schemes fit up to 5x5 and up to 9x9 (GH-313)`() {
        assertTrue(TileLabels.CYRILLIC_LETTERS.fits(5))
        assertFalse(TileLabels.CYRILLIC_LETTERS.fits(6))
        assertTrue(TileLabels.CYRILLIC_LETTER_PAIRS.fits(9))
        assertFailsWith<IllegalArgumentException> { TileLabels.CYRILLIC_LETTERS.label(0, 6) }
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

    @Test
    fun `the limit text names the largest board a scheme labels (GH-160)`() {
        assertEquals("up to 5x5", TileLabels.LETTERS.limitText)
    }
}
