package testing

import presenter.Presenter

/**
 * A [Presenter] test double that records every call it receives. Used to
 * verify [view.ViewImpl] parses console input and delegates correctly,
 * without depending on real presenter/board logic.
 */
class FakePresenter : Presenter {

    val calls = mutableListOf<String>()

    override fun play() {
        calls.add("play")
    }

    override fun shiftLeft(row: Int) {
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

    override fun resetGame() {
        calls.add("resetGame")
    }
}
