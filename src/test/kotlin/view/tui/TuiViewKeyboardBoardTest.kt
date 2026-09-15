package view.tui

import testing.FakeTerminal
import testing.FakeViewModel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers GH-18's keyboard-driven board interaction loop: `TuiView` dispatching arrow-key cursor
 * movement and Enter/Space activation on the perimeter ring, in `--keyboard` mode. Mirrors
 * `TuiViewBoardTest` (GH-3's mouse equivalent) but driven via keyboard events - no clicks.
 */
class TuiViewKeyboardBoardTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private fun view(terminal: FakeTerminal, viewModel: FakeViewModel): TuiView =
        TuiView.create(terminal, input = KeyboardInput(), viewModel = viewModel)

    @Test
    fun `an arrow-key walk around the ring followed by Enter calls the expected shift`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        // LEFT[0] (start) -> Down -> LEFT[1], per ArrowRing's movement table.
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.Enter),
            terminalSize = terminalSize,
        )

        view(terminal, viewModel).play()

        assertTrue(viewModel.calls.contains("shiftLeft(1)"))
    }

    @Test
    fun `Space also activates the highlighted arrow, same as Enter`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.KeyPress(' ')),
            terminalSize = terminalSize,
        )

        view(terminal, viewModel).play()

        assertTrue(viewModel.calls.contains("shiftLeft(1)"))
    }

    @Test
    fun `F5, F6 open Save and Load, Escape opens Exit, all regardless of cursor position`(): Unit = runBlocking {
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

        assertTrue(framesFor(TerminalEvent.FunctionKey(5)).any { it.contains("Save game") })
        assertTrue(framesFor(TerminalEvent.FunctionKey(6)).any { it.contains("Load game") })
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
