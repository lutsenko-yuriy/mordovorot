package viewmodel

import texts.texts

/** The alphabet a [TileLabels] scheme belongs to (GH-323): the Tile labels dialog opens one group at a
 *  time. [schemes] are in the order the dialog lists them. */
enum class TileLabelGroup(val schemes: List<TileLabels>) {
    NUMBERS(listOf(TileLabels.NUMBERS, TileLabels.PAIRS, TileLabels.NUMBERS_FROM_0, TileLabels.PAIRS_FROM_0)),
    LATIN(listOf(TileLabels.LETTERS, TileLabels.LETTER_PAIRS)),
    CYRILLIC(listOf(TileLabels.CYRILLIC_LETTERS, TileLabels.CYRILLIC_LETTER_PAIRS)),
    ;

    val title: String get() = with(texts().page) {
        when (this@TileLabelGroup) {
            NUMBERS -> labelGroupNumbers
            LATIN -> labelGroupLatin
            CYRILLIC -> labelGroupCyrillic
        }
    }

    companion object {
        fun of(labels: TileLabels): TileLabelGroup = entries.first { labels in it.schemes }
    }
}
