package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import testing.FakeBoardModel
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
        val saves = FakeSaveRepository()
        val board = FakeBoardModel()
        val analytics = RecordingAnalyticsService()
        val dialog = Dialog(
            Dialog.Kind.SIZE,
            "New game size",
            buttons = listOf(
                DialogButtonSpec("3", "3x3"), DialogButtonSpec("4", "4x4"),
                DialogButtonSpec("5", "5x5"), DialogButtonSpec("cancel", "Cancel"),
            ),
        )
        val button5 = DialogLayout(dialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("5") }
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(button5.x, DialogLayout(dialog, terminalSize).buttonsRow())),
            terminalSize = terminalSize,
        )

        TuiView.create(terminal, analytics, viewModel = ViewModelImpl(board, saves, analytics)).play()

        assertTrue(terminal.frames.first().contains("New game size"))
        assertEquals(listOf("newGame(5)"), board.calls)
        assertTrue(analytics.events.any { it.name == "screen_size_dialog" && it.properties["opened_from"] == "startup" })
    }
}
