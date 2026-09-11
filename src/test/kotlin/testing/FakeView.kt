package testing

import view.View

/**
 * A [View] test double for exercising [presenter.ConsolePresenterImpl.play] without any
 * console I/O. Each call to [processCommand] pops and runs the next scripted
 * action from [commands] - typically a lambda that mutates a [FakeBoardModel]
 * (e.g. flips `correct = true`) or throws, to control loop termination and
 * exception handling. Running out of scripted commands throws, which bounds
 * every test to a finite number of play() iterations instead of risking a hang.
 */
class FakeView(
    private val commands: MutableList<() -> Unit> = mutableListOf(),
    /** Scripted return values for [confirmRestore], consumed in call order. */
    private val confirmRestoreResponses: MutableList<Boolean> = mutableListOf(),
    /** Scripted return values for [chooseSaveToRestore], consumed in call order. */
    private val chooseSaveToRestoreResponses: MutableList<String?> = mutableListOf(),
    /** Scripted return values for [confirmSaveBeforeExit], consumed in call order. */
    private val confirmSaveBeforeExitResponses: MutableList<Boolean> = mutableListOf(),
    /** Scripted return values for [promptSaveName], consumed in call order. */
    private val promptSaveNameResponses: MutableList<String?> = mutableListOf(),
) : View {

    val displayBoardCalls = mutableListOf<Pair<IntArray, Int>>()

    val shownMessages = mutableListOf<String>()

    var processCommandCallCount = 0
        private set

    val confirmRestoreCalls = mutableListOf<String>()

    val chooseSaveToRestoreCalls = mutableListOf<List<String>>()

    var confirmSaveBeforeExitCallCount = 0
        private set

    var promptSaveNameCallCount = 0
        private set

    override fun displayBoard(boardState: IntArray, squareSide: Int) {
        displayBoardCalls.add(boardState.copyOf() to squareSide)
    }

    override fun showMessage(message: String) {
        shownMessages.add(message)
    }

    override fun processCommand() {
        processCommandCallCount++
        if (commands.isEmpty()) {
            throw IllegalStateException("FakeView ran out of scripted commands")
        }
        commands.removeAt(0).invoke()
    }

    override fun confirmRestore(saveName: String): Boolean {
        confirmRestoreCalls.add(saveName)
        if (confirmRestoreResponses.isEmpty()) {
            throw IllegalStateException("FakeView ran out of scripted confirmRestore responses")
        }
        return confirmRestoreResponses.removeAt(0)
    }

    override fun chooseSaveToRestore(saveNames: List<String>): String? {
        chooseSaveToRestoreCalls.add(saveNames)
        if (chooseSaveToRestoreResponses.isEmpty()) {
            throw IllegalStateException("FakeView ran out of scripted chooseSaveToRestore responses")
        }
        return chooseSaveToRestoreResponses.removeAt(0)
    }

    override fun confirmSaveBeforeExit(): Boolean {
        confirmSaveBeforeExitCallCount++
        if (confirmSaveBeforeExitResponses.isEmpty()) {
            throw IllegalStateException("FakeView ran out of scripted confirmSaveBeforeExit responses")
        }
        return confirmSaveBeforeExitResponses.removeAt(0)
    }

    override fun promptSaveName(): String? {
        promptSaveNameCallCount++
        if (promptSaveNameResponses.isEmpty()) {
            throw IllegalStateException("FakeView ran out of scripted promptSaveName responses")
        }
        return promptSaveNameResponses.removeAt(0)
    }

    override fun play() {
        throw UnsupportedOperationException("FakeView.play() is not used by these tests")
    }
}
