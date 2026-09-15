package view.tui

import testing.FakeTerminal
import testing.FakeTuiPresenter
import testing.runTestBlocking
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

    private fun view(terminal: FakeTerminal, presenter: FakeTuiPresenter): TuiView =
        TuiView.create(terminal, input = KeyboardInput()) { presenter }

    @Test
    fun `an arrow-key walk around the ring followed by Enter calls the expected shift`() = runTestBlocking {
        val presenter = FakeTuiPresenter()
        // LEFT[0] (start) -> Down -> LEFT[1], per ArrowRing's movement table.
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.Enter),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(presenter.calls.contains("shiftLeft(1)"))
    }

    @Test
    fun `Space also activates the highlighted arrow, same as Enter`() = runTestBlocking {
        val presenter = FakeTuiPresenter()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.KeyPress(' ')),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(presenter.calls.contains("shiftLeft(1)"))
    }

    @Test
    fun `F5, F6 open Save and Load, Escape opens Exit, all regardless of cursor position`() = runTestBlocking {
        // Any frame, not just the last - EndOfInput closing the dialog triggers one more board
        // repaint before quitting.
        suspend fun framesFor(event: TerminalEvent): List<String> {
            val presenter = FakeTuiPresenter()
            val terminal = FakeTerminal(
                events = mutableListOf(TerminalEvent.Arrow(Direction.DOWN), event),
                terminalSize = terminalSize,
            )
            view(terminal, presenter).play()
            return terminal.frames
        }

        assertTrue(framesFor(TerminalEvent.FunctionKey(5)).any { it.contains("Save game") })
        assertTrue(framesFor(TerminalEvent.FunctionKey(6)).any { it.contains("Load game") })
        assertTrue(framesFor(TerminalEvent.Escape).any { it.contains("Save before quitting?") })
    }

    @Test
    fun `the board repaints after every cursor move`() = runTestBlocking {
        val presenter = FakeTuiPresenter()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.Arrow(Direction.RIGHT)),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        // One initial paint on startup, plus one per move.
        assertEquals(3, terminal.frames.size)
    }

    @Test
    fun `play enters raw mode without enabling mouse reporting, and restores on the way out`() = runTestBlocking {
        val presenter = FakeTuiPresenter()
        val terminal = FakeTerminal(events = mutableListOf(), terminalSize = terminalSize)

        view(terminal, presenter).play()

        assertTrue(terminal.rawModeEntered)
        assertTrue(terminal.restored)
        assertFalse(terminal.mouseReportingEnabled)
    }

    @Test
    fun `EndOfInput from the terminal ends the loop cleanly`() = runTestBlocking {
        val presenter = FakeTuiPresenter()
        val terminal = FakeTerminal(events = mutableListOf(), terminalSize = terminalSize)

        view(terminal, presenter).play()

        assertTrue(presenter.calls.none { it.startsWith("shift") })
    }
}
