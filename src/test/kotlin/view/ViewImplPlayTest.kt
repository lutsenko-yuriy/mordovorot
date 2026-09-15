package view

import board_model.BoardModel
import presenter.Presenter
import presenter.PresenterImpl
import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakePresenter
import testing.FakeSaveRepository
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the console session loop that moved from `ConsolePresenterImpl.play()` into
 * [ViewImpl.play] (GH-42 WU3, hard problem 1 on the plan comment on GH-42 - a presenter-owned
 * loop that called back into the View it also raised requests on would deadlock). Driven end to
 * end over a real [ViewImpl] and a [StringReader] of scripted console input - the loop's own
 * request-handler coroutine answers `confirmSaveBeforeExit`/`promptSaveName`/etc. from the same
 * input stream, exactly as a real session would, so no separate request-draining double is
 * needed here (unlike [presenter.PresenterExitTest]'s direct `exitGame()` calls). Assertions read
 * real console output/board state instead of `FakeView` call counters - `FakePresenter` covers
 * the loop-mechanics tests that don't need a real board; [testing.FakeSaveRepository] backs the
 * two that need a real save failure.
 */
class ViewImplPlayTest {

    private fun viewWith(input: String, presenter: Presenter): Pair<ViewImpl, ByteArrayOutputStream> {
        val outputBuffer = ByteArrayOutputStream()
        val view = ViewImpl(BufferedReader(StringReader(input)), PrintStream(outputBuffer))
        view.presenter = presenter
        return view to outputBuffer
    }

    /** Wraps [board] so [BoardModel.shiftLeft] flips `isCorrect()` to `true` right after
     *  recording the shift - the fixture every test below that needs a real "solved by playing"
     *  transition shares, since [FakeBoardModel] itself has no such hook. */
    private fun solveOnShiftLeft(board: FakeBoardModel): BoardModel = object : BoardModel by board {
        override fun shiftLeft(row: Int) {
            board.shiftLeft(row)
            board.correct = true
        }
    }

    @Test
    fun `play returns immediately when the board is already correct`(): Unit = runBlocking {
        val presenter = FakePresenter().apply { solved = true }
        val (view, output) = viewWith("", presenter)

        view.play()

        assertEquals("", output.toString())
        assertFalse(presenter.calls.contains("boardState"))
    }

    /** Console-side anchor for [presenter.PresenterImpl.restoreOnStartup] - the flow's own
     *  branch matrix (0/1/2+ saves, decline, unknown name, etc.) is covered directly in
     *  [presenter.PresenterStartupRestoreTest]; this test just pins that [ViewImpl.play] still
     *  calls it as its first step, before checking whether the board is solved. */
    @Test
    fun `play offers startup restore before entering the loop`(): Unit = runBlocking {
        val presenter = FakePresenter()
        val (view, output) = viewWith("", presenter)

        view.play()

        assertEquals("restoreOnStartup", presenter.calls.first())
        assertTrue(presenter.calls.contains("isSolved"))
        assertTrue(output.toString().isNotEmpty())
    }

    /** Pins the one ordering property [ViewImpl.play]'s shape actually depends on: the
     *  `uiRequests` handler must be launched *before* `restoreOnStartup()`, not just called
     *  before the loop - `FakePresenter` can't catch this (it never raises a `UiRequest`), so
     *  this test drives a real [PresenterImpl] through the handler instead (audit finding on
     *  PR #46). [withTimeout] turns a regression here into a failure instead of a silent hang -
     *  see `build.gradle.kts`'s note on `kotlinx-coroutines-test` for why that distinction
     *  matters for this exact class of bug. */
    @Test
    fun `play answers the startup restore prompt through its own request handler`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, IntArray(16) { it })))
        val (view, output) = viewWith("y\n", PresenterImpl(board, saves))

        withTimeout(3_000) { view.play() }

        assertTrue(output.toString().contains("Restore save 'foo'?"))
        assertTrue(board.calls.any { it.startsWith("restoreState") })
    }

    @Test
    fun `play displays the board and processes one command before the board is solved`(): Unit = runBlocking {
        val fakeBoard = FakeBoardModel()
        val presenter = PresenterImpl(solveOnShiftLeft(fakeBoard))
        val (view, output) = viewWith("left 1\n", presenter)

        view.play()

        assertEquals(listOf("shiftLeft(0)"), fakeBoard.calls)
        val nl = System.lineSeparator()
        assertTrue(output.toString().startsWith("1\t2\t3\t4\t$nl"))
    }

    @Test
    fun `play returns without looping when processCommand throws EndOfInputException`(): Unit = runBlocking {
        val presenter = FakePresenter()
        val (view, output) = viewWith("", presenter)

        view.play()

        // One board render before the EOF, no shift ever attempted.
        assertTrue(output.toString().isNotEmpty())
        assertTrue(presenter.calls.none { it.startsWith("shift") })
    }

    /** Guards [ViewImpl.play]'s `catch (e: ExitRequestedException) { break }` - a real
     *  `exitGame()` call, reached the same way a real session reaches it (the `exit` command,
     *  answered through the loop's own request handler), must stop the loop instead of
     *  processing whatever input follows. */
    @Test
    fun `play returns when exitGame requests an exit`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val presenter = PresenterImpl(board, FakeSaveRepository())
        // "n" declines the save-before-quitting prompt - exitGame() throws immediately. The
        // trailing "left 1" must never be reached if the loop actually stops at exit.
        val (view, _) = viewWith("exit\nn\nleft 1\n", presenter)

        view.play()

        assertTrue(board.calls.none { it.startsWith("shift") })
    }

    @Test
    fun `play swallows processCommand exceptions and keeps looping`(): Unit = runBlocking {
        val fakeBoard = FakeBoardModel()
        val presenter = PresenterImpl(solveOnShiftLeft(fakeBoard))
        val (view, output) = viewWith("banana\nleft 1\n", presenter)

        view.play()

        assertTrue(output.toString().contains("Incorrect input"))
        assertEquals(listOf("shiftLeft(0)"), fakeBoard.calls)
    }

    @Test
    fun `play routes a swallowed exception's message through showMessage, not System-err`(): Unit = runBlocking {
        val presenter = PresenterImpl(FakeBoardModel())
        val (view, output) = viewWith("left abc\n", presenter)

        view.play()

        assertTrue(output.toString().contains("Incorrect input"))
    }

    /** Play-loop-specific half of `exit, save confirmed but the save itself fails` - "does not
     *  quit" is a loop-continuation property of [ViewImpl.play], not of
     *  [presenter.PresenterImpl.exitGame] itself, whose own message/analytics contract is
     *  covered directly, without a play() loop, in [presenter.PresenterExitTest]. */
    @Test
    fun `a failed save during exit does not quit - play keeps looping`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        saves.saveException = RuntimeException("disk full")
        val presenter = PresenterImpl(FakeBoardModel(), saves)
        // "y" confirms saving, "foo" is the name - the save then fails, and the loop must not
        // have thrown ExitRequestedException, or this call would never return normally.
        val (view, output) = viewWith("exit\ny\nfoo\n", presenter)

        view.play()

        assertTrue(output.toString().contains("Not quitting"))
    }
}
