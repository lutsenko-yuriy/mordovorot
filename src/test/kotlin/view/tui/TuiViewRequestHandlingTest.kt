package view.tui

import InputMode
import presenter.ModeSwitchRequestedException
import presenter.TuiPresenterImpl
import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeTerminal
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Closes a coverage gap an audit found on PR #45 (GH-42 WU2): every `TuiView*Test` that drives
 * `play()` uses either `FakeTuiPresenter` (whose `uiRequests` nothing ever sends to - the
 * handler starts, suspends, and is cancelled without ever reaching [TuiView.handle]) or a real
 * `TuiPresenterImpl` with exactly one save (exercising `handle`'s `ConfirmRestore`/
 * `ConfirmSaveBeforeExit`/`PromptSaveName` branches via the exit and 1-save-startup flows, but
 * never `ChooseSaveToRestore` - that needs 2+ saves).
 */
class TuiViewRequestHandlingTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    /** Hard problem 2's residual case (GH-42 WU2, see the plan comment on GH-42), TuiView's half
     *  of [view.ViewImplRequestHandlingTest] - a control-flow exception thrown from *inside* the
     *  request-handler coroutine still escapes [TuiView.play] to its caller. Forced here by
     *  making the very first `terminal.write` (reached from inside `handle`'s `ConfirmRestore`
     *  branch, since `restoreOnStartup()` runs before the board's own first `repaint()`) throw. */
    @Test
    fun `an exception thrown inside the request handler escapes play() to the caller`(): Unit = runBlocking {
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, IntArray(16) { it })))
        val board = FakeBoardModel()
        var writeCount = 0
        val terminal = object : Terminal {
            override fun enterRawMode() {}
            override fun restore() {}
            override fun enableMouseReporting() {}
            override fun write(frame: String) {
                writeCount++
                if (writeCount == 1) throw ModeSwitchRequestedException(InputMode.CONSOLE)
            }
            override fun readEvent(): TerminalEvent = TerminalEvent.EndOfInput
            override fun size(): TerminalSize = terminalSize
        }
        val view = TuiView.create(terminal) { v -> TuiPresenterImpl(v, board, saves) }

        assertFailsWith<ModeSwitchRequestedException> { view.play() }
    }

    @Test
    fun `chooseSaveToRestore is answered through the real request handler when 2+ saves exist at startup`(): Unit = runBlocking {
        val fooState = IntArray(16) { it }
        val barState = intArrayOf(3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12)
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, fooState),
                "bar" to SavedBoard(4, barState),
            ),
        )
        val board = FakeBoardModel()
        val dialog = Dialog(
            Dialog.Kind.LOAD,
            "Restore a saved game?",
            listItems = listOf("bar", "foo"),
            buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
        )
        val dialogLayout = DialogLayout(dialog, terminalSize)
        val loadButton = dialogLayout.buttons().first { it.target == HitTarget.DialogButton("load") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                // Row 1 is "foo" - "bar" (row 0) is selected by default.
                TerminalEvent.MouseClick(dialogLayout.left + 2, dialogLayout.listRowPosition(1)),
                TerminalEvent.MouseClick(loadButton.x, dialogLayout.buttonsRow()),
            ),
            terminalSize = terminalSize,
        )

        TuiView.create(terminal) { v -> TuiPresenterImpl(v, board, saves) }.play()

        assertTrue(terminal.frames.first().contains("Restore a saved game?"))
        assertEquals(listOf("restoreState(${fooState.toList()})"), board.calls)
    }
}
