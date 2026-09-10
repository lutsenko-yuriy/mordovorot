package presenter

import board_model.BoardImpl
import view.View

import view.ViewImpl

class PresenterImpl(var view: View) : Presenter {

    var board = BoardImpl()

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
            } catch (e: Exception) {
                System.err.println(e.message)
            }
        }
    }
}

