package testing

import view.View

/**
 * A [View] test double for exercising [presenter.PresenterImpl.play] without any
 * console I/O. Each call to [processCommand] pops and runs the next scripted
 * action from [commands] - typically a lambda that mutates a [FakeBoardModel]
 * (e.g. flips `correct = true`) or throws, to control loop termination and
 * exception handling. Running out of scripted commands throws, which bounds
 * every test to a finite number of play() iterations instead of risking a hang.
 */
class FakeView(private val commands: MutableList<() -> Unit> = mutableListOf()) : View {

    val displayBoardCalls = mutableListOf<Pair<IntArray, Int>>()

    var processCommandCallCount = 0
        private set

    override fun displayBoard(boardState: IntArray, squareSide: Int) {
        displayBoardCalls.add(boardState.copyOf() to squareSide)
    }

    override fun processCommand() {
        processCommandCallCount++
        if (commands.isEmpty()) {
            throw IllegalStateException("FakeView ran out of scripted commands")
        }
        commands.removeAt(0).invoke()
    }

    override fun play() {
        throw UnsupportedOperationException("FakeView.play() is not used by these tests")
    }
}
