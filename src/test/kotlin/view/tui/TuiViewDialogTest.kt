package view.tui

import presenter.PresenterImpl
import testing.FakeBoardModel
import testing.FakePresenter
import testing.FakeSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers GH-3's Save/Load/Exit dialogs and the startup restore prompt, plus their analytics.
 * Driven via `FakeTerminal` + `RecordingAnalyticsService`, and either `FakePresenter` (board
 * click dispatch, which doesn't need real presenter logic) or a real `PresenterImpl` over
 * `FakeBoardModel`/`FakeSaveRepository` (the exit and startup-restore flows, which round-trip
 * through the presenter calling back into `TuiView`'s own `View` methods - a `FakePresenter`
 * doesn't do that).
 */
class TuiViewDialogTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private fun view(terminal: FakeTerminal, presenter: FakePresenter, analytics: RecordingAnalyticsService = RecordingAnalyticsService()): TuiView =
        TuiView.create(terminal, analytics) { presenter }

    private fun viewWithRealPresenter(
        terminal: FakeTerminal,
        saves: FakeSaveRepository = FakeSaveRepository(),
        board: FakeBoardModel = FakeBoardModel(),
        analytics: RecordingAnalyticsService = RecordingAnalyticsService(),
    ): TuiView = TuiView.create(terminal, analytics) { v -> PresenterImpl(v, board, saves, analytics) }

    private fun boardLayout(presenter: FakePresenter) = BoardLayout(terminalSize, presenter.side, arrowsEnabled = true)

    @Test
    fun `Save dialog happy path - typed name with no conflict saves and closes`() {
        val presenter = FakePresenter()
        val (saveX, saveY) = boardLayout(presenter).saveButtonPosition()
        // typing "hi" then clicking wherever the Save dialog's Save button lands - the dialog
        // is a fixed layout (DialogLayout), so its position is computed the same way here.
        val dialog = Dialog(Dialog.Kind.SAVE, "Save game", buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")))
        val saveButton = DialogLayout(dialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("save") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(saveX, saveY),
                TerminalEvent.KeyPress('h'),
                TerminalEvent.KeyPress('i'),
                TerminalEvent.MouseClick(saveButton.x, DialogLayout(dialog, terminalSize).buttonsRow()),
            ),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(presenter.calls.contains("saveGame(hi)"))
        assertFalse(terminal.frames.last().contains("Save game"))
    }

    @Test
    fun `Save dialog shows the overwrite warning when the typed name matches an existing save`() {
        val presenter = FakePresenter()
        presenter.existingSaveNames = setOf("hi")
        val (saveX, saveY) = boardLayout(presenter).saveButtonPosition()
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(saveX, saveY),
                TerminalEvent.KeyPress('h'),
                TerminalEvent.KeyPress('i'),
            ),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(terminal.frames.any { it.contains("already exists") })
    }

    @Test
    fun `Save dialog Cancel closes without saving and tracks dialog_cancelled`() {
        val presenter = FakePresenter()
        val analytics = RecordingAnalyticsService()
        val (saveX, saveY) = boardLayout(presenter).saveButtonPosition()
        val dialog = Dialog(Dialog.Kind.SAVE, "Save game", buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")))
        val cancelButton = DialogLayout(dialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("cancel") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(saveX, saveY),
                TerminalEvent.MouseClick(cancelButton.x, DialogLayout(dialog, terminalSize).buttonsRow()),
            ),
            terminalSize = terminalSize,
        )

        view(terminal, presenter, analytics).play()

        assertTrue(presenter.calls.none { it.startsWith("saveGame") })
        assertTrue(analytics.events.any { it.name == "dialog_cancelled" && it.properties["dialog"] == "save" })
    }

    @Test
    fun `Load dialog lists existing saves and Load on a selected row restores the board`() {
        val presenter = FakePresenter()
        presenter.saveNames = listOf("foo", "bar")
        val (loadX, loadY) = boardLayout(presenter).loadButtonPosition()
        val dialog = Dialog(Dialog.Kind.LOAD, "Load game", listItems = listOf("foo", "bar"), buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")))
        val dialogLayout = DialogLayout(dialog, terminalSize)
        val loadButton = dialogLayout.buttons().first { it.target == HitTarget.DialogButton("load") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(loadX, loadY),
                // Row 1 is "bar" - click anywhere in the list row's clickable width.
                TerminalEvent.MouseClick(dialogLayout.left + 2, dialogLayout.listRowPosition(1)),
                TerminalEvent.MouseClick(loadButton.x, dialogLayout.buttonsRow()),
            ),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(presenter.calls.contains("loadGame(bar)"))
    }

    @Test
    fun `Load dialog with no saves shows No saves found and Load is inert`() {
        val presenter = FakePresenter()
        presenter.saveNames = emptyList()
        val (loadX, loadY) = boardLayout(presenter).loadButtonPosition()
        val dialog = Dialog(Dialog.Kind.LOAD, "Load game", buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")))
        val loadButton = DialogLayout(dialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("load") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(loadX, loadY),
                TerminalEvent.MouseClick(loadButton.x, DialogLayout(dialog, terminalSize).buttonsRow()),
            ),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(terminal.frames.any { it.contains("No saves found.") })
        assertTrue(presenter.calls.none { it.startsWith("loadGame") })
    }

    @Test
    fun `Load dialog Cancel closes without loading and tracks dialog_cancelled`() {
        val presenter = FakePresenter()
        presenter.saveNames = listOf("foo")
        val analytics = RecordingAnalyticsService()
        val (loadX, loadY) = boardLayout(presenter).loadButtonPosition()
        val dialog = Dialog(Dialog.Kind.LOAD, "Load game", listItems = listOf("foo"), buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")))
        val cancelButton = DialogLayout(dialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("cancel") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(loadX, loadY),
                TerminalEvent.MouseClick(cancelButton.x, DialogLayout(dialog, terminalSize).buttonsRow()),
            ),
            terminalSize = terminalSize,
        )

        view(terminal, presenter, analytics).play()

        assertTrue(presenter.calls.none { it.startsWith("loadGame") })
        assertTrue(analytics.events.any { it.name == "dialog_cancelled" && it.properties["dialog"] == "load" })
    }

    @Test
    fun `Exit dialog Yes opens the Save dialog then quits after a successful save`() {
        val saves = FakeSaveRepository()
        val board = FakeBoardModel()
        val (exitX, exitY) = BoardLayout(terminalSize, board.SQUARE_SIDE, arrowsEnabled = true).exitButtonPosition()
        val exitDialog = Dialog(
            Dialog.Kind.EXIT, "Save before quitting?",
            buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel")),
        )
        val yesButton = DialogLayout(exitDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("yes") }
        val saveDialog = Dialog(Dialog.Kind.SAVE, "Save game", buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")))
        val saveButton = DialogLayout(saveDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("save") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(exitX, exitY),
                TerminalEvent.MouseClick(yesButton.x, DialogLayout(exitDialog, terminalSize).buttonsRow()),
                TerminalEvent.KeyPress('h'),
                TerminalEvent.MouseClick(saveButton.x, DialogLayout(saveDialog, terminalSize).buttonsRow()),
            ),
            terminalSize = terminalSize,
        )
        val analytics = RecordingAnalyticsService()

        viewWithRealPresenter(terminal, saves, board, analytics).play()

        assertTrue(saves.saveCalls.any { it.first == "h" })
        assertTrue(analytics.events.any { it.name == "exit_command_used" && it.properties["save_choice"] == "saved" })
    }

    @Test
    fun `Exit dialog No quits without saving`() {
        val saves = FakeSaveRepository()
        val board = FakeBoardModel()
        val (exitX, exitY) = BoardLayout(terminalSize, board.SQUARE_SIDE, arrowsEnabled = true).exitButtonPosition()
        val exitDialog = Dialog(
            Dialog.Kind.EXIT, "Save before quitting?",
            buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel")),
        )
        val noButton = DialogLayout(exitDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("no") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(exitX, exitY),
                TerminalEvent.MouseClick(noButton.x, DialogLayout(exitDialog, terminalSize).buttonsRow()),
            ),
            terminalSize = terminalSize,
        )
        val analytics = RecordingAnalyticsService()

        viewWithRealPresenter(terminal, saves, board, analytics).play()

        assertTrue(saves.saveCalls.isEmpty())
        assertTrue(analytics.events.any { it.name == "exit_command_used" && it.properties["save_choice"] == "declined" })
    }

    @Test
    fun `Exit dialog Cancel closes the dialog and exitGame is never called`() {
        val presenter = FakePresenter()
        val analytics = RecordingAnalyticsService()
        val (exitX, exitY) = boardLayout(presenter).exitButtonPosition()
        val exitDialog = Dialog(
            Dialog.Kind.EXIT, "Save before quitting?",
            buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel")),
        )
        val cancelButton = DialogLayout(exitDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("cancel") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(exitX, exitY),
                TerminalEvent.MouseClick(cancelButton.x, DialogLayout(exitDialog, terminalSize).buttonsRow()),
            ),
            terminalSize = terminalSize,
        )

        view(terminal, presenter, analytics).play()

        assertTrue(presenter.calls.none { it == "exitGame" })
        assertTrue(terminal.frames.last().contains("Mordovorot"))
        assertTrue(analytics.events.any { it.name == "dialog_cancelled" && it.properties["dialog"] == "exit" })
    }

    @Test
    fun `startup restore dialog appears when saves exist and Cancel starts a new game`() {
        val saves = FakeSaveRepository(mutableMapOf("foo" to storage.SavedBoard(4, IntArray(16) { it })))
        val board = FakeBoardModel()
        val dialog = Dialog(Dialog.Kind.LOAD, "Restore a saved game?", listItems = listOf("foo"), buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")))
        val cancelButton = DialogLayout(dialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("cancel") }
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(cancelButton.x, DialogLayout(dialog, terminalSize).buttonsRow())),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, saves, board).play()

        assertTrue(terminal.frames.first().contains("Restore a saved game?"))
        assertTrue(board.calls.none { it.startsWith("restoreState") })
        assertTrue(terminal.frames.last().contains("Mordovorot"))
    }

    @Test
    fun `screen views fire with the right opened_from for each dialog entry point`() {
        val presenter = FakePresenter()
        val analytics = RecordingAnalyticsService()
        val (saveX, saveY) = boardLayout(presenter).saveButtonPosition()
        val (loadX, loadY) = boardLayout(presenter).loadButtonPosition()
        val (exitX, exitY) = boardLayout(presenter).exitButtonPosition()
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(saveX, saveY),
                TerminalEvent.Escape,
                TerminalEvent.MouseClick(loadX, loadY),
                TerminalEvent.Escape,
                TerminalEvent.MouseClick(exitX, exitY),
                TerminalEvent.Escape,
            ),
            terminalSize = terminalSize,
        )

        view(terminal, presenter, analytics).play()

        assertTrue(analytics.events.any { it.name == "screen_save_dialog" && it.properties["opened_from"] == "toolbar" })
        assertTrue(analytics.events.any { it.name == "screen_load_dialog" && it.properties["opened_from"] == "toolbar" })
        assertTrue(analytics.events.any { it.name == "screen_exit_dialog" })
    }

    // --- Audit round 1 on PR #24: presenter messages (save/load confirmations, the
    // failed-exit-save explanation) were collected via showMessage but never actually shown -
    // TuiView had no board-level status line to render them on. ---

    @Test
    fun `a successful toolbar Save shows the presenter's confirmation message on the board afterward`() {
        val saves = FakeSaveRepository()
        val board = FakeBoardModel()
        val (saveX, saveY) = BoardLayout(terminalSize, board.SQUARE_SIDE, arrowsEnabled = true).saveButtonPosition()
        val saveDialog = Dialog(Dialog.Kind.SAVE, "Save game", buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")))
        val saveButton = DialogLayout(saveDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("save") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(saveX, saveY),
                TerminalEvent.KeyPress('h'),
                TerminalEvent.MouseClick(saveButton.x, DialogLayout(saveDialog, terminalSize).buttonsRow()),
            ),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, saves, board).play()

        assertTrue(terminal.frames.last().contains("Saved as 'h'."))
    }

    @Test
    fun `a failed save during Exit Yes shows why the quit was aborted, and the session keeps running`() {
        val saves = FakeSaveRepository()
        saves.saveException = RuntimeException("disk full")
        val board = FakeBoardModel()
        val (exitX, exitY) = BoardLayout(terminalSize, board.SQUARE_SIDE, arrowsEnabled = true).exitButtonPosition()
        val exitDialog = Dialog(
            Dialog.Kind.EXIT, "Save before quitting?",
            buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel")),
        )
        val yesButton = DialogLayout(exitDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("yes") }
        val saveDialog = Dialog(Dialog.Kind.SAVE, "Save game", buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")))
        val saveButton = DialogLayout(saveDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("save") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(exitX, exitY),
                TerminalEvent.MouseClick(yesButton.x, DialogLayout(exitDialog, terminalSize).buttonsRow()),
                TerminalEvent.KeyPress('h'),
                TerminalEvent.MouseClick(saveButton.x, DialogLayout(saveDialog, terminalSize).buttonsRow()),
                TerminalEvent.EndOfInput,
            ),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, saves, board).play()

        assertTrue(terminal.frames.any { it.contains("Not quitting") })
        assertTrue(terminal.frames.last().contains("Mordovorot"))
    }

    @Test
    fun `a board status message does not leak into a dialog opened afterward`() {
        val saves = FakeSaveRepository(mutableMapOf("foo" to storage.SavedBoard(4, IntArray(16) { it })))
        val board = FakeBoardModel()
        val (loadX, loadY) = BoardLayout(terminalSize, board.SQUARE_SIDE, arrowsEnabled = true).loadButtonPosition()
        val (saveX, saveY) = BoardLayout(terminalSize, board.SQUARE_SIDE, arrowsEnabled = true).saveButtonPosition()
        val loadDialog = Dialog(Dialog.Kind.LOAD, "Load game", listItems = listOf("foo"), buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")))
        val loadButton = DialogLayout(loadDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("load") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(loadX, loadY),
                TerminalEvent.MouseClick(loadButton.x, DialogLayout(loadDialog, terminalSize).buttonsRow()),
                TerminalEvent.MouseClick(saveX, saveY),
            ),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, saves, board).play()

        // Frame 0: initial board. Frame 1: Load dialog open. Frame 2: board repaint right after
        // Load closes - this is where the message must show up. Frame 3: the freshly-opened
        // Save dialog - it must NOT inherit the stale board message as its own.
        assertTrue(terminal.frames[2].contains("Loaded 'foo'."))
        assertTrue(terminal.frames[2].contains(TITLE_UNSOLVED))
        assertTrue(terminal.frames[3].contains("Save game"))
        assertFalse(terminal.frames[3].contains("Loaded 'foo'."))
    }

    @Test
    fun `pressing Enter with an empty name in the Save dialog cancels instead of looping forever`() {
        val presenter = FakePresenter()
        val (saveX, saveY) = boardLayout(presenter).saveButtonPosition()
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(saveX, saveY), TerminalEvent.Enter),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(presenter.calls.none { it.startsWith("saveGame") })
    }

    @Test
    fun `confirmRestore renders the presenter-provided save name even if listSaves would disagree`() {
        val presenter = FakePresenter()
        presenter.saveNames = listOf("stale-name")
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.Escape), terminalSize = terminalSize)

        view(terminal, presenter).confirmRestore("real-name")

        assertTrue(terminal.frames.first().contains("real-name"))
        assertFalse(terminal.frames.first().contains("stale-name"))
    }

    // --- Round 2 audit findings on PR #24. ---

    @Test
    fun `clicking Save with an empty name cancels, same as pressing Enter on an empty name`() {
        val presenter = FakePresenter()
        val (saveX, saveY) = boardLayout(presenter).saveButtonPosition()
        val dialog = Dialog(Dialog.Kind.SAVE, "Save game", buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")))
        val saveButton = DialogLayout(dialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("save") }
        val terminal = FakeTerminal(
            events = mutableListOf(TerminalEvent.MouseClick(saveX, saveY), TerminalEvent.MouseClick(saveButton.x, DialogLayout(dialog, terminalSize).buttonsRow())),
            terminalSize = terminalSize,
        )

        view(terminal, presenter).play()

        assertTrue(presenter.calls.none { it.startsWith("saveGame") })
    }

    @Test
    fun `the exit flow's invalid-name explanation stays visible past the user's first corrective keystroke`() {
        val saves = FakeSaveRepository()
        val board = FakeBoardModel()
        val (exitX, exitY) = BoardLayout(terminalSize, board.SQUARE_SIDE, arrowsEnabled = true).exitButtonPosition()
        val exitDialog = Dialog(
            Dialog.Kind.EXIT, "Save before quitting?",
            buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel")),
        )
        val yesButton = DialogLayout(exitDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("yes") }
        val saveDialog = Dialog(Dialog.Kind.SAVE, "Save game", buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")))
        val saveButton = DialogLayout(saveDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("save") }
        // The re-prompted dialog is wider (it carries the invalid-name explanation as its
        // message), which shifts the Save button's x - compute its real position against a
        // Dialog matching that message, or the click below misses the button entirely.
        val invalidNameMessage = "'a/b' isn't a usable save name (no spaces, path separators, or '..') - " +
            "try again, or press Enter to skip saving."
        val reprompDialog = saveDialog.copy(message = invalidNameMessage)
        val reprompSaveButton = DialogLayout(reprompDialog, terminalSize).buttons().first { it.target == HitTarget.DialogButton("save") }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(exitX, exitY),
                TerminalEvent.MouseClick(yesButton.x, DialogLayout(exitDialog, terminalSize).buttonsRow()),
                // "a/b" is an invalid name (path separator) - triggers PresenterImpl's
                // re-prompt with an explanatory showMessage().
                TerminalEvent.KeyPress('a'),
                TerminalEvent.KeyPress('/'),
                TerminalEvent.KeyPress('b'),
                TerminalEvent.MouseClick(saveButton.x, DialogLayout(saveDialog, terminalSize).buttonsRow()),
                // One corrective keystroke on the re-prompted dialog - the explanation must
                // still be on screen after this, not just on the frame right after re-prompting.
                TerminalEvent.KeyPress('h'),
                TerminalEvent.MouseClick(reprompSaveButton.x, DialogLayout(reprompDialog, terminalSize).buttonsRow()),
            ),
            terminalSize = terminalSize,
        )

        viewWithRealPresenter(terminal, saves, board).play()

        val explanationFrames = terminal.frames.filter { it.contains("isn't a usable save name") }
        assertTrue(explanationFrames.size >= 2)
        assertTrue(saves.saveCalls.any { it.first == "h" })
    }

    @Test
    fun `dialog content is truncated to the box width instead of overflowing the border on a narrow terminal`() {
        val narrow = TerminalSize(columns = 40, rows = 24)
        val presenter = FakePresenter()
        presenter.existingSaveNames = setOf("somesave")
        val (saveX, saveY) = BoardLayout(narrow, presenter.side, arrowsEnabled = true).saveButtonPosition()
        val terminal = FakeTerminal(
            events = (listOf(TerminalEvent.MouseClick(saveX, saveY)) + "somesave".map { TerminalEvent.KeyPress(it) }).toMutableList(),
            terminalSize = narrow,
        )

        view(terminal, presenter).play()

        // The full warning ("'somesave' already exists - it will be overwritten.") is 51 chars -
        // wider than the 40-column terminal - so the frame showing it (the one drawn right
        // after the last keystroke, before EOF closes the dialog) must show it truncated (an
        // ellipsis), not whole, and every rendered line must still fit the terminal's width.
        val dialogFrame = terminal.frames[terminal.frames.size - 2]
        assertTrue(dialogFrame.contains("…"))
        // Row 0 carries the leading clear-screen escape sequence, not board content - every
        // other row is a fixed-width canvas row and must fit the terminal exactly.
        assertTrue(dialogFrame.lines().drop(1).all { it.length <= narrow.columns })
    }
}
