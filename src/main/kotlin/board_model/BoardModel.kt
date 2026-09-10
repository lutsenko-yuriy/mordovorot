package board_model

/**
 * board_model.BoardModel for the Mordovorot game
 */
public interface BoardModel {

    val SQUARE_SIDE: Int
    val boardArray: IntArray

    // Create new game
    fun resetGame()

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