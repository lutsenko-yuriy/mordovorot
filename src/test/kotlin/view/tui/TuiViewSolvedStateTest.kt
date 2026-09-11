package view.tui

import presenter.Presenter
import testing.FakePresenter
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers GH-3's solved-state view: disabled shift arrows, the "Congratulations ✓" title, the
 * live toolbar, `screen_congratulations` firing once per transition, and the transition back to
 * the normal board when a load restores an unsolved game. The state-driven design (no phase
 * flag - every repaint just asks [presenter.Presenter.isSolved] fresh) is what makes the
 * "load from Congratulations" case fall out for free; see the plan's solved-state note.
 */
class TuiViewSolvedStateTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private fun view(terminal: FakeTerminal, presenter: Presenter, analytics: RecordingAnalyticsService = RecordingAnalyticsService()): TuiView =
        TuiView.create(terminal, analytics) { presenter }

    private fun boardLayout(presenter: FakePresenter) = BoardLayout(terminalSize, presenter.side, arrowsEnabled = true)

    @Test
    fun `when the board becomes solved, arrow clicks make no presenter call`() {
        val presenter = FakePresenter().apply { solved = true }
        val (x, y) = boardLayout(presenter).leftArrowPosition(0)
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(x, y)), terminalSize = terminalSize)

        view(terminal, presenter).play()

        assertFalse(presenter.calls.contains("shiftLeft(0)"))
    }

    @Test
    fun `the title changes to Congratulations tick on solve`() {
        val presenter = FakePresenter().apply { solved = true }
        val terminal = FakeTerminal(terminalSize = terminalSize)

        view(terminal, presenter).play()

        assertTrue(terminal.frames.last().contains("Congratulations ✓"))
        assertFalse(terminal.frames.last().contains("Mordovorot"))
    }

    @Test
    fun `Save, Load, and Exit remain active after solve`() {
        val presenter = FakePresenter().apply { solved = true }

        val (saveX, saveY) = boardLayout(presenter).saveButtonPosition()
        val saveTerminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(saveX, saveY)), terminalSize = terminalSize)
        view(saveTerminal, presenter).play()
        assertTrue(saveTerminal.frames.any { it.contains("Save game") })

        val (loadX, loadY) = boardLayout(presenter).loadButtonPosition()
        val loadTerminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(loadX, loadY)), terminalSize = terminalSize)
        view(loadTerminal, presenter).play()
        assertTrue(loadTerminal.frames.any { it.contains("Load game") })

        val (exitX, exitY) = boardLayout(presenter).exitButtonPosition()
        val exitTerminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(exitX, exitY)), terminalSize = terminalSize)
        view(exitTerminal, presenter).play()
        assertTrue(exitTerminal.frames.any { it.contains("Save before quitting?") })
    }

    @Test
    fun `screen_congratulations fires exactly once on the transition into solved`() {
        // isSolved() is queried once per repaint: once at startup, then once per click below.
        // false, false, true, true mirrors: starts unsolved, still unsolved after a dead-space
        // click, becomes solved after the next click, and stays solved after a third click -
        // the event must fire on the third query (the transition), not the fourth (still solved).
        val solvedSequence = mutableListOf(false, false, true, true)
        val delegate = FakePresenter()
        val presenter = object : Presenter by delegate {
            override fun isSolved(): Boolean {
                delegate.calls.add("isSolved")
                return solvedSequence.removeAt(0)
            }
        }
        val analytics = RecordingAnalyticsService()
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(-1, -1),
                TerminalEvent.MouseClick(-1, -1),
                TerminalEvent.MouseClick(-1, -1),
            ),
            terminalSize = terminalSize,
        )

        view(terminal, presenter, analytics).play()

        assertTrue(solvedSequence.isEmpty(), "expected exactly 4 isSolved() queries to be consumed")
        assertTrue(analytics.events.count { it.name == "screen_congratulations" } == 1)
    }

    @Test
    fun `loading an unsolved board from the Congratulations screen re-enables the arrows and title`() {
        val delegate = FakePresenter().apply { solved = true; saveNames = listOf("save1") }
        val presenter = object : Presenter by delegate {
            override fun loadGame(name: String) {
                delegate.loadGame(name)
                delegate.solved = false
            }
        }
        val (loadX, loadY) = boardLayout(delegate).loadButtonPosition()
        val loadDialog = Dialog(
            kind = Dialog.Kind.LOAD,
            title = "Load game",
            listItems = listOf("save1"),
            selectedIndex = 0,
            buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
        )
        val loadButton = DialogLayout(loadDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("load") }
        val (leftArrowX, leftArrowY) = boardLayout(delegate).leftArrowPosition(0)
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(loadX, loadY),
                TerminalEvent.MouseClick(loadButton.x, DialogLayout(loadDialog, terminalSize).buttonsRow()),
                TerminalEvent.MouseClick(leftArrowX, leftArrowY),
            ),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(delegate.calls.contains("loadGame(save1)"))
        assertTrue(delegate.calls.contains("shiftLeft(0)"))
        val boardFrame = terminal.frames.last()
        assertTrue(boardFrame.contains("Mordovorot"))
        assertFalse(boardFrame.contains("Congratulations"))
    }
}
