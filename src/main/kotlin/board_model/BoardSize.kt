package board_model

/** The single home for the valid board-side range (GH-44). `BoardImpl.newGame`/the constructor
 *  validate against this; `Main.kt`'s `--size=N` parsing and every UI-side size prompt report
 *  the same range back to the user. */
object BoardSize {
    const val MIN: Int = 3
    const val MAX: Int = 5
    const val DEFAULT: Int = 4

    fun isValid(side: Int): Boolean = side in MIN..MAX

    /** Returns [side] unchanged if valid, otherwise throws [IllegalArgumentException] naming the
     *  valid range. */
    fun require(side: Int): Int {
        require(isValid(side)) { "Board size must be between $MIN and $MAX, got $side" }
        return side
    }
}
