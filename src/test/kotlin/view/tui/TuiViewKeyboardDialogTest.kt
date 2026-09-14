package view.tui

import presenter.TuiPresenterImpl
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeTerminal
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Covers GH-18's keyboard-driven Save/Load/Exit dialogs and the startup restore prompt, plus
 * their analytics. Mirrors `TuiViewDialogTest` (GH-3's mouse equivalent) but driven via
 * F5/F6/Escape, Tab/arrow-key focus movement, and Enter/Escape instead of clicks.
 */
class TuiViewKeyboardDialogTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

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
        // TODO: 1. Script an F5 event to open the Save dialog.
        // TODO: 2. Script KeyPress('h'), KeyPress('i') to type a name.
        // TODO: 3. Script a Tab event to move focus from the text field to the Save button.
        // TODO: 4. Script an Enter event to activate it.
        // TODO: 5. Play the view, then verify presenter.calls contains "saveGame(hi)" and the
        //          last frame no longer shows "Save game".
    }

    @Test
    fun `Save dialog Escape cancels and tracks dialog_cancelled with input_method keyboard`() {
        // TODO: 1. Script an F5 event, then an Escape event.
        // TODO: 2. Play the view with a RecordingAnalyticsService.
        // TODO: 3. Verify presenter.calls has no saveGame call.
        // TODO: 4. Verify analytics.events contains dialog_cancelled with dialog="save" and
        //          input_method="keyboard".
    }

    @Test
    fun `pressing Enter with an empty name in the Save dialog cancels instead of looping forever`() {
        // TODO: 1. Script an F5 event, then an Enter event with no typed name.
        // TODO: 2. Play the view, then verify presenter.calls has no saveGame call.
    }

    @Test
    fun `Load dialog - Down selects a row, Tab to Load, Enter restores that save`() {
        // TODO: 1. Set presenter.saveNames = listOf("foo", "bar").
        // TODO: 2. Script an F6 event, a Down event to move the list selection to "bar",
        //          a Tab event to move focus to the Load button, and an Enter event.
        // TODO: 3. Play the view, then verify presenter.calls contains "loadGame(bar)".
    }

    @Test
    fun `Load dialog with no saves can only be cancelled`() {
        // TODO: 1. Set presenter.saveNames = emptyList().
        // TODO: 2. Script an F6 event, then a Tab event (focus can only land on Cancel since
        //          Load is inert), then an Enter event.
        // TODO: 3. Play the view, then verify presenter.calls has no loadGame call and the
        //          dialog is closed.
    }

    @Test
    fun `Exit dialog Yes - Tab to Yes, Enter opens the Save dialog, then saving quits`() {
        // TODO: 1. Script an Escape event (opens the Exit dialog), a Tab event to focus the Yes
        //          button, an Enter event.
        // TODO: 2. Script KeyPress('h') to type a save name, a Tab event, an Enter event.
        // TODO: 3. Play the view with a real TuiPresenterImpl over FakeSaveRepository and a
        //          RecordingAnalyticsService.
        // TODO: 4. Verify saves.saveCalls contains the typed name and analytics.events contains
        //          exit_command_used with save_choice="saved".
    }

    @Test
    fun `Exit dialog Escape cancels and keeps playing`() {
        // TODO: 1. Script an Escape event (opens the Exit dialog), then a second Escape event
        //          (Cancel, once inside it).
        // TODO: 2. Play the view with a RecordingAnalyticsService.
        // TODO: 3. Verify presenter.calls has no exitGame call, the last frame shows
        //          "Mordovorot", and analytics.events contains dialog_cancelled with
        //          dialog="exit".
    }

    @Test
    fun `startup restore dialog is navigable by keyboard - Down then Enter restores`() {
        // TODO: 1. Seed a FakeSaveRepository with one save so the startup restore dialog opens
        //          automatically.
        // TODO: 2. Script a Down event to select the row, a Tab event to focus Load, an Enter
        //          event.
        // TODO: 3. Play the view with a real TuiPresenterImpl over a FakeBoardModel.
        // TODO: 4. Verify board.calls contains a restoreState call.
    }
}
