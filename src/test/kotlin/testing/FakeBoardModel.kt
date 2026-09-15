package testing

import board_model.BoardModel

/**
 * A [BoardModel] test double that records every call it receives instead of
 * actually mutating a board. Used to verify [viewmodel.ViewModelImpl]
 * delegates correctly without depending on real board logic.
 */
class FakeBoardModel(
    override val SQUARE_SIDE: Int = 4,
    override var boardArray: IntArray = IntArray(SQUARE_SIDE * SQUARE_SIDE) { it },
) : BoardModel {

    val calls = mutableListOf<String>()

    /** Controls [isCorrect]'s return value; flip it from a test to end a play() loop. */
    var correct = false

    /** When set, [shiftLeft] throws this instead of recording the call - used to verify
     *  ViewModelImpl propagates board exceptions unchanged. */
    var shiftLeftException: Throwable? = null

    override fun resetGame() {
        calls.add("resetGame")
    }

    override fun restoreState(state: IntArray) {
        // Mirrors BoardImpl's own validation (audit finding on PR #46) - without it, a
        // mis-sized fixture leaves a fake board whose array doesn't match SQUARE_SIDE, and
        // ViewImpl.play()'s displayBoard call throws every iteration with nothing consuming
        // input to ever reach EOF - an unbounded loop instead of a readable test failure.
        require(state.size == SQUARE_SIDE * SQUARE_SIDE) { "Expected ${SQUARE_SIDE * SQUARE_SIDE} values, got ${state.size}" }
        require(state.toSet() == (0 until state.size).toSet()) { "Board state must contain each of ${state.size} tile values exactly once" }
        calls.add("restoreState(${state.toList()})")
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
