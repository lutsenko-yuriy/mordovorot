package board_model

public class BoardImpl(initialSide: Int = BoardSize.DEFAULT) : BoardModel {

    private var side: Int = BoardSize.require(initialSide)
    override val squareSide: Int get() = side

    var counter: Int = 0

    override var boardArray: IntArray =
            IntArray(squareSide * squareSide, { item -> item })

    init {
        resetGame()
    }

    override fun resetGame() {
        boardArray =
                IntArray(squareSide * squareSide, { it })
        counter = 0

        boardArray.shuffle()
    }

    override fun newGame(side: Int) {
        BoardSize.require(side)
        this.side = side
        resetGame()
    }

    override fun restoreState(state: IntArray) {
        require(state.size == squareSide * squareSide) { "Expected ${squareSide * squareSide} values, got ${state.size}" }
        require(state.toSet() == (0 until state.size).toSet()) { "Board state must contain each of ${state.size} tile values exactly once" }

        boardArray = state.copyOf()
        counter = 0
    }

    override fun shiftLeft(row: Int) {
        if (row !in 0 until squareSide) {
            throw IllegalArgumentException("Incorrect row")
        }

        val t = boardArray[row * squareSide]
        for (i in 0..squareSide - 2) {
            boardArray[i + row * squareSide] = boardArray[i + 1 + row * squareSide]
        }
        boardArray[(row + 1) * squareSide - 1] = t

    }

    override fun shiftRight(row: Int) {
        if (row !in 0 until squareSide) {
            throw IllegalArgumentException("Incorrect row")
        }

        val t = boardArray[(row + 1) * squareSide - 1]
        for (i in squareSide - 2 downTo 0) {
            boardArray[i + 1 + row * squareSide] = boardArray[i + row * squareSide]
        }
        boardArray[row * squareSide] = t
    }

    override fun shiftUp(col: Int) {
        if (col !in 0 until squareSide) {
            throw IllegalArgumentException("Incorrect column")
        }

        val t = boardArray[col]
        for (i in 0..squareSide - 2) {
            boardArray[col + i * squareSide] = boardArray[col + (i + 1) * squareSide]
        }
        boardArray[col + (squareSide - 1) * squareSide] = t
    }

    override fun shiftDown(col: Int) {
        if (col !in 0 until squareSide) {
            throw IllegalArgumentException("Incorrect column")
        }

        val t = boardArray[col + (squareSide - 1) * squareSide]
        for (i in squareSide - 2 downTo 0) {
            boardArray[col + (i + 1) * squareSide] = boardArray[col + i * squareSide]
        }
        boardArray[col] = t
    }

    override fun isCorrect(): Boolean {
        return (1..boardArray.size - 1).none { boardArray[it - 1] > boardArray[it] }
    }
}