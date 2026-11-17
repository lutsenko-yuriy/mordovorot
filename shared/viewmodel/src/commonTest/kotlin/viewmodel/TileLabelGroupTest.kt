package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-323: every scheme belongs to exactly one group, and a group lists its schemes in dialog order. */
class TileLabelGroupTest {
    @Test
    fun `the groups hold every scheme once`() {
        assertEquals(TileLabels.entries.toSet(), TileLabelGroup.entries.flatMap { it.schemes }.toSet())
        assertEquals(TileLabels.entries.size, TileLabelGroup.entries.sumOf { it.schemes.size })
    }

    @Test
    fun `of finds a scheme's group`() {
        assertEquals(TileLabelGroup.NUMBERS_FROM_0, TileLabelGroup.of(TileLabels.PAIRS_FROM_0))
        assertEquals(TileLabelGroup.LATIN, TileLabelGroup.of(TileLabels.LETTER_PAIRS))
        assertEquals(TileLabelGroup.CYRILLIC, TileLabelGroup.of(TileLabels.CYRILLIC_LETTERS))
        assertEquals(TileLabelGroup.GREEK, TileLabelGroup.of(TileLabels.GREEK_LETTER_PAIRS))
    }

    @Test
    fun `every group holds a single scheme and its paired one`() {
        assertEquals(listOf(TileLabels.NUMBERS, TileLabels.PAIRS), TileLabelGroup.NUMBERS.schemes)
        assertEquals(listOf(TileLabels.NUMBERS_FROM_0, TileLabels.PAIRS_FROM_0), TileLabelGroup.NUMBERS_FROM_0.schemes)
        assertEquals(listOf(2), TileLabelGroup.entries.map { it.schemes.size }.distinct())
    }

    @Test
    fun `only the two number groups have no alphabet (GH-332)`() {
        assertEquals(
            setOf(TileLabelGroup.NUMBERS, TileLabelGroup.NUMBERS_FROM_0),
            TileLabelGroup.entries.filter { it.alphabet == null }.toSet(),
        )
        assertEquals(TileLabels.LATIN_ALPHABET, TileLabelGroup.LATIN.alphabet)
        assertEquals(TileLabels.CYRILLIC_ALPHABET, TileLabelGroup.CYRILLIC.alphabet)
        assertEquals(TileLabels.GREEK_ALPHABET, TileLabelGroup.GREEK.alphabet)
    }
}
