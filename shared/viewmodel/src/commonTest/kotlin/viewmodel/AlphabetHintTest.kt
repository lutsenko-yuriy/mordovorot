package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** GH-332: what the alphabet hint shows - the one place that decides it. */
class AlphabetHintTest {

    @Test
    fun `a Latin board shows the 26 letters and the ones it uses`() {
        val hint = AlphabetHint.of(TileLabels.LETTERS, 4)!!

        assertEquals(TileLabelGroup.LATIN, hint.group)
        assertEquals(26, hint.letters.size)
        assertEquals("A", hint.letters.first())
        assertEquals("Z", hint.letters.last())
        assertEquals(16, hint.used)
    }

    @Test
    fun `a paired scheme uses as many letters as the board's side`() {
        val hint = AlphabetHint.of(TileLabels.CYRILLIC_LETTER_PAIRS, 5)!!

        assertEquals(TileLabelGroup.CYRILLIC, hint.group)
        assertEquals(28, hint.letters.size)
        assertEquals(5, hint.used)
    }

    @Test
    fun `Greek on 5x5 uses all 25 labels, the question mark included`() {
        val hint = AlphabetHint.of(TileLabels.GREEK_LETTERS, 5)!!

        assertEquals(25, hint.letters.size)
        assertEquals(25, hint.used)
        assertEquals(0x37E, hint.letters.last().single().code)
    }

    @Test
    fun `Greek pairs keep the question mark unused`() {
        val hint = AlphabetHint.of(TileLabels.GREEK_LETTER_PAIRS, 5)!!

        assertEquals(5, hint.used)
        assertEquals(25, hint.letters.size)
    }

    @Test
    fun `the number schemes have no hint`() {
        for (scheme in listOf(TileLabels.NUMBERS, TileLabels.NUMBERS_FROM_0, TileLabels.PAIRS, TileLabels.PAIRS_FROM_0)) {
            assertNull(AlphabetHint.of(scheme, 4), scheme.wireName)
        }
    }
}
