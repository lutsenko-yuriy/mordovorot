import board_model.BoardImpl
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import storage.FileSaveRepository
import testing.FakeMordieSearchFactory
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import view.tui.BoardLayout
import view.tui.TerminalEvent
import view.tui.TerminalEvent.Enter
import view.tui.TerminalEvent.Tab
import view.tui.TerminalSize

/** GH-79: GH-74's console unsolvable-save scenarios rewritten for the TUI - real BoardImpl +
 *  ViewModelImpl + FileSaveRepository (temp dir), driven through FakeTerminal in keyboard mode
 *  (Tab/Enter reach every dialog button). Same seeds: `odd.save` 3x3 `2 1 0 3 4 5 6 7 8`
 *  (unsolvable, 5 moves; repair gives `1 2 0 3 4 5 6 7 8`), `even.save` 4x4. */
class UnsolvableSaveScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private class Run(val terminal: FakeTerminal, val board: BoardImpl, val analytics: RecordingAnalyticsService) {
        fun event(name: String) = analytics.events.single { it.name == name }.properties
    }

    private fun session(events: List<TerminalEvent>, savesDir: Path, sizeChosenAtLaunch: Boolean = false): Run = runBlocking {
        val terminal = FakeTerminal(events.toMutableList(), terminalSize)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4)
        testGameSession(
            initialMode = InputMode.KEYBOARD,
            board = board,
            saves = FileSaveRepository(savesDir),
            analytics = analytics,
            sizeChosenAtLaunch = sizeChosenAtLaunch,
            terminalFactory = { terminal },
            mordie = Mordie(FakeMordieSearchFactory()),
        ).run()
        Run(terminal, board, analytics)
    }

    private fun savesDir(vararg files: Pair<String, String>): Path = createTempDirectory("mordovorot-gh74").also { dir ->
        for ((name, content) in files) dir.resolve("$name.save").writeText(content)
    }

    private val oddSave = "odd" to "3\n2 1 0 3 4 5 6 7 8\n5\n"
    private val evenSave = "even" to "4\n1 0 ${(2 until 16).joinToString(" ")}\n0\n"

    /** Startup restore dialog: Tab from the one row to Load, Enter. */
    private val restoreAtStartup = listOf(Tab, Enter)

    /** F6, then the same Tab + Enter on the Load dialog. */
    private val loadMidGame = listOf(TerminalEvent.FunctionKey(6), Tab, Enter)

    // Can't-be-solved dialog: focus starts on Repair & load.
    private val chooseRepair = listOf(Enter)
    private val chooseNewGame = listOf(Tab, Enter)
    private val chooseExit = listOf(Tab, Tab, Enter)

    private fun Run.anyFrame(predicate: (String) -> Boolean) = terminal.frames.any(predicate)

    /** The first frame drawn after the Can't-be-solved dialog closed. */
    private fun Run.frameAfterUnsolvableDialog(): String =
        terminal.frames[terminal.frames.indexOfLast { it.contains("Can't be solved") } + 1]

    private fun movesShown(frame: String, side: Int): String {
        val rows = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
        return rows[BoardLayout(terminalSize, side).movesRow].trim()
    }

    @Test
    fun `an unsolvable save is marked in the startup restore dialog`() {
        val run = session(emptyList(), savesDir(oddSave))

        assertTrue(run.anyFrame { it.contains("Restore a saved game?") && it.contains("odd (3x3) unsolvable") })
    }

    @Test
    fun `a 4x4 save is never marked unsolvable`() {
        val run = session(emptyList(), savesDir(evenSave))

        assertTrue(run.anyFrame { it.contains("even (4x4)") })
        assertFalse(run.anyFrame { it.contains("unsolvable") })
    }

    @Test
    fun `restoring an unsolvable save at startup opens the Can't-be-solved dialog`() {
        val run = session(restoreAtStartup, savesDir(oddSave))

        assertTrue(
            run.anyFrame {
                it.contains("Can't be solved") && it.contains("Repair & load") && it.contains("New game") && it.contains("Exit")
            },
        )
    }

    @Test
    fun `Repair & load at startup loads the fixed board and rewrites the save`() {
        val dir = savesDir(oddSave)

        val run = session(restoreAtStartup + chooseRepair, dir)

        assertEquals(listOf(1, 2, 0, 3, 4, 5, 6, 7, 8), run.board.boardArray.toList())
        assertEquals("Moves: 5", movesShown(run.terminal.frames.last(), side = 3))
        assertEquals("3\n1 2 0 3 4 5 6 7 8\n5\n", dir.resolve("odd.save").readText())
        assertEquals("repair", run.event("unsolvable_save_resolved")["choice"])
    }

    @Test
    fun `New game at startup opens the size picker`() {
        val run = session(restoreAtStartup + chooseNewGame, savesDir(oddSave))

        assertTrue(run.frameAfterUnsolvableDialog().contains("New game size"))
        assertEquals("new_game", run.event("unsolvable_save_resolved")["choice"])
    }

    @Test
    fun `Exit at startup quits without asking to save`() {
        val run = session(restoreAtStartup + chooseExit, savesDir(oddSave))

        assertTrue(run.terminal.restored)
        assertFalse(run.anyFrame { it.contains("Save before quitting?") })
        assertEquals("quit", run.event("startup_restore_decision")["decision"])
    }

    @Test
    fun `loading an unsolvable save mid-game and choosing New game opens the size picker`() {
        val run = session(loadMidGame + chooseNewGame, savesDir(oddSave), sizeChosenAtLaunch = true)

        assertTrue(run.frameAfterUnsolvableDialog().contains("New game size"))
        assertEquals("command", run.event("unsolvable_save_resolved")["trigger"])
    }

    @Test
    fun `exiting from a mid-game unsolvable load runs the Save-before-quitting dialog`() {
        val run = session(loadMidGame + chooseExit, savesDir(oddSave), sizeChosenAtLaunch = true)

        val exitFrame = run.terminal.frames.last()
        assertTrue(exitFrame.contains("Save before quitting?"))
        assertTrue(exitFrame.contains("Yes") && exitFrame.contains("No"))
        assertFalse(exitFrame.contains("Cancel"))
        assertEquals("declined", run.event("exit_command_used")["save_choice"])
    }
}
