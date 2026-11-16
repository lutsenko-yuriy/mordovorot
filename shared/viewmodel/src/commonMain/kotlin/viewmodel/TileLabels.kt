package viewmodel

import texts.texts

/** How tiles are labelled (GH-139). [wireName] is the one name for analytics, a save's labels and
 *  the stored preference; the packed save format keeps a short code for each name (GH-237); [maxSide] is the largest board a scheme can label. [NUMBERS] is the
 *  1-based display dialect of GH-10. */
enum class TileLabels(val wireName: String, val maxSide: Int = Int.MAX_VALUE) {
    NUMBERS("numbers"),
    NUMBERS_FROM_0("numbers_from_0"),
    PAIRS("pairs", 9),
    PAIRS_FROM_0("pairs_from_0", 9),
    LETTERS("letters", 5),
    LETTER_PAIRS("letter_pairs", 26),
    ;

    /** The name the labels dialog shows (GH-274: from `:shared:texts`). */
    val title: String get() = with(texts().page) {
        when (this@TileLabels) {
            NUMBERS -> labelNumbers
            NUMBERS_FROM_0 -> labelNumbersFrom0
            PAIRS -> labelPairs
            PAIRS_FROM_0 -> labelPairsFrom0
            LETTERS -> labelLetters
            LETTER_PAIRS -> labelLetterPairs
        }
    }

    fun fits(side: Int): Boolean = side <= maxSide

    /** The labels dialog's sample for a board the scheme doesn't [fits] (GH-160). */
    val limitText: String get() = texts().page.labelLimit(maxSide)

    /** The label of the 0-based [tile] on a [side]x[side] board: its solved row and column for pairs. */
    fun label(tile: Int, side: Int): String {
        require(side <= maxSide) { "$title labels at most a ${maxSide}x$maxSide board" }
        val row = tile / side
        val col = tile % side
        return when (this) {
            NUMBERS -> "${tile + 1}"
            NUMBERS_FROM_0 -> "$tile"
            PAIRS -> "${row + 1}${col + 1}"
            PAIRS_FROM_0 -> "$row$col"
            LETTERS -> "${'A' + tile}"
            LETTER_PAIRS -> "${'A' + row}${'A' + col}"
        }
    }

    /** The solved board's first row - the labels dialog's sample. */
    fun sampleRow(side: Int): List<String> = (0 until side).map { label(it, side) }

    companion object {
        val DEFAULT = NUMBERS

        fun fromWireName(name: String?): TileLabels? = entries.firstOrNull { it.wireName == name }
    }
}
