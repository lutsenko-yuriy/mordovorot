package board_model

/**
 * board_model.BoardModel for the Mordovorot game
 */
public interface BoardModel {

    val squareSide: Int
    val boardArray: IntArray

    // Create new game
    fun resetGame()

    /** Starts a fresh shuffled game at [side] - one of the two ways the board's side ever changes,
     *  the other being [restoreState] (GH-44 WU5). [side] must be a [BoardSize.isValid] value; an
     *  invalid one throws [IllegalArgumentException] and leaves the board untouched. Unlike
     *  [resetGame], which reshuffles at the current size. */
    fun newGame(side: Int)

    /** Replaces the board with a previously saved arrangement, resizing to [state]'s own square
     *  root if it differs from the board's current side (GH-44 WU5) - a valid save is whatever
     *  size it says it is. The derived side must still be a [BoardSize.isValid] value, same as
     *  [newGame]; an invalid one throws [IllegalArgumentException] and leaves the board untouched. */
    fun restoreState(state: IntArray)

    // Moving tiles across the board
    fun shiftLeft(row: Int)
    fun shiftRight(row: Int)

    fun shiftUp(col: Int)
    fun shiftDown(col: Int)

    // Checking for correctness of board
    fun isCorrect(): Boolean
}