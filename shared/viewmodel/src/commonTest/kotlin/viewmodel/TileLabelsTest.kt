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
    fun `the Latin schemes have a code in the packed format, the Cyrillic and Greek ones fall into its escape`() {
        val board = intArrayOf(1, 0, 2, 3)
        val plain = SaveFileCodec.encode(2, board, 0, null).length
        val escaped = setOf(TileLabels.CYRILLIC_LETTERS, TileLabels.CYRILLIC_LETTER_PAIRS, TileLabels.GREEK_LETTERS, TileLabels.GREEK_LETTER_PAIRS)

        for (scheme in TileLabels.entries) {
            val text = SaveFileCodec.encode(2, board, 0, null, scheme.wireName)
            if (scheme in escaped) assertTrue(text.length > plain, scheme.wireName) else assertEquals(plain, text.length, scheme.wireName)
            assertEquals(scheme.wireName, SaveFileCodec.decode("t", text).labels)
        }
    }

    @Test
    fun `tile 6 on 4x4 reads 7, 6, 23, 12, G and BC`() {
        assertEquals(listOf("7", "6", "23", "12", "G", "BC", "Ж", "БВ", "Η", "ΒΓ"), labels(6, 4))
    }

    @Test
    fun `the first tile on each size`() {
        for (side in 3..5) assertEquals(listOf("1", "0", "11", "00", "A", "AA", "А", "АА", "Α", "ΑΑ"), labels(0, side))
    }

    @Test
    fun `the middle and last tiles on 3x3`() {
        assertEquals(listOf("5", "4", "22", "11", "E", "BB", "Д", "ББ", "Ε", "ΒΒ"), labels(4, 3))
        assertEquals(listOf("9", "8", "33", "22", "I", "CC", "И", "ВВ", "Ι", "ΓΓ"), labels(8, 3))
    }

    @Test
    fun `the middle and last tiles on 5x5`() {
        assertEquals(listOf("13", "12", "33", "22", "M", "CC", "Н", "ВВ", "Ν", "ΓΓ"), labels(12, 5))
        assertEquals(listOf("25", "24", "55", "44", "Y", "EE", "Щ", "ДД", "\u037E", "ΕΕ"), labels(24, 5))
    }

    @Test
    fun `the last tile on 4x4`() {
        assertEquals(listOf("16", "15", "44", "33", "P", "DD", "Р", "ГГ", "Π", "ΔΔ"), labels(15, 4))
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
            listOf("numbers", "numbers_from_0", "pairs", "pairs_from_0", "letters", "letter_pairs", "cyrillic_letters", "cyrillic_letter_pairs", "greek_letters", "greek_letter_pairs"),
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

    /** GH-322: U+037E is the Greek question mark; every normalisation form turns it into the semicolon, so it is pinned by code point. */
    @Test
    fun `the Greek schemes read the 24 capitals and the Greek question mark`() {
        assertEquals(25, TileLabels.GREEK_ALPHABET.length)
        assertEquals(listOf("Α", "Β", "Γ", "Δ"), TileLabels.GREEK_LETTERS.sampleRow(4))
        assertEquals(0x391, TileLabels.GREEK_ALPHABET.first().code)
        assertEquals(0x3A9, TileLabels.GREEK_ALPHABET[23].code)
        assertEquals(0x37E, TileLabels.GREEK_ALPHABET[24].code)
        assertEquals(0x37E, TileLabels.GREEK_LETTERS.label(24, 5).single().code)
        assertEquals(listOf("ΑΑ", "ΑΒ", "ΑΓ"), TileLabels.GREEK_LETTER_PAIRS.sampleRow(3))
        assertEquals("ΙΙ", TileLabels.GREEK_LETTER_PAIRS.label(80, 9))
        assertTrue(TileLabels.GREEK_LETTERS.fits(5))
        assertFalse(TileLabels.GREEK_LETTERS.fits(6))
        assertTrue(TileLabels.GREEK_LETTER_PAIRS.fits(9))
        assertFalse(TileLabels.GREEK_LETTER_PAIRS.fits(10))
        assertFailsWith<IllegalArgumentException> { TileLabels.GREEK_LETTERS.label(0, 6) }
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

    // GH-332: the alphabets behind the hint, and how many of their letters a board uses.

    @Test
    fun `the single-letter schemes label tile i with the alphabet's letter i`() {
        for (i in 0 until 25) {
            assertEquals(TileLabels.LATIN_ALPHABET[i].toString(), TileLabels.LETTERS.label(i, 5))
            assertEquals(TileLabels.CYRILLIC_ALPHABET[i].toString(), TileLabels.CYRILLIC_LETTERS.label(i, 5))
            assertEquals(TileLabels.GREEK_ALPHABET[i].toString(), TileLabels.GREEK_LETTERS.label(i, 5))
        }
    }

    @Test
    fun `the alphabets have 26, 28 and 25 letters`() {
        assertEquals(26, TileLabels.LATIN_ALPHABET.length)
        assertEquals(28, TileLabels.CYRILLIC_ALPHABET.length)
        assertEquals(25, TileLabels.GREEK_ALPHABET.length)
    }

    @Test
    fun `the Cyrillic alphabet lacks yo, short i, the signs and yery`() {
        assertTrue("ЁЙЪЫЬ".none { it in TileLabels.CYRILLIC_ALPHABET })
    }

    @Test
    fun `the Greek alphabet ends with the question mark U+037E, compared by code`() {
        assertEquals(0x37E, TileLabels.GREEK_ALPHABET.last().code)
    }

    @Test
    fun `a single-letter scheme uses a prefix of its alphabet, as many letters as the board has tiles`() {
        assertEquals(9, TileLabels.LETTERS.lettersUsed(3))
        assertEquals(25, TileLabels.LETTERS.lettersUsed(5))
        assertEquals(25, TileLabels.CYRILLIC_LETTERS.lettersUsed(5))
        assertEquals(16, TileLabels.GREEK_LETTERS.lettersUsed(4))
        assertEquals(25, TileLabels.GREEK_LETTERS.lettersUsed(5))
    }

    @Test
    fun `a pair scheme uses as many letters as the board's side`() {
        for (scheme in listOf(TileLabels.LETTER_PAIRS, TileLabels.CYRILLIC_LETTER_PAIRS, TileLabels.GREEK_LETTER_PAIRS)) {
            for (side in 3..9) assertEquals(side, scheme.lettersUsed(side), "${scheme.wireName} $side")
        }
    }

    @Test
    fun `the number schemes use no letters`() {
        for (scheme in listOf(TileLabels.NUMBERS, TileLabels.NUMBERS_FROM_0, TileLabels.PAIRS, TileLabels.PAIRS_FROM_0)) {
            assertNull(scheme.lettersUsed(4), scheme.wireName)
        }
    }

    @Test
    fun `the Greek question mark is in use only on a 5x5 single-letter board`() {
        val last = TileLabels.GREEK_ALPHABET.length
        assertTrue(TileLabels.GREEK_LETTERS.lettersUsed(5)!! >= last)
        assertTrue(TileLabels.GREEK_LETTERS.lettersUsed(4)!! < last)
        assertTrue((3..9).all { TileLabels.GREEK_LETTER_PAIRS.lettersUsed(it)!! < last })
    }
}
