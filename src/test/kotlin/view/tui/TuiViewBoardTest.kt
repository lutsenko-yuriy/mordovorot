package view.tui

import testing.FakeViewModel
import testing.FakeTerminal
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers GH-3's board interaction loop: `TuiView` dispatching mouse clicks on the board to the
 * viewModel and repainting. Driven via [FakeTerminal] + [FakeViewModel] - no real terminal or
 * viewModel logic involved.
 */
class TuiViewBoardTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private fun layoutFor(viewModel: FakeViewModel) =
        BoardLayout(terminalSize, viewModel.side, viewModel.solved.not())

    private fun view(terminal: FakeTerminal, viewModel: FakeViewModel): TuiView =
        TuiView.create(terminal, viewModel = viewModel)

    @Test
    fun `clicking a row's left or right arrow calls shiftLeft or shiftRight with the 0-based row`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val layout = layoutFor(viewModel)
        val (lx, ly) = layout.leftArrowPosition(1)
        val (rx, ry) = layout.rightArrowPosition(2)
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(lx, ly), TerminalEvent.MouseClick(rx, ry)),
            terminalSize = terminalSize,
        )

        view(terminal, viewModel).play()

        assertTrue(viewModel.calls.contains("shiftLeft(1)"))
        assertTrue(viewModel.calls.contains("shiftRight(2)"))
    }

    @Test
    fun `clicking a column's up or down arrow calls shiftUp or shiftDown with the 0-based column`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val layout = layoutFor(viewModel)
        val (ux, uy) = layout.upArrowPosition(2)
        val (dx, dy) = layout.downArrowPosition(3)
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(ux, uy), TerminalEvent.MouseClick(dx, dy)),
            terminalSize = terminalSize,
        )

        view(terminal, viewModel).play()

        assertTrue(viewModel.calls.contains("shiftUp(2)"))
        assertTrue(viewModel.calls.contains("shiftDown(3)"))
    }

    @Test
    fun `the board repaints after every click`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val layout = layoutFor(viewModel)
        val (lx, ly) = layout.leftArrowPosition(0)
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(lx, ly), TerminalEvent.MouseClick(lx, ly)),
            terminalSize = terminalSize,
        )

        view(terminal, viewModel).play()

        // One initial paint on startup, plus one per click.
        assertEquals(3, terminal.frames.size)
    }

    @Test
    fun `a click on dead space makes no viewModel call`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val layout = layoutFor(viewModel)
        val (lx, ly) = layout.leftArrowPosition(0)
        // A cell well inside the grid body, not on any arrow or toolbar button.
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(lx + 5, ly + 1)),
            terminalSize = terminalSize,
        )

        view(terminal, viewModel).play()

        assertTrue(viewModel.calls.none { it.startsWith("shift") })
    }

    @Test
    fun `EndOfInput from the terminal ends the loop cleanly`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val terminal = FakeTerminal(events = mutableListOf(), terminalSize = terminalSize)

        view(terminal, viewModel).play()

        // No click events were scripted - readEvent() falls straight through to EndOfInput.
        assertTrue(viewModel.calls.none { it.startsWith("shift") })
    }

    @Test
    fun `a solved board disables the shift arrows and shows the Congratulations title`(): Unit = runBlocking {
        // Confirmed product decision: once solved, the arrows go dead - Ctrl+C is the only way
        // out until WU4/5 add a live toolbar/Congratulations screen to replace them. An earlier
        // audit-driven attempt to keep the arrows live instead (rounds 2/3 on PR #22) was a
        // misreading of that trade-off - reverted per direct confirmation.
        val viewModel = FakeViewModel()
        viewModel.solved = true
        val layout = BoardLayout(terminalSize, viewModel.side, arrowsEnabled = true)
        val (lx, ly) = layout.leftArrowPosition(0)
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(lx, ly)), terminalSize = terminalSize)

        view(terminal, viewModel).play()

        assertTrue(viewModel.calls.none { it.startsWith("shift") })
        assertTrue(terminal.frames.last().contains("Congratulations ✓"))
    }

    @Test
    fun `play enters raw mode and mouse reporting before reading any event, and restores on the way out`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val terminal = FakeTerminal(events = mutableListOf(), terminalSize = terminalSize)

        view(terminal, viewModel).play()

        assertTrue(terminal.rawModeEntered)
        assertTrue(terminal.mouseReportingEnabled)
        assertTrue(terminal.restored)
    }
}
