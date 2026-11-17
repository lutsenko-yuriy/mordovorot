package texts

/** What a screen reader hears (GH-166). Lines are counted from 1 here, as read. */
interface ReaderTexts {
    fun rowLeft(line: Int): String
    fun rowRight(line: Int): String
    fun columnUp(line: Int): String
    fun columnDown(line: Int): String

    /** [words] with the move count after a shift that started a move. */
    fun withMoves(words: String, moves: Int): String

    /** `Row 2 left 2`: how far a drag shifted its line. */
    fun dragged(words: String, distance: Int): String
    fun solved(moves: Int): String
    fun newGame(side: Int): String
    fun loadedSolution(name: String, side: Int, steps: Int): String
    fun loadedSolved(name: String, side: Int, moves: Int): String
    fun loadedPlain(name: String, side: Int, moves: Int): String
    fun solution(steps: Int): String
    fun stepStart(total: Int): String

    /** [words] is the step's shift, `Row 2 left`. */
    fun stepOf(current: Int, total: Int, words: String, count: Int): String

    /** `Board, step 3 of 12: …`, from [step] the step's own words. */
    fun boardStep(step: String): String
    fun withNow(words: String, tile: String): String

    /** `Row 1: 5, 2, 9.`, [tiles] already spoken and comma-joined. */
    fun boardRow(row: Int, tiles: String): String
    fun tilesInPlace(count: Int): String
    val inPlace: String
    val board: String
    val readBoard: String

    /** An alphabet hint letter the board doesn't use (GH-332): [letter] as already spoken, `zulu, not on this board`. */
    fun notOnBoard(letter: String): String

    /** The letters A to Z as said, so `B` and `D` don't sound alike. */
    val phonetic: List<String>

    /** The 28 Cyrillic letters as said (GH-313). */
    val cyrillicPhonetic: List<String>

    /** The 25 Greek labels as said, in the order of `TileLabels.GREEK_ALPHABET`: the 24 letters' names, then the question mark (GH-322). */
    val greekPhonetic: List<String>
}
