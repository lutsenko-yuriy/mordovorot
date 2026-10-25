package viewmodel

/** How tiles are labelled (GH-139). [wireName] is the one name for analytics, save line 8 and
 *  the stored preference; [maxSide] is the largest board a scheme can label. [NUMBERS] is the
 *  1-based display dialect of GH-10. */
enum class TileLabels(val wireName: String, val title: String, val maxSide: Int = Int.MAX_VALUE) {
    NUMBERS("numbers", "Numbers"),
    NUMBERS_FROM_0("numbers_from_0", "Numbers from 0"),
    PAIRS("pairs", "Pairs", 9),
    PAIRS_FROM_0("pairs_from_0", "Pairs from 0", 9),
    LETTERS("letters", "Letters", 5),
    LETTER_PAIRS("letter_pairs", "Letter pairs", 26),
    ;

    fun fits(side: Int): Boolean = side <= maxSide

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
