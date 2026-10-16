package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import testing.testTuiView
import testing.testViewModel

/** GH-74: the TUI's non-cancellable "Can't be solved" dialog (Repair & load / New game / Exit),
 *  reached from the startup restore and the toolbar Load, plus the Save-before-quitting dialog
 *  its mid-game Exit opens. Keyboard-driven except where a click is the point. */
class TuiViewUnsolvableDialogTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    /** 3x3, tiles 0/2 swapped - unsolvable. Repair swaps positions 0/1. */
    private val oddState = intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8)
    private val repairedState = listOf(1, 2, 0, 3, 4, 5, 6, 7, 8)

    private class Run(val terminal: FakeTerminal, val board: FakeBoardModel, val saves: FakeSaveRepository, val analytics: RecordingAnalyticsService) {
        fun event(name: String) = analytics.events.single { it.name == name }.properties
        fun restored() = board.calls.filter { it.startsWith("restoreState") }
    }

    private fun play(events: List<TerminalEvent>, midGame: Boolean, input: TuiInput = KeyboardInput()): Run = runBlocking {
        val saves = FakeSaveRepository(mutableMapOf("odd" to SavedBoard(3, oddState.copyOf(), 5)))
        val board = FakeBoardModel()
        val analytics = RecordingAnalyticsService()
        val terminal = FakeTerminal(events.toMutableList(), terminalSize)
        testTuiView(terminal, analytics, input, viewModel = testViewModel(board, saves, analytics, sizeChosenAtLaunch = midGame)).play()
        Run(terminal, board, saves, analytics)
    }

    /** Startup restore dialog: Tab from the one row to Load, Enter. */
    private val restoreAtStartup = listOf(TerminalEvent.Tab, TerminalEvent.Enter)

    /** F6, then the same Tab + Enter on the Load dialog. */
    private val loadMidGame = listOf(TerminalEvent.FunctionKey(6), TerminalEvent.Tab, TerminalEvent.Enter)

    @Test
    fun `restoring an unsolvable save at startup opens the dialog, and Repair loads the fixed board`() {
        val run = play(restoreAtStartup + TerminalEvent.Enter, midGame = false)

        assertTrue(run.terminal.frames.any { it.contains("Can't be solved") && it.contains("Repair & load") })
        assertEquals(listOf("restoreState($repairedState)"), run.restored())
        assertEquals(repairedState, run.saves.load("odd")!!.state.toList())
        assertEquals(mapOf("opened_from" to "startup"), run.event("screen_unsolvable_dialog"))
        assertEquals("restored", run.event("startup_restore_decision")["decision"])
    }

    @Test
    fun `Exit at startup quits without a save prompt and restores the terminal`() {
        val run = play(restoreAtStartup + listOf(TerminalEvent.Tab, TerminalEvent.Tab, TerminalEvent.Enter), midGame = false)

        assertTrue(run.terminal.restored)
        assertTrue(run.restored().isEmpty())
        assertTrue(run.terminal.frames.none { it.contains("Save before quitting?") })
        assertEquals("quit", run.event("startup_restore_decision")["decision"])
    }

    @Test
    fun `Escape is ignored - the dialog stays until a choice is made`() {
        val run = play(restoreAtStartup + listOf(TerminalEvent.Escape, TerminalEvent.Enter), midGame = false)

        assertEquals(listOf("restoreState($repairedState)"), run.restored())
        assertTrue(run.analytics.events.none { it.name == "dialog_cancelled" })
    }

    @Test
    fun `end of input at the dialog counts as Exit`() {
        val run = play(restoreAtStartup, midGame = false)

        assertEquals("exit", run.event("unsolvable_save_resolved")["choice"])
        assertEquals("quit", run.event("startup_restore_decision")["decision"])
    }

    @Test
    fun `New game from a toolbar Load opens the size dialog with opened_from unsolvable_save`() {
        // Size dialog focus starts on its first button, 3x3.
        val run = play(loadMidGame + listOf(TerminalEvent.Tab, TerminalEvent.Enter, TerminalEvent.Enter), midGame = true)

        assertEquals(mapOf("opened_from" to "toolbar"), run.event("screen_unsolvable_dialog"))
        assertEquals("unsolvable_save", run.analytics.events.last { it.name == "screen_size_dialog" }.properties["opened_from"])
        assertTrue(run.board.calls.contains("newGame(3)"), run.board.calls.toString())
        assertEquals("unsolvable_save", run.event("new_game_size_selected")["trigger"])
    }

    @Test
    fun `Exit from a toolbar Load asks whether to save before quitting`() {
        val run = play(
            loadMidGame + listOf(
                TerminalEvent.Tab, TerminalEvent.Tab, TerminalEvent.Enter, // Exit
                TerminalEvent.Tab, TerminalEvent.Enter, // No
            ),
            midGame = true,
        )

        assertTrue(run.terminal.frames.any { it.contains("Save before quitting?") })
        assertEquals(1, run.analytics.events.count { it.name == "screen_exit_dialog" })
        assertEquals("declined", run.event("exit_command_used")["save_choice"])
        assertEquals("exit", run.event("unsolvable_save_resolved")["choice"])
    }

    @Test
    fun `Save before quitting can't be cancelled and Yes goes on to the save name`() {
        val run = play(
            loadMidGame + listOf(
                TerminalEvent.Tab, TerminalEvent.Tab, TerminalEvent.Enter, // Exit
                TerminalEvent.Escape, // ignored
                TerminalEvent.Enter, // Yes
                TerminalEvent.KeyPress('k'), TerminalEvent.Tab, TerminalEvent.Enter, // save as "k"
            ),
            midGame = true,
        )

        assertTrue(run.saves.saveCalls.any { it.first == "k" })
        assertEquals("saved", run.event("exit_command_used")["save_choice"])
        assertTrue(run.analytics.events.none { it.name == "dialog_cancelled" })
    }

    @Test
    fun `end of input at Save before quitting declines saving`() {
        val run = play(loadMidGame + listOf(TerminalEvent.Tab, TerminalEvent.Tab, TerminalEvent.Enter), midGame = true)

        assertEquals("declined", run.event("exit_command_used")["save_choice"])
    }

    @Test
    fun `a mouse click on Repair & load repairs the save`() {
        val dialog = Dialog(
            Dialog.Kind.UNSOLVABLE,
            "Can't be solved",
            message = "'odd (3x3)' can't be solved by any sequence of shifts.",
            buttons = listOf(DialogButtonSpec("repair", "Repair & load"), DialogButtonSpec("new", "New game"), DialogButtonSpec("exit", "Exit")),
        )
        val layout = DialogLayout(dialog, terminalSize)
        val repair = layout.buttons().first { it.target == HitTarget.DialogButton("repair") }
        val loadDialog = DialogLayout(
            Dialog(
                Dialog.Kind.LOAD,
                "Restore a saved game?",
                listItems = listOf("odd (3x3) unsolvable"),
                selectedIndex = 0,
                buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
            ),
            terminalSize,
        )
        val load = loadDialog.buttons().first { it.target == HitTarget.DialogButton("load") }

        val run = play(
            listOf(TerminalEvent.MouseClick(load.x, loadDialog.buttonsRow()), TerminalEvent.MouseClick(repair.x, layout.buttonsRow())),
            midGame = false,
            input = MouseInput(),
        )

        assertEquals(listOf("restoreState($repairedState)"), run.restored())
    }

    @Test
    fun `a toolbar Exit still asks only once`() {
        val run = play(listOf(TerminalEvent.Escape, TerminalEvent.Tab, TerminalEvent.Enter), midGame = true)

        assertEquals(1, run.analytics.events.count { it.name == "screen_exit_dialog" })
        assertEquals("declined", run.event("exit_command_used")["save_choice"])
    }
}
