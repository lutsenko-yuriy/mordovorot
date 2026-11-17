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
    CYRILLIC_LETTERS("cyrillic_letters", 5),
    CYRILLIC_LETTER_PAIRS("cyrillic_letter_pairs", 9),
    GREEK_LETTERS("greek_letters", 5),
    GREEK_LETTER_PAIRS("greek_letter_pairs", 9),
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
            CYRILLIC_LETTERS -> labelCyrillicLetters
            CYRILLIC_LETTER_PAIRS -> labelCyrillicLetterPairs
            GREEK_LETTERS -> labelGreekLetters
            GREEK_LETTER_PAIRS -> labelGreekLetterPairs
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
            CYRILLIC_LETTERS -> "${CYRILLIC_ALPHABET[tile]}"
            CYRILLIC_LETTER_PAIRS -> "${CYRILLIC_ALPHABET[row]}${CYRILLIC_ALPHABET[col]}"
            GREEK_LETTERS -> "${GREEK_ALPHABET[tile]}"
            GREEK_LETTER_PAIRS -> "${GREEK_ALPHABET[row]}${GREEK_ALPHABET[col]}"
        }
    }

    /** How many of the alphabet's letters, from the start, a [side]x[side] board uses (GH-332): a pair scheme [side], a single-letter
     *  scheme as many as it has tiles, up to the alphabet's length. `null` for the number schemes, which have no alphabet. */
    fun lettersUsed(side: Int): Int? {
        val group = TileLabelGroup.of(this)
        val alphabet = group.alphabet ?: return null
        return if (this == group.schemes.last()) side else minOf(side * side, alphabet.length)
    }

    /** The solved board's first row - the labels dialog's sample. */
    fun sampleRow(side: Int): List<String> = (0 until side).map { label(it, side) }

    companion object {
        val DEFAULT = NUMBERS

        /** The 26 letters of the Latin schemes (GH-332), as the hint lists them; `label` still says `'A' + tile`, and a test pins that the two agree. */
        const val LATIN_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        /** The 28 letters the Cyrillic schemes use, without ё, й, ъ, ы, ь (GH-313): the only place the alphabet lives. */
        const val CYRILLIC_ALPHABET = "АБВГДЕЖЗИКЛМНОПРСТУФХЦЧШЩЭЮЯ"

        /** The 25 labels the Greek schemes use (GH-322): the 24 capitals, then U+037E, the Greek question mark, which looks
         *  like a semicolon but is its own character. It is written as an escape, since every Unicode normalisation turns
         *  it into the semicolon U+003B when it is typed out. Only a 5x5 board's 25th tile shows it. */
        const val GREEK_ALPHABET = "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ\u037E"

        fun fromWireName(name: String?): TileLabels? = entries.firstOrNull { it.wireName == name }
    }
}
