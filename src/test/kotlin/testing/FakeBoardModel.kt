package testing

import board_model.BoardModel
import board_model.BoardSize

/**
 * A [BoardModel] test double that records every call it receives instead of
 * actually mutating a board. Used to verify [viewmodel.ViewModelImpl]
 * delegates correctly without depending on real board logic.
 */
class FakeBoardModel(
    initialSquareSide: Int = 4,
    override var boardArray: IntArray = IntArray(initialSquareSide * initialSquareSide) { it },
) : BoardModel {

    /** Settable, unlike the constructor param it starts from - [newGame] changes it (GH-44). */
    override var squareSide: Int = initialSquareSide

    val calls = mutableListOf<String>()

    /** When set, [newGame] throws this instead of recording the call - mirrors
     *  [shiftLeftException] for the same purpose. */
    var newGameException: Throwable? = null

    /** Controls [isCorrect]'s return value; flip it from a test to end a play() loop. */
    var correct = false

    /** When set, [shiftLeft] throws this instead of recording the call - used to verify
     *  ViewModelImpl propagates board exceptions unchanged. */
    var shiftLeftException: Throwable? = null

    override fun resetGame() {
        calls.add("resetGame")
    }

    override fun newGame(side: Int) {
        newGameException?.let { throw it }
        calls.add("newGame($side)")
        squareSide = side
        boardArray = IntArray(side * side) { it }
    }

    override fun restoreState(state: IntArray) {
        // Mirrors BoardImpl's own validation (audit finding on PR #46), including squareSide now
        // deriving from state's own length rather than requiring a match (GH-44 WU5) - without the
        // perfect-square check, a mis-sized fixture leaves a fake board whose array doesn't match
        // squareSide, and ViewImpl.play()'s displayBoard call throws every iteration with nothing
        // consuming input to ever reach EOF - an unbounded loop instead of a readable test failure.
        val restoredSide = Math.sqrt(state.size.toDouble()).toInt()
        require(restoredSide * restoredSide == state.size) { "Board state size must be a perfect square, got ${state.size}" }
        require(state.toSet() == (0 until state.size).toSet()) { "Board state must contain each of ${state.size} tile values exactly once" }
        BoardSize.require(restoredSide) // mirrors BoardImpl's own BoardSize check (audit finding on PR #55)
        calls.add("restoreState(${state.toList()})")
        squareSide = restoredSide
        boardArray = state
    }

    override fun shiftLeft(row: Int) {
        shiftLeftException?.let { throw it }
        calls.add("shiftLeft($row)")
    }

    override fun shiftRight(row: Int) {
        calls.add("shiftRight($row)")
    }

    override fun shiftUp(col: Int) {
        calls.add("shiftUp($col)")
    }

    override fun shiftDown(col: Int) {
        calls.add("shiftDown($col)")
    }

    override fun isCorrect(): Boolean = correct
}
