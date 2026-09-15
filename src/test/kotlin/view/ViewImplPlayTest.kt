package view

import board_model.BoardModel
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeViewModel
import viewmodel.ViewModel
import viewmodel.ViewModelImpl

/**
 * Covers the console session loop that moved from `ConsoleViewModelImpl.play()` into
 * [ViewImpl.play] (GH-42 WU3, hard problem 1 on the plan comment on GH-42 - a viewModel-owned
 * loop that called back into the View it also raised requests on would deadlock). Driven end to
 * end over a real [ViewImpl] and a [StringReader] of scripted console input - the loop's own
 * request-handler coroutine answers `confirmSaveBeforeExit`/`promptSaveName`/etc. from the same
 * input stream, exactly as a real session would, so no separate request-draining double is
 * needed here (unlike [viewmodel.ViewModelExitTest]'s direct `exitGame()` calls). Assertions read
 * real console output/board state instead of `FakeView` call counters - `FakeViewModel` covers
 * the loop-mechanics tests that don't need a real board; [testing.FakeSaveRepository] backs the
 * two that need a real save failure.
 */
class ViewImplPlayTest {

    private fun viewWith(input: String, viewModel: ViewModel): Pair<ViewImpl, ByteArrayOutputStream> {
        val outputBuffer = ByteArrayOutputStream()
        val view = ViewImpl(BufferedReader(StringReader(input)), PrintStream(outputBuffer))
        view.viewModel = viewModel
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
        val viewModel = FakeViewModel().apply { solved = true }
        val (view, output) = viewWith("", viewModel)

        view.play()

        assertEquals("", output.toString())
        assertFalse(viewModel.calls.contains("boardState"))
    }

    /** Console-side anchor for [viewmodel.ViewModelImpl.restoreOnStartup] - the flow's own
     *  branch matrix (0/1/2+ saves, decline, unknown name, etc.) is covered directly in
     *  [viewmodel.ViewModelStartupRestoreTest]; this test just pins that [ViewImpl.play] still
     *  calls it as its first step, before checking whether the board is solved. */
    @Test
    fun `play offers startup restore before entering the loop`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val (view, output) = viewWith("", viewModel)

        view.play()

        assertEquals("restoreOnStartup", viewModel.calls.first())
        assertTrue(viewModel.calls.contains("isSolved"))
        assertTrue(output.toString().isNotEmpty())
    }

    /** Pins the one ordering property [ViewImpl.play]'s shape actually depends on: the
     *  `uiRequests` handler must be launched *before* `restoreOnStartup()`, not just called
     *  before the loop - `FakeViewModel` can't catch this (it never raises a `UiRequest`), so
     *  this test drives a real [ViewModelImpl] through the handler instead (audit finding on
     *  PR #46). [withTimeout] turns a regression here into a failure instead of a silent hang -
     *  see `build.gradle.kts`'s note on `kotlinx-coroutines-test` for why that distinction
     *  matters for this exact class of bug. */
    @Test
    fun `play answers the startup restore prompt through its own request handler`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, IntArray(16) { it })))
        val (view, output) = viewWith("y\n", ViewModelImpl(board, saves))

        withTimeout(3_000) { view.play() }

        assertTrue(output.toString().contains("Restore save 'foo'?"))
        assertTrue(board.calls.any { it.startsWith("restoreState") })
    }

    @Test
    fun `play displays the board and processes one command before the board is solved`(): Unit = runBlocking {
        val fakeBoard = FakeBoardModel()
        val viewModel = ViewModelImpl(solveOnShiftLeft(fakeBoard), sizeChosenAtLaunch = true)
        val (view, output) = viewWith("left 1\n", viewModel)

        view.play()

        assertEquals(listOf("shiftLeft(0)"), fakeBoard.calls)
        val nl = System.lineSeparator()
        assertTrue(output.toString().startsWith("1\t2\t3\t4\t$nl"))
    }

    @Test
    fun `play returns without looping when processCommand throws EndOfInputException`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val (view, output) = viewWith("", viewModel)

        view.play()

        // One board render before the EOF, no shift ever attempted.
        assertTrue(output.toString().isNotEmpty())
        assertTrue(viewModel.calls.none { it.startsWith("shift") })
    }

    /** Guards [ViewImpl.play]'s `catch (e: ExitRequestedException) { break }` - a real
     *  `exitGame()` call, reached the same way a real session reaches it (the `exit` command,
     *  answered through the loop's own request handler), must stop the loop instead of
     *  processing whatever input follows. */
    @Test
    fun `play returns when exitGame requests an exit`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val viewModel = ViewModelImpl(board, FakeSaveRepository(), sizeChosenAtLaunch = true)
        // "n" declines the save-before-quitting prompt - exitGame() throws immediately. The
        // trailing "left 1" must never be reached if the loop actually stops at exit.
        val (view, _) = viewWith("exit\nn\nleft 1\n", viewModel)

        view.play()

        assertTrue(board.calls.none { it.startsWith("shift") })
    }

    @Test
    fun `play swallows processCommand exceptions and keeps looping`(): Unit = runBlocking {
        val fakeBoard = FakeBoardModel()
        val viewModel = ViewModelImpl(solveOnShiftLeft(fakeBoard), sizeChosenAtLaunch = true)
        val (view, output) = viewWith("banana\nleft 1\n", viewModel)

        view.play()

        assertTrue(output.toString().contains("Incorrect input"))
        assertEquals(listOf("shiftLeft(0)"), fakeBoard.calls)
    }

    @Test
    fun `size command with an out-of-range value shows a rejection message and the game keeps playing at the current size`(): Unit = runBlocking {
        // FakeBoardModel itself doesn't validate the side - newGameException stands in for
        // BoardImpl's real BoardSize.require throw (GH-44), same as shiftLeftException stands
        // in for shiftLeft's own real out-of-range check elsewhere in this file.
        val fakeBoard = FakeBoardModel()
        fakeBoard.newGameException = IllegalArgumentException("Board size must be between 3 and 5, got 9")
        val viewModel = ViewModelImpl(fakeBoard, sizeChosenAtLaunch = true)
        val (view, output) = viewWith("size 9\nleft 1\n", viewModel)

        view.play()

        assertTrue(output.toString().contains("Board size must be between 3 and 5, got 9"))
        // The loop kept going past the rejected size - the subsequent "left 1" still reached
        // the board, at whatever size it was left at.
        assertEquals(listOf("shiftLeft(0)"), fakeBoard.calls)
    }

    @Test
    fun `play routes a swallowed exception's message through showMessage, not System-err`(): Unit = runBlocking {
        val viewModel = ViewModelImpl(FakeBoardModel(), sizeChosenAtLaunch = true)
        val (view, output) = viewWith("left abc\n", viewModel)

        view.play()

        assertTrue(output.toString().contains("Incorrect input"))
    }

    /** Play-loop-specific half of `exit, save confirmed but the save itself fails` - "does not
     *  quit" is a loop-continuation property of [ViewImpl.play], not of
     *  [viewmodel.ViewModelImpl.exitGame] itself, whose own message/analytics contract is
     *  covered directly, without a play() loop, in [viewmodel.ViewModelExitTest]. */
    @Test
    fun `a failed save during exit does not quit - play keeps looping`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        saves.saveException = RuntimeException("disk full")
        val viewModel = ViewModelImpl(FakeBoardModel(), saves, sizeChosenAtLaunch = true)
        // "y" confirms saving, "foo" is the name - the save then fails, and the loop must not
        // have thrown ExitRequestedException, or this call would never return normally.
        val (view, output) = viewWith("exit\ny\nfoo\n", viewModel)

        view.play()

        assertTrue(output.toString().contains("Not quitting"))
    }
}
