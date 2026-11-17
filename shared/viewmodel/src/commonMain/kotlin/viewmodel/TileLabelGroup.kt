package viewmodel

import texts.texts

/** The alphabet a [TileLabels] scheme belongs to (GH-323): the Tile labels dialog opens one group at a
 *  time. [schemes] are in the order the dialog lists them. */
enum class TileLabelGroup(val schemes: List<TileLabels>) {
    NUMBERS(listOf(TileLabels.NUMBERS, TileLabels.PAIRS)),
    NUMBERS_FROM_0(listOf(TileLabels.NUMBERS_FROM_0, TileLabels.PAIRS_FROM_0)),
    LATIN(listOf(TileLabels.LETTERS, TileLabels.LETTER_PAIRS)),
    CYRILLIC(listOf(TileLabels.CYRILLIC_LETTERS, TileLabels.CYRILLIC_LETTER_PAIRS)),
    GREEK(listOf(TileLabels.GREEK_LETTERS, TileLabels.GREEK_LETTER_PAIRS)),
    ;

    val title: String get() = with(texts().page) {
        when (this@TileLabelGroup) {
            NUMBERS -> labelGroupNumbers
            NUMBERS_FROM_0 -> labelNumbersFrom0
            LATIN -> labelGroupLatin
            CYRILLIC -> labelGroupCyrillic
            GREEK -> labelGroupGreek
        }
    }

    /** [scheme]'s row inside this group: the group's header names the alphabet, so a row only says single or paired,
     *  `Numbers` or `Letters` and `Pairs` in every group. */
    fun rowTitle(scheme: TileLabels): String = with(texts().page) {
        when {
            scheme == schemes.last() -> labelPairs
            this@TileLabelGroup == NUMBERS || this@TileLabelGroup == NUMBERS_FROM_0 -> labelNumbers
            else -> labelRowLetters
        }
    }

    companion object {
        fun of(labels: TileLabels): TileLabelGroup = entries.first { labels in it.schemes }
    }
}
