package presenter

import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeView
import view.EndOfInputException
import kotlin.test.Test
import kotlin.test.assertEquals

class ConsolePresenterPlayTest {

    @Test
    fun `play returns immediately when the board is already correct`() {
        val board = FakeBoardModel().apply { correct = true }
        val view = FakeView()
        val presenter = ConsolePresenterImpl(view, board)

        presenter.play()

        assertEquals(0, view.displayBoardCalls.size)
        assertEquals(0, view.processCommandCallCount)
    }

    /** Console-side anchor for [BasePresenter.offerStartupRestore] - the flow's own branch
     *  matrix (0/1/2+ saves, decline, unknown name, etc.) is covered against `play()` in
     *  [ConsolePresenterStartupRestoreTest] and against [TuiPresenterImpl.restoreOnStartup]
     *  directly in [TuiPresenterStartupRestoreTest]; this test just pins that
     *  [ConsolePresenterImpl.play] still calls it as its first step (audit finding on PR #26:
     *  splitting `offerStartupRestore` off into a `protected` method meant each concrete
     *  subclass needed its own anchor, and the console side lost its coverage when every
     *  startup-restore test moved onto the TUI entry point). */
    @Test
    fun `play offers startup restore before entering the loop`() {
        val board = FakeBoardModel().apply { correct = true }
        val savedState = intArrayOf(3, 2, 1, 0)
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, savedState)))
        val view = FakeView(confirmRestoreResponses = mutableListOf(true))
        val presenter = ConsolePresenterImpl(view, board, saves)

        presenter.play()

        assertEquals(listOf("foo"), view.confirmRestoreCalls)
        assertEquals(listOf("restoreState(${savedState.toList()})"), board.calls)
    }

    @Test
    fun `play displays the board and processes one command before the board is solved`() {
        val board = FakeBoardModel()
        val view = FakeView(mutableListOf({ board.correct = true }))
        val presenter = ConsolePresenterImpl(view, board)

        presenter.play()

        assertEquals(1, view.displayBoardCalls.size)
        assertEquals(1, view.processCommandCallCount)
        assertEquals(board.SQUARE_SIDE, view.displayBoardCalls[0].second)
        assertEquals(board.boardArray.toList(), view.displayBoardCalls[0].first.toList())
    }

    @Test
    fun `play returns without looping when processCommand throws EndOfInputException`() {
        val board = FakeBoardModel()
        val view = FakeView(mutableListOf({ throw EndOfInputException() }))
        val presenter = ConsolePresenterImpl(view, board)

        presenter.play()

        assertEquals(1, view.displayBoardCalls.size)
        assertEquals(1, view.processCommandCallCount)
    }

    /** Guards [ConsolePresenterImpl.play]'s `catch (e: ExitRequestedException) { return }` -
     *  the only path that actually exercised it was `exitGame()` running inside `play()`, which
     *  moved to [BasePresenterExitTest] with the split (audit finding on PR #27: the split left
     *  this catch unexercised, since [BasePresenterExitTest] asserts the throw against
     *  `TestPresenter` directly, with no loop to catch it). */
    @Test
    fun `play returns when exitGame requests an exit`() {
        val board = FakeBoardModel()
        lateinit var presenter: ConsolePresenterImpl
        val view = FakeView(
            commands = mutableListOf({ presenter.exitGame() }),
            confirmSaveBeforeExitResponses = mutableListOf(false),
        )
        presenter = ConsolePresenterImpl(view, board)

        presenter.play()

        assertEquals(1, view.processCommandCallCount)
    }

    @Test
    fun `play swallows processCommand exceptions and keeps looping`() {
        val board = FakeBoardModel()
        val view = FakeView(
            mutableListOf(
                { throw RuntimeException("boom") },
                { board.correct = true },
            )
        )
        val presenter = ConsolePresenterImpl(view, board)

        presenter.play()

        assertEquals(2, view.displayBoardCalls.size)
        assertEquals(2, view.processCommandCallCount)
    }

    @Test
    fun `play routes a swallowed exception's message through view showMessage, not System-err`() {
        val board = FakeBoardModel()
        val view = FakeView(
            mutableListOf(
                { throw IllegalArgumentException("bad command") },
                { board.correct = true },
            )
        )
        val presenter = ConsolePresenterImpl(view, board)

        presenter.play()

        assertEquals(listOf("bad command"), view.shownMessages)
    }

    /** Play-loop-specific half of `exit, save confirmed but the save itself fails` - moved here
     *  (GH-23 WU2) because "does not quit" is a [play] loop-continuation assertion, not a
     *  [BasePresenter.exitGame] behaviour; that method's own message/analytics contract is
     *  covered directly, without a play() loop, in [BasePresenterExitTest]. */
    @Test
    fun `a failed save during exit does not quit - play keeps looping`() {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        saves.saveException = RuntimeException("disk full")
        lateinit var presenter: ConsolePresenterImpl
        val view = FakeView(
            commands = mutableListOf({ presenter.exitGame() }, { board.correct = true }),
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("foo"),
        )
        presenter = ConsolePresenterImpl(view, board, saves)

        presenter.play()

        assertEquals(2, view.processCommandCallCount)
    }
}
