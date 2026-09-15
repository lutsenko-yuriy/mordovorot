package board_model

/**
 * board_model.BoardModel for the Mordovorot game
 */
public interface BoardModel {

    val squareSide: Int
    val boardArray: IntArray

    // Create new game
    fun resetGame()

    /** Starts a fresh shuffled game at [side] - the *only* way the board's side ever changes
     *  (GH-44). [side] must be a [BoardSize.isValid] value; an invalid one throws
     *  [IllegalArgumentException] and leaves the board untouched. Unlike [resetGame], which
     *  reshuffles at the current size. */
    fun newGame(side: Int)

    // Replace the board with a previously saved arrangement
    fun restoreState(state: IntArray)

    // Moving tiles across the board
    fun shiftLeft(row: Int)
    fun shiftRight(row: Int)

    fun shiftUp(col: Int)
    fun shiftDown(col: Int)

    // Checking for correctness of board
    fun isCorrect(): Boolean
}