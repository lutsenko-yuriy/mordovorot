package view.tui

import InputMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import testing.FakeTerminal
import testing.FakeViewModel
import testing.RecordingAnalyticsService
import viewmodel.ViewModel

/**
 * Covers GH-3's solved-state view: disabled shift arrows, the "Congratulations ✓" title, the
 * live toolbar, `screen_congratulations` firing once per transition, and the transition back to
 * the normal board when a load restores an unsolved game. The state-driven design (no phase
 * flag - every repaint just asks [viewmodel.ViewModel.isSolved] fresh) is what makes the
 * "load from Congratulations" case fall out for free; see the plan's solved-state note.
 */
class TuiViewSolvedStateTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private fun view(terminal: FakeTerminal, viewModel: ViewModel, analytics: RecordingAnalyticsService = RecordingAnalyticsService()): TuiView =
        TuiView.create(terminal, analytics, viewModel = viewModel)

    // Matches MouseInput.decorateBoard's GH-30 modeButtons - see TuiViewDialogTest's identical
    // helper for why.
    private fun boardLayout(viewModel: FakeViewModel) =
        BoardLayout(terminalSize, viewModel.side, arrowsEnabled = true, modeButtons = listOf(InputMode.KEYBOARD, InputMode.CONSOLE))

    @Test
    fun `when the board becomes solved, arrow clicks make no viewModel call`(): Unit = runBlocking {
        val viewModel = FakeViewModel().apply { solved = true }
        val (x, y) = boardLayout(viewModel).leftArrowPosition(0)
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(x, y)), terminalSize = terminalSize)

        view(terminal, viewModel).play()

        assertFalse(viewModel.calls.contains("shiftLeft(0)"))
    }

    @Test
    fun `the title changes to Congratulations tick on solve`(): Unit = runBlocking {
        val viewModel = FakeViewModel().apply { solved = true }
        val terminal = FakeTerminal(terminalSize = terminalSize)

        view(terminal, viewModel).play()

        assertTrue(terminal.frames.last().contains("Congratulations ✓"))
        assertFalse(terminal.frames.last().contains("Mordovorot"))
    }

    @Test
    fun `Save, Load, and Exit remain active after solve`(): Unit = runBlocking {
        val viewModel = FakeViewModel().apply { solved = true }

        val (saveX, saveY) = boardLayout(viewModel).saveButtonPosition()
        val saveTerminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(saveX, saveY)), terminalSize = terminalSize)
        view(saveTerminal, viewModel).play()
        assertTrue(saveTerminal.frames.any { it.contains("Save game") })

        val (loadX, loadY) = boardLayout(viewModel).loadButtonPosition()
        val loadTerminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(loadX, loadY)), terminalSize = terminalSize)
        view(loadTerminal, viewModel).play()
        assertTrue(loadTerminal.frames.any { it.contains("Load game") })

        val (exitX, exitY) = boardLayout(viewModel).exitButtonPosition()
        val exitTerminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(exitX, exitY)), terminalSize = terminalSize)
        view(exitTerminal, viewModel).play()
        assertTrue(exitTerminal.frames.any { it.contains("Save before quitting?") })
    }

    @Test
    fun `screen_congratulations fires exactly once when a shift solves the board`(): Unit = runBlocking {
        // solved() flips to true only once shiftLeft(0) actually runs - a real transition
        // reached by playing, not by loading an already-solved save (see the next test).
        val delegate = FakeViewModel()
        val viewModel = object : ViewModel by delegate {
            override fun shiftLeft(row: Int) {
                delegate.shiftLeft(row)
                delegate.solved = true
            }
        }
        val analytics = RecordingAnalyticsService()
        val (x, y) = boardLayout(delegate).leftArrowPosition(0)
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(x, y)), terminalSize = terminalSize)

        view(terminal, viewModel, analytics).play()

        assertTrue(delegate.calls.contains("shiftLeft(0)"))
        assertTrue(analytics.events.count { it.name == "screen_congratulations" } == 1)
    }

    @Test
    fun `screen_congratulations does not fire when a startup restore loads an already-solved save`(): Unit = runBlocking {
        // Audit finding on PR #25: gating solely on isSolved() at repaint time fired this event
        // on every restore of a pre-solved save (startup or toolbar Load) - inflating a "board
        // was solved by playing" metric with saves that were already solved before this session
        // even started. The event now fires only from a shift that causes the transition.
        val delegate = FakeViewModel().apply { solved = true }
        val viewModel = object : ViewModel by delegate {
            override suspend fun restoreOnStartup() {
                delegate.restoreOnStartup()
                delegate.loadGame("solved-save")
            }
        }
        val analytics = RecordingAnalyticsService()
        val terminal = FakeTerminal(terminalSize = terminalSize)

        view(terminal, viewModel, analytics).play()

        assertTrue(terminal.frames.last().contains("Congratulations ✓"))
        assertTrue(analytics.events.none { it.name == "screen_congratulations" })
    }

    @Test
    fun `loading an unsolved board from the Congratulations screen re-enables the arrows and title`(): Unit = runBlocking {
        val delegate = FakeViewModel().apply { solved = true; saveNames = listOf("save1") }
        val viewModel = object : ViewModel by delegate {
            override suspend fun loadGame(name: String) {
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

        view(terminal, viewModel).play()

        assertTrue(delegate.calls.contains("loadGame(save1)"))
        assertTrue(delegate.calls.contains("shiftLeft(0)"))
        val boardFrame = terminal.frames.last()
        assertTrue(boardFrame.contains("Mordovorot"))
        assertFalse(boardFrame.contains("Congratulations"))
    }
}
