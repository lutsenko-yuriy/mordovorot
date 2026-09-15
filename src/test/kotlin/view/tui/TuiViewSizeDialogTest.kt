package view.tui

import InputMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
 * Mirrors [TuiViewDialogTest]'s fixtures. Tests 1-4 (the toolbar/F9 entry points) drive a real
 * [ViewModelImpl] rather than [FakeViewModel] - `new_game_size_selected` is tracked inside
 * [ViewModelImpl.newGame], not [TuiView] itself, so a call-recording fake can't confirm it fired.
 */
class TuiViewSizeDialogTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    /** Matches [MouseInput.decorateBoard]'s GH-30 mode buttons, same as [TuiViewDialogTest]'s
     *  own helper - the real toolbar always carries them, so a hardcoded position here lines up
     *  with what [TuiView] actually draws/hit-tests. */
    private fun boardLayout(squareSide: Int) =
        BoardLayout(terminalSize, squareSide, arrowsEnabled = true, modeButtons = listOf(InputMode.KEYBOARD, InputMode.CONSOLE))

    private fun sizeDialog() = Dialog(
        Dialog.Kind.SIZE, "New game size",
        buttons = listOf(
            DialogButtonSpec("3", "3x3"), DialogButtonSpec("4", "4x4"),
            DialogButtonSpec("5", "5x5"), DialogButtonSpec("cancel", "Cancel"),
        ),
    )

    @Test
    fun `Size dialog happy path - clicking a size button starts a new game at that size and tracks new_game_size_selected`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val (newX, newY) = boardLayout(board.squareSide).newButtonPosition()
        val dialog = sizeDialog()
        val button5 = DialogLayout(dialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("5") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(newX, newY),
                TerminalEvent.MouseClick(button5.x, DialogLayout(dialog, terminalSize).buttonsRow()),
            ),
            terminalSize = terminalSize,
        )

        TuiView.create(terminal, analytics, viewModel = ViewModelImpl(board, saves, analytics, sizeChosenAtLaunch = true)).play()

        assertTrue(board.calls.contains("newGame(5)"))
        assertTrue(analytics.events.any { it.name == "new_game_size_selected" && it.properties["size"] == 5 && it.properties["trigger"] == "toolbar" })
        assertFalse(terminal.frames.last().contains("New game size"))
    }

    @Test
    fun `Size dialog Cancel closes without changing the board size and tracks dialog_cancelled`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val (newX, newY) = boardLayout(board.squareSide).newButtonPosition()
        val dialog = sizeDialog()
        val cancelButton = DialogLayout(dialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("cancel") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(newX, newY),
                TerminalEvent.MouseClick(cancelButton.x, DialogLayout(dialog, terminalSize).buttonsRow()),
            ),
            terminalSize = terminalSize,
        )

        TuiView.create(terminal, analytics, viewModel = ViewModelImpl(board, saves, analytics, sizeChosenAtLaunch = true)).play()

        assertTrue(board.calls.none { it.startsWith("newGame") })
        assertTrue(analytics.events.any { it.name == "dialog_cancelled" && it.properties["dialog"] == "size" })
    }

    @Test
    fun `F9 opens the Size dialog, same as clicking the New toolbar button`(): Unit = runBlocking {
        val board = FakeBoardModel()
        val saves = FakeSaveRepository()
        val analytics = RecordingAnalyticsService()
        val terminal = FakeTerminal(
            // A fresh Size dialog focuses its first button ("3") - Enter activates it directly,
            // same as KeyboardInputTest's other dialog-focus tests (KeyboardInput doesn't
            // interpret mouse clicks at all, unlike MouseInput).
            events = mutableListOf(TerminalEvent.FunctionKey(9), TerminalEvent.Enter),
            terminalSize = terminalSize,
        )

        TuiView.create(terminal, analytics, input = KeyboardInput(), viewModel = ViewModelImpl(board, saves, analytics, sizeChosenAtLaunch = true)).play()

        assertTrue(terminal.frames.any { it.contains("New game size") })
        assertTrue(board.calls.contains("newGame(3)"))
    }

    @Test
    fun `screen_size_dialog tracks opened_from=toolbar when opened via the New toolbar button`(): Unit = runBlocking {
        val viewModel = FakeViewModel()
        val analytics = RecordingAnalyticsService()
        val (newX, newY) = boardLayout(viewModel.side).newButtonPosition()
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(newX, newY), TerminalEvent.Escape), terminalSize = terminalSize)

        TuiView.create(terminal, analytics, viewModel = viewModel).play()

        assertTrue(analytics.events.any { it.name == "screen_size_dialog" && it.properties["opened_from"] == "toolbar" })
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
