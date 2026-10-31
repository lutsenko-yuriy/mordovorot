package board_model

/** The single home for the valid board-side range (GH-44). `BoardImpl.newGame`/the constructor
 *  validate against this; `Main.kt`'s `--size=N` parsing and every UI-side size prompt report
 *  the same range back to the user. */
object BoardSize {
    const val MIN: Int = 3
    const val MAX: Int = 6
    const val DEFAULT: Int = 4

    /** The sizes the web client offers beyond [MAX] (GH-207); only a `BoardImpl(maxSide)` accepts them. */
    val VERY_LARGE: IntRange = 7..9

    fun isValid(side: Int, max: Int = MAX): Boolean = side in MIN..max

    /** Returns [side] unchanged if valid up to [max], otherwise throws [IllegalArgumentException]
     *  naming the valid range. */
    fun require(side: Int, max: Int = MAX): Int {
        require(isValid(side, max)) { "Board size must be between $MIN and $max, got $side" }
        return side
    }
}
