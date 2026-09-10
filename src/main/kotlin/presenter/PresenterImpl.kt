package presenter

import board_model.BoardImpl
import board_model.BoardModel
import view.View

class PresenterImpl(var view: View, var board: BoardModel = BoardImpl()) : Presenter {

    override fun shiftLeft(row: Int) = board.shiftLeft(row)

    override fun shiftRight(row: Int) = board.shiftRight(row)

    override fun shiftUp(col: Int) = board.shiftUp(col)

    override fun shiftDown(col: Int) = board.shiftDown(col)

    override fun resetGame() = board.resetGame()

    override fun play() {
        while (!board.isCorrect()) {
            try {
                view.displayBoard(board.boardArray, board.SQUARE_SIDE)
                view.processCommand()
            } catch (e: java.io.EOFException) {
                // No more input to read - stop instead of spinning on a closed stream.
                return
            } catch (e: Exception) {
                System.err.println(e.message)
            }
        }
    }
}

