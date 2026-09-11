package view.tui

import testing.FakeTuiPresenter
import testing.FakeTerminal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers GH-3's board interaction loop: `TuiView` dispatching mouse clicks on the board to the
 * presenter and repainting. Driven via [FakeTerminal] + [FakeTuiPresenter] - no real terminal or
 * presenter logic involved.
 */
class TuiViewBoardTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private fun layoutFor(presenter: FakeTuiPresenter) =
        BoardLayout(terminalSize, presenter.side, presenter.solved.not())

    private fun view(terminal: FakeTerminal, presenter: FakeTuiPresenter): TuiView =
        TuiView.create(terminal) { presenter }

    @Test
    fun `clicking a row's left or right arrow calls shiftLeft or shiftRight with the 0-based row`() {
        val presenter = FakeTuiPresenter()
        val layout = layoutFor(presenter)
        val (lx, ly) = layout.leftArrowPosition(1)
        val (rx, ry) = layout.rightArrowPosition(2)
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(lx, ly), TerminalEvent.MouseClick(rx, ry)),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(presenter.calls.contains("shiftLeft(1)"))
        assertTrue(presenter.calls.contains("shiftRight(2)"))
    }

    @Test
    fun `clicking a column's up or down arrow calls shiftUp or shiftDown with the 0-based column`() {
        val presenter = FakeTuiPresenter()
        val layout = layoutFor(presenter)
        val (ux, uy) = layout.upArrowPosition(2)
        val (dx, dy) = layout.downArrowPosition(3)
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(ux, uy), TerminalEvent.MouseClick(dx, dy)),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(presenter.calls.contains("shiftUp(2)"))
        assertTrue(presenter.calls.contains("shiftDown(3)"))
    }

    @Test
    fun `the board repaints after every click`() {
        val presenter = FakeTuiPresenter()
        val layout = layoutFor(presenter)
        val (lx, ly) = layout.leftArrowPosition(0)
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(lx, ly), TerminalEvent.MouseClick(lx, ly)),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        // One initial paint on startup, plus one per click.
        assertEquals(3, terminal.frames.size)
    }

    @Test
    fun `a click on dead space makes no presenter call`() {
        val presenter = FakeTuiPresenter()
        val layout = layoutFor(presenter)
        val (lx, ly) = layout.leftArrowPosition(0)
        // A cell well inside the grid body, not on any arrow or toolbar button.
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(lx + 5, ly + 1)),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(presenter.calls.none { it.startsWith("shift") })
    }

    @Test
    fun `EndOfInput from the terminal ends the loop cleanly`() {
        val presenter = FakeTuiPresenter()
        val terminal = FakeTerminal(events = mutableListOf(), terminalSize = terminalSize)

        view(terminal, presenter).play()

        // No click events were scripted - readEvent() falls straight through to EndOfInput.
        assertTrue(presenter.calls.none { it.startsWith("shift") })
    }

    @Test
    fun `a solved board disables the shift arrows and shows the Congratulations title`() {
        // Confirmed product decision: once solved, the arrows go dead - Ctrl+C is the only way
        // out until WU4/5 add a live toolbar/Congratulations screen to replace them. An earlier
        // audit-driven attempt to keep the arrows live instead (rounds 2/3 on PR #22) was a
        // misreading of that trade-off - reverted per direct confirmation.
        val presenter = FakeTuiPresenter()
        presenter.solved = true
        val layout = BoardLayout(terminalSize, presenter.side, arrowsEnabled = true)
        val (lx, ly) = layout.leftArrowPosition(0)
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(lx, ly)), terminalSize = terminalSize)

        view(terminal, presenter).play()

        assertTrue(presenter.calls.none { it.startsWith("shift") })
        assertTrue(terminal.frames.last().contains("Congratulations ✓"))
    }

    @Test
    fun `play enters raw mode and mouse reporting before reading any event, and restores on the way out`() {
        val presenter = FakeTuiPresenter()
        val terminal = FakeTerminal(events = mutableListOf(), terminalSize = terminalSize)

        view(terminal, presenter).play()

        assertTrue(terminal.rawModeEntered)
        assertTrue(terminal.mouseReportingEnabled)
        assertTrue(terminal.restored)
    }
}
