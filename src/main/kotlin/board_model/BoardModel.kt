package board_model

/**
 * board_model.BoardModel for the Mordovorot game
 */
public interface BoardModel {

    val squareSide: Int
    val boardArray: IntArray

    /** Moves made this game (GH-62): a move is a run of consecutive shifts on one row or column.
     *  Zeroed by [resetGame]/[newGame]; set by [restoreState], after which the next shift always
     *  starts a new move. */
    val moveCount: Int

    /** Starts a fresh shuffled game at the current size; the shuffle is always solvable
     *  ([Solvability], GH-74). */
    fun resetGame()

    /** Starts a fresh, always-solvable shuffled game at [side] - one of the two ways the board's side ever changes,
     *  the other being [restoreState] (GH-44 WU5). [side] must be a [BoardSize.isValid] value; an
     *  invalid one throws [IllegalArgumentException] and leaves the board untouched. Unlike
     *  [resetGame], which reshuffles at the current size. */
    fun newGame(side: Int)

    /** Replaces the board with a previously saved arrangement, resizing to [state]'s own square
     *  root if it differs from the board's current side (GH-44 WU5) - a valid save is whatever
     *  size it says it is. The derived side must still be a [BoardSize.isValid] value, same as
     *  [newGame]; an invalid one throws [IllegalArgumentException] and leaves the board untouched.
     *  [moveCount] must be non-negative, same rule. */
    fun restoreState(state: IntArray, moveCount: Int = 0)

    // Moving tiles across the board
    fun shiftLeft(row: Int)
    fun shiftRight(row: Int)

    fun shiftUp(col: Int)
    fun shiftDown(col: Int)

    // Checking for correctness of board
    fun isCorrect(): Boolean
}