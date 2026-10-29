package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import testing.FakeTerminal
import testing.FakeViewModel
import testing.testTuiView

/**
 * Covers the keyboard-driven board interaction loop (GH-18, GH-101): `TuiView` dispatching
 * WASD selection and HJKL/arrow shifts in `--keyboard` mode. Mirrors `TuiViewBoardTest`
 * (GH-3's mouse equivalent) but driven via keyboard events - no clicks.
 */
class TuiViewKeyboardBoardTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private fun view(terminal: FakeTerminal, viewModel: FakeViewModel): TuiView =
        testTuiView(terminal, input = KeyboardInput(KeyboardSelection()), viewModel = viewModel)

    @Test
    fun `S then H shifts row 2 left, passing the HJKL key to the viewModel`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.KeyPress('s'), TerminalEvent.KeyPress('h')),
            terminalSize = terminalSize,
        )

        view(terminal, viewModel).play()

        assertEquals(listOf("shiftLeft(1, HJKL)"), viewModel.calls.filter { it.startsWith("shift") })
    }

    @Test
    fun `D then the down arrow shifts column 2 down, passing the ARROW key`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.KeyPress('d'), TerminalEvent.Arrow(Direction.DOWN)),
            terminalSize = terminalSize,
        )

        view(terminal, viewModel).play()

        assertEquals(listOf("shiftDown(1, ARROW)"), viewModel.calls.filter { it.startsWith("shift") })
    }

    @Test
    fun `2 and 3 open Save and Load, Escape opens Exit, all regardless of cursor position`(): Unit = runBlocking {
        // Any frame, not just the last - EndOfInput closing the dialog triggers one more board
        // repaint before quitting.
        suspend fun framesFor(event: TerminalEvent): List<String> {
            val viewModel = FakeViewModel()
            val terminal = FakeTerminal(
                events = mutableListOf(TerminalEvent.Arrow(Direction.DOWN), event),
                terminalSize = terminalSize,
            )
            view(terminal, viewModel).play()
            return terminal.frames
        }

        assertTrue(framesFor(TerminalEvent.KeyPress('2')).any { it.contains("Save game") })
        assertTrue(framesFor(TerminalEvent.KeyPress('3')).any { it.contains("Load game") })
        assertTrue(framesFor(TerminalEvent.Escape).any { it.contains("Save before quitting?") })
    }

    @Test
    fun `the board repaints after every cursor move`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.Arrow(Direction.RIGHT)),
            terminalSize = terminalSize,
        )

        view(terminal, viewModel).play()

        // One initial paint on startup, plus one per move.
        assertEquals(3, terminal.frames.size)
    }

    @Test
    fun `play enters raw mode without enabling mouse reporting, and restores on the way out`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val terminal = FakeTerminal(events = mutableListOf(), terminalSize = terminalSize)

        view(terminal, viewModel).play()

        assertTrue(terminal.rawModeEntered)
        assertTrue(terminal.restored)
        assertFalse(terminal.mouseReportingEnabled)
    }

    @Test
    fun `EndOfInput from the terminal ends the loop cleanly`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val terminal = FakeTerminal(events = mutableListOf(), terminalSize = terminalSize)

        view(terminal, viewModel).play()

        assertTrue(viewModel.calls.none { it.startsWith("shift") })
    }
}
