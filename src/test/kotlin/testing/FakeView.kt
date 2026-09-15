package testing

import view.View

/**
 * A [View] test double for exercising [presenter.ConsolePresenterImpl.play] without any
 * console I/O. Each call to [processCommand] pops and runs the next scripted
 * action from [commands] - typically a lambda that mutates a [FakeBoardModel]
 * (e.g. flips `correct = true`) or throws, to control loop termination and
 * exception handling. Running out of scripted commands throws, which bounds
 * every test to a finite number of play() iterations instead of risking a hang.
 *
 * Trimmed to [displayBoard]/[showMessage]/[processCommand]/[play] (GH-42 WU2) - the four
 * prompt methods it used to script (`confirmRestore`/`chooseSaveToRestore`/
 * `confirmSaveBeforeExit`/`promptSaveName`) left the [View] interface once
 * [presenter.BasePresenter] started raising [presenter.UiRequest]s for them instead of calling
 * a `View` directly; [FakePresenterUi] scripts those now.
 */
class FakeView(
    private val commands: MutableList<suspend () -> Unit> = mutableListOf(),
) : View {

    val displayBoardCalls = mutableListOf<Pair<IntArray, Int>>()

    val shownMessages = mutableListOf<String>()

    var processCommandCallCount = 0
        private set

    override fun displayBoard(boardState: IntArray, squareSide: Int) {
        displayBoardCalls.add(boardState.copyOf() to squareSide)
    }

    override suspend fun showMessage(message: String) {
        shownMessages.add(message)
    }

    override suspend fun processCommand() {
        processCommandCallCount++
        if (commands.isEmpty()) {
            throw IllegalStateException("FakeView ran out of scripted commands")
        }
        commands.removeAt(0).invoke()
    }

    override suspend fun play() {
        throw UnsupportedOperationException("FakeView.play() is not used by these tests")
    }
}
