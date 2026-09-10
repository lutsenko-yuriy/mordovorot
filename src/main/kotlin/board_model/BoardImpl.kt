package board_model

public class BoardImpl constructor(override val SQUARE_SIDE: Int = 4) : BoardModel {

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
        if (row !in 0..SQUARE_SIDE) {
            throw IllegalArgumentException("Incorrect row")
        }

        val t = boardArray[row * SQUARE_SIDE]
        for (i in 0..SQUARE_SIDE - 2) {
            boardArray[i + row * SQUARE_SIDE] = boardArray[i + 1 + row * SQUARE_SIDE]
        }
        boardArray[(row + 1) * SQUARE_SIDE - 1] = t

    }

    override fun shiftRight(row: Int) {
        if (row !in 0..SQUARE_SIDE) {
            throw IllegalArgumentException("Incorrect row")
        }

        val t = boardArray[(row + 1) * SQUARE_SIDE - 1]
        for (i in SQUARE_SIDE - 2 downTo 0) {
            boardArray[i + 1 + row * SQUARE_SIDE] = boardArray[i + row * SQUARE_SIDE]
        }
        boardArray[row * SQUARE_SIDE] = t
    }

    override fun shiftUp(col: Int) {
        if (col !in 0..SQUARE_SIDE) {
            throw IllegalArgumentException("Incorrect column")
        }

        val t = boardArray[col]
        for (i in 0..SQUARE_SIDE - 2) {
            boardArray[col + i * SQUARE_SIDE] = boardArray[col + (i + 1) * SQUARE_SIDE]
        }
        boardArray[col + (SQUARE_SIDE - 1) * SQUARE_SIDE] = t
    }

    override fun shiftDown(col: Int) {
        if (col !in 0..SQUARE_SIDE) {
            throw IllegalArgumentException("Incorrect column")
        }

        val t = boardArray[col + (SQUARE_SIDE - 1) * SQUARE_SIDE]
        for (i in SQUARE_SIDE - 2 downTo 0) {
            boardArray[col + (i + 1) * SQUARE_SIDE] = boardArray[col + i * SQUARE_SIDE]
        }
        boardArray[col] = t
    }

    override fun isCorrect(): Boolean {
        return (1..boardArray.size - 1).none { boardArray[it - 1] > boardArray[it] }
    }
}