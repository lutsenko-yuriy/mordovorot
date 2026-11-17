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
        assertEquals(TileLabelGroup.NUMBERS, TileLabelGroup.of(TileLabels.PAIRS_FROM_0))
        assertEquals(TileLabelGroup.LATIN, TileLabelGroup.of(TileLabels.LETTER_PAIRS))
        assertEquals(TileLabelGroup.CYRILLIC, TileLabelGroup.of(TileLabels.CYRILLIC_LETTERS))
    }

    @Test
    fun `numbers list the 1-based schemes before the 0-based ones`() {
        assertEquals(
            listOf(TileLabels.NUMBERS, TileLabels.PAIRS, TileLabels.NUMBERS_FROM_0, TileLabels.PAIRS_FROM_0),
            TileLabelGroup.NUMBERS.schemes,
        )
    }
}
