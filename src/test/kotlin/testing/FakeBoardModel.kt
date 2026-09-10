package testing

import board_model.BoardModel

/**
 * A [BoardModel] test double that records every call it receives instead of
 * actually mutating a board. Used to verify [presenter.PresenterImpl]
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
     *  PresenterImpl propagates board exceptions unchanged. */
    var shiftLeftException: Throwable? = null

    override fun resetGame() {
        calls.add("resetGame")
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
