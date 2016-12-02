package board_model

public class Board constructor(override val SQUARE_SIDE: Int = 4) : BoardModel {

    var counter: Int = 0

    var boardArray: IntArray =
            IntArray(SQUARE_SIDE * SQUARE_SIDE, { item -> item })

    init {
        resetGame()
    }

    override fun resetGame() {
        boardArray =
                IntArray(SQUARE_SIDE * SQUARE_SIDE, { it })
        counter = 0

        boardArray.shuffle()
    }

    override fun shiftLeft(row: Int) {

    }

    override fun shiftRight(row: Int) {

    }

    override fun shiftUp(col: Int) {

    }

    override fun shiftDown(col: Int) {

    }

    override fun isCorrect(): Boolean {

        return (1..boardArray.size - 1).none { boardArray[it - 1] > boardArray[it] }
    }

    override fun boardState(): IntArray {
        return boardArray
    }
}