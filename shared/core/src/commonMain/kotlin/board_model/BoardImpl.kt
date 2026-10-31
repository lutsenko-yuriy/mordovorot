package board_model

import kotlin.math.sqrt

/** [maxSide] is the largest side this board accepts; only the web client raises it (GH-207). */
public class BoardImpl(initialSide: Int = BoardSize.DEFAULT, private val maxSide: Int = BoardSize.MAX) : BoardModel {

    private var side: Int = BoardSize.require(initialSide, maxSide)
    override val squareSide: Int get() = side

    override var moveCount: Int = 0
        private set

    /** One row or column - shifts in either direction along it extend the same move. */
    private data class Line(val isRow: Boolean, val index: Int)

    /** Line of the last shift; null when the next shift must start a new move. */
    private var lastLine: Line? = null

    override var boardArray: IntArray =
            IntArray(squareSide * squareSide, { item -> item })

    init {
        resetGame()
    }

    override fun resetGame() {
        boardArray =
                IntArray(squareSide * squareSide, { it })
        moveCount = 0
        lastLine = null

        boardArray.shuffle()
        // Swapping 0/1 maps odd permutations one-to-one onto even ones, so the result stays
        // uniform over solvable boards (GH-74).
        if (!Solvability.isSolvable(boardArray)) {
            boardArray[0] = boardArray[1].also { boardArray[1] = boardArray[0] }
        }
    }

    override fun newGame(side: Int) {
        BoardSize.require(side, maxSide)
        this.side = side
        resetGame()
    }

    override fun restoreState(state: IntArray, moveCount: Int) {
        // squareSide comes from state's own length, not the board's current side (GH-44 WU5) -
        // a valid save is whatever size it says it is; loading one resizes the board to match
        // instead of requiring a coincidental match to what the board happened to be at.
        val restoredSide = sqrt(state.size.toDouble()).toInt()
        require(restoredSide * restoredSide == state.size) { "Board state size must be a perfect square, got ${state.size}" }
        require(state.toSet() == (0 until state.size).toSet()) { "Board state must contain each of ${state.size} tile values exactly once" }
        // BoardSize.require, same as newGame (audit finding on PR #55) - without it, a
        // hand-edited/corrupted save with a squareSide outside BoardSize (the file store only
        // rejects <= 0) would resize the live board past what every other size-changing surface
        // enforces, e.g. a 1x1 board that's permanently solved or a board too wide to render.
        BoardSize.require(restoredSide, maxSide)
        require(moveCount >= 0) { "Move count must not be negative, got $moveCount" }

        side = restoredSide
        boardArray = state.copyOf()
        this.moveCount = moveCount
        lastLine = null
    }

    override fun shiftLeft(row: Int) {
        if (row !in 0 until squareSide) {
            throw IllegalArgumentException("Incorrect row")
        }
        countMove(Line(isRow = true, index = row))

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
        countMove(Line(isRow = true, index = row))

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
        countMove(Line(isRow = false, index = col))

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
        countMove(Line(isRow = false, index = col))

        val t = boardArray[col + (squareSide - 1) * squareSide]
        for (i in squareSide - 2 downTo 0) {
            boardArray[col + (i + 1) * squareSide] = boardArray[col + i * squareSide]
        }
        boardArray[col] = t
    }

    private fun countMove(line: Line) {
        if (line != lastLine) moveCount++
        lastLine = line
    }

    override fun isCorrect(): Boolean {
        return (1..boardArray.size - 1).none { boardArray[it - 1] > boardArray[it] }
    }
}