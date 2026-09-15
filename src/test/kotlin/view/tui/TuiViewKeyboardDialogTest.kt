package view.tui

import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import viewmodel.ViewModelImpl

/**
 * Covers GH-18's keyboard-driven Save/Load/Exit dialogs and the startup restore prompt, plus
 * their analytics. Mirrors `TuiViewDialogTest` (GH-3's mouse equivalent) but driven via
 * F5/F6/Escape, Tab/arrow-key focus movement, and Enter/Escape instead of clicks.
 */
class TuiViewKeyboardDialogTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private fun viewWithRealViewModel(
        terminal: FakeTerminal,
        saves: FakeSaveRepository = FakeSaveRepository(),
        board: FakeBoardModel = FakeBoardModel(),
        analytics: AnalyticsService = RecordingAnalyticsService(),
    ): TuiView = TuiView.create(terminal, analytics, input = KeyboardInput(), viewModel = ViewModelImpl(board, saves, analytics))

    @Test
    fun `the exit flow's Save re-prompt after a rejected name starts focus back on the text field`(): Unit = runBlocking {
        // Regression: the Exit -> Save handoff chains into a new modal loop with no board
        // repaint in between, so dialogFocusIndex used to carry over from the previous dialog.
        val saves = FakeSaveRepository()
        val board = FakeBoardModel()
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.Escape, // open the Exit dialog
                TerminalEvent.Tab, // focus -> "no"
                TerminalEvent.BackTab, // focus back -> "yes"
                TerminalEvent.Enter, // Yes - the Save dialog opens directly, no board repaint first
                TerminalEvent.KeyPress('a'), TerminalEvent.KeyPress(' '), TerminalEvent.KeyPress('b'), // invalid: a space
                TerminalEvent.Tab, // focus -> the Save button
                TerminalEvent.Enter, // rejected - re-prompts with an explanation
                TerminalEvent.KeyPress('o'), TerminalEvent.KeyPress('k'),
                TerminalEvent.Tab, // focus -> the Save button
                TerminalEvent.Enter, // saves "ok"
            ),
            terminalSize = terminalSize,
        )

        TuiView.create(terminal, input = KeyboardInput(), viewModel = ViewModelImpl(board, saves)).play()

        assertTrue(saves.saveCalls.any { it.first == "ok" })
    }

    @Test
    fun `Save dialog happy path - typed name, Tab to the Save button, Enter saves and closes`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        val board = FakeBoardModel()
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.FunctionKey(5),
                TerminalEvent.KeyPress('h'), TerminalEvent.KeyPress('i'),
                TerminalEvent.Tab,
                TerminalEvent.Enter,
            ),
            terminalSize = terminalSize,
        )

        viewWithRealViewModel(terminal, saves, board).play()

        assertTrue(saves.saveCalls.any { it.first == "hi" })
        assertTrue(terminal.frames.last().contains("Saved as 'hi'."))
    }

    @Test
    fun `Save dialog Escape cancels and tracks dialog_cancelled with input_method keyboard`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        val recording = RecordingAnalyticsService()
        val analytics = InputMethodAnalyticsService(recording, inputMethod = "keyboard")
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.FunctionKey(5), TerminalEvent.Escape),
            terminalSize = terminalSize,
        )

        viewWithRealViewModel(terminal, saves, analytics = analytics).play()

        assertTrue(saves.saveCalls.isEmpty())
        assertTrue(
            recording.events.any {
                it.name == "dialog_cancelled" && it.properties["dialog"] == "save" && it.properties["input_method"] == "keyboard"
            },
        )
    }

    @Test
    fun `pressing Enter with an empty name in the Save dialog cancels instead of looping forever`(): Unit = runBlocking {
        // No Tab - focus stays on the text field, so Enter goes through the Submit path
        // (as opposed to clicking the Save button with an empty field, already covered on the
        // mouse side by TuiViewDialogTest).
        val saves = FakeSaveRepository()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.FunctionKey(5), TerminalEvent.Enter),
            terminalSize = terminalSize,
        )

        viewWithRealViewModel(terminal, saves).play()

        assertTrue(saves.saveCalls.isEmpty())
    }

    @Test
    fun `Down on the Load dialog moves selection off the first (alphabetically first) row`(): Unit = runBlocking {
        val fooBoard = storage.SavedBoard(4, IntArray(16) { it })
        val barBoard = storage.SavedBoard(4, IntArray(16) { it + 1 })
        // listSaves() returns names sorted, so "bar" is row 0 (selected by default) and "foo" is
        // row 1 - Down must move onto "foo", not stay on "bar".
        val saves = FakeSaveRepository(mutableMapOf("foo" to fooBoard, "bar" to barBoard))
        val board = FakeBoardModel()
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.FunctionKey(6),
                TerminalEvent.Arrow(Direction.DOWN), // selection: "bar" (row 0) -> "foo" (row 1)
                TerminalEvent.Tab, // focus -> the Load button
                TerminalEvent.Enter,
            ),
            terminalSize = terminalSize,
        )

        viewWithRealViewModel(terminal, saves, board).play()

        assertTrue(board.calls.contains("restoreState(${fooBoard.state.toList()})"))
    }

    @Test
    fun `Load dialog with no saves can only be cancelled`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        val board = FakeBoardModel()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.FunctionKey(6), TerminalEvent.Tab, TerminalEvent.Enter),
            terminalSize = terminalSize,
        )

        viewWithRealViewModel(terminal, saves, board).play()

        assertTrue(board.calls.none { it.startsWith("restoreState") })
        assertFalse(terminal.frames.last().contains("Load game"))
    }

    @Test
    fun `Exit dialog Yes opens the Save dialog (focus starts there), then saving quits`(): Unit = runBlocking {
        val saves = FakeSaveRepository()
        val board = FakeBoardModel()
        val analytics = RecordingAnalyticsService()
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.Escape, // open the Exit dialog - focus starts on Yes
                TerminalEvent.Enter,
                TerminalEvent.KeyPress('h'),
                TerminalEvent.Tab,
                TerminalEvent.Enter, // saves "h" and quits
                // Left unconsumed if the quit actually happened - proves the loop didn't just
                // run out of scripted input, it stopped reading before reaching these.
                TerminalEvent.Arrow(Direction.DOWN),
                TerminalEvent.Enter,
            ),
            terminalSize = terminalSize,
        )

        viewWithRealViewModel(terminal, saves, board, analytics).play()

        assertTrue(saves.saveCalls.any { it.first == "h" })
        assertTrue(analytics.events.any { it.name == "exit_command_used" && it.properties["save_choice"] == "saved" })
        assertTrue(board.calls.none { it.startsWith("shift") })
    }

    @Test
    fun `Exit dialog Escape cancels and keeps playing`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.Escape, TerminalEvent.Escape),
            terminalSize = terminalSize,
        )

        viewWithRealViewModel(terminal, analytics = analytics).play()

        assertFalse(terminal.frames.last().contains("Save before quitting?"))
        assertTrue(terminal.frames.last().contains("Mordovorot"))
        assertTrue(analytics.events.any { it.name == "dialog_cancelled" && it.properties["dialog"] == "exit" })
    }

    @Test
    fun `startup restore dialog - Tab to Load, Enter restores the one save on offer`(): Unit = runBlocking {
        val saved = storage.SavedBoard(4, IntArray(16) { it })
        val saves = FakeSaveRepository(mutableMapOf("foo" to saved))
        val board = FakeBoardModel()
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.Tab, // focus: the one row -> the Load button
                TerminalEvent.Enter,
            ),
            terminalSize = terminalSize,
        )

        viewWithRealViewModel(terminal, saves, board).play()

        assertTrue(board.calls.contains("restoreState(${saved.state.toList()})"))
    }
}
