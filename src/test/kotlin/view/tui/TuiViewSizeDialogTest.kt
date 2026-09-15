package view.tui

import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import testing.FakeSaveRepository
import testing.FakeTerminal
import testing.FakeViewModel
import testing.RecordingAnalyticsService
import viewmodel.ViewModelImpl

/**
 * Covers GH-44's TUI size-picker dialog - opened via the `[ New ]` toolbar button, the F9
 * shortcut, or automatically at startup when no `--size` was given and nothing was restored.
 * Stubs only - `implement` fills these in as it builds the dialog (plan comment on GH-44, WU3).
 * Mirrors [TuiViewDialogTest]'s fixtures.
 */
class TuiViewSizeDialogTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    @Test
    fun `Size dialog happy path - clicking a size button starts a new game at that size and tracks new_game_size_selected`(): Unit = runBlocking {
        // TODO: Click the [ New ] toolbar button, then click the 5x5 size button in the dialog.
        // TODO: Verify viewModel.calls shows a newGame call at side 5.
        // TODO: Verify new_game_size_selected {trigger: "toolbar", size: 5} was tracked, dialog closes.
    }

    @Test
    fun `Size dialog Cancel closes without changing the board size and tracks dialog_cancelled`(): Unit = runBlocking {
        // TODO: Open the dialog via [ New ], click Cancel.
        // TODO: Verify no newGame call happened.
        // TODO: Verify dialog_cancelled {dialog: "size"} was tracked.
    }

    @Test
    fun `F9 opens the Size dialog, same as clicking the New toolbar button`(): Unit = runBlocking {
        // TODO: Drive TerminalEvent.FunctionKey(9) instead of a toolbar click.
        // TODO: Verify the Size dialog frame renders, same effect as clicking [ New ].
    }

    @Test
    fun `screen_size_dialog tracks opened_from=toolbar when opened via the New toolbar button`(): Unit = runBlocking {
        // TODO: Click [ New ].
        // TODO: Verify screen_size_dialog {opened_from: "toolbar"} was tracked.
    }

    @Test
    fun `startup - no --size given, restore declined-no saves - the Size dialog appears automatically (opened_from=startup)`(): Unit = runBlocking {
        // TODO: Build a TuiView over a real ViewModelImpl(FakeBoardModel(), FakeSaveRepository(), analytics, sizePromptEnabled = true), no saves.
        // TODO: Run play().
        // TODO: Verify the Size dialog frame appears without a toolbar click.
        // TODO: Verify screen_size_dialog {opened_from: "startup"} was tracked.
    }
}
