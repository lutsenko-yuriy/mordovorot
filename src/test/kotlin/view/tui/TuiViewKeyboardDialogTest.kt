package view.tui

import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import presenter.TuiPresenterImpl
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Covers GH-18's keyboard-driven Save/Load/Exit dialogs and the startup restore prompt, plus
 * their analytics. Mirrors `TuiViewDialogTest` (GH-3's mouse equivalent) but driven via
 * F5/F6/Escape, Tab/arrow-key focus movement, and Enter/Escape instead of clicks.
 */
class TuiViewKeyboardDialogTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private fun viewWithRealPresenter(
        terminal: FakeTerminal,
        saves: FakeSaveRepository = FakeSaveRepository(),
        board: FakeBoardModel = FakeBoardModel(),
        analytics: AnalyticsService = RecordingAnalyticsService(),
    ): TuiView = TuiView.create(terminal, analytics, input = KeyboardInput()) { v -> TuiPresenterImpl(v, board, saves, analytics) }

    @Test
    fun `the exit flow's Save re-prompt after a rejected name starts focus back on the text field`() {
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

        TuiView.create(terminal, input = KeyboardInput()) { v -> TuiPresenterImpl(v, board, saves) }.play()

        assertTrue(saves.saveCalls.any { it.first == "ok" })
    }

    @Test
    fun `Save dialog happy path - typed name, Tab to the Save button, Enter saves and closes`() {
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

        viewWithRealPresenter(terminal, saves, board).play()

        assertTrue(saves.saveCalls.any { it.first == "hi" })
        assertTrue(terminal.frames.last().contains("Saved as 'hi'."))
    }

    @Test
    fun `Save dialog Escape cancels and tracks dialog_cancelled with input_method keyboard`() {
        val saves = FakeSaveRepository()
        val recording = RecordingAnalyticsService()
        val analytics = InputMethodAnalyticsService(recording, inputMethod = "keyboard")
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.FunctionKey(5), TerminalEvent.Escape),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, saves, analytics = analytics).play()

        assertTrue(saves.saveCalls.isEmpty())
        assertTrue(
            recording.events.any {
                it.name == "dialog_cancelled" && it.properties["dialog"] == "save" && it.properties["input_method"] == "keyboard"
            },
        )
    }

    @Test
    fun `pressing Enter with an empty name in the Save dialog cancels instead of looping forever`() {
        val saves = FakeSaveRepository()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.FunctionKey(5), TerminalEvent.Tab, TerminalEvent.Enter),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, saves).play()

        assertTrue(saves.saveCalls.isEmpty())
    }

    @Test
    fun `Load dialog - Down selects a row, Tab to Load, Enter restores that save`() {
        val saved = storage.SavedBoard(4, IntArray(16) { it })
        val saves = FakeSaveRepository(mutableMapOf("foo" to saved, "bar" to saved))
        val board = FakeBoardModel()
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.FunctionKey(6),
                TerminalEvent.Arrow(Direction.DOWN), // selection -> "bar"
                TerminalEvent.Tab, // focus -> the Load button
                TerminalEvent.Enter,
            ),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, saves, board).play()

        assertTrue(board.calls.any { it.startsWith("restoreState") })
    }

    @Test
    fun `Load dialog with no saves can only be cancelled`() {
        val saves = FakeSaveRepository()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.FunctionKey(6), TerminalEvent.Tab, TerminalEvent.Enter),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, saves).play()

        assertTrue(saves.saveCalls.isEmpty())
        assertTrue(terminal.frames.last().contains("Mordovorot"))
    }

    @Test
    fun `Exit dialog Yes - Tab to Yes, Enter opens the Save dialog, then saving quits`() {
        val saves = FakeSaveRepository()
        val board = FakeBoardModel()
        val analytics = RecordingAnalyticsService()
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.Escape, // open the Exit dialog, focus starts on Yes
                TerminalEvent.Enter,
                TerminalEvent.KeyPress('h'),
                TerminalEvent.Tab,
                TerminalEvent.Enter,
            ),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, saves, board, analytics).play()

        assertTrue(saves.saveCalls.any { it.first == "h" })
        assertTrue(analytics.events.any { it.name == "exit_command_used" && it.properties["save_choice"] == "saved" })
    }

    @Test
    fun `Exit dialog Escape cancels and keeps playing`() {
        val analytics = RecordingAnalyticsService()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.Escape, TerminalEvent.Escape),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, analytics = analytics).play()

        assertTrue(terminal.frames.last().contains("Mordovorot"))
        assertTrue(analytics.events.any { it.name == "dialog_cancelled" && it.properties["dialog"] == "exit" })
    }

    @Test
    fun `startup restore dialog is navigable by keyboard - Down then Enter restores`() {
        val saved = storage.SavedBoard(4, IntArray(16) { it })
        val saves = FakeSaveRepository(mutableMapOf("foo" to saved))
        val board = FakeBoardModel()
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.Tab, // focus starts on the single row - move to the Load button
                TerminalEvent.Enter,
            ),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, saves, board).play()

        assertTrue(board.calls.any { it.startsWith("restoreState") })
    }
}
