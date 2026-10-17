import board_model.BoardImpl
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
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
import testing.testGameSession
import view.tui.BoardLayout
import view.tui.TerminalEvent
import view.tui.TerminalEvent.Backspace
import view.tui.TerminalEvent.Enter
import view.tui.TerminalEvent.Escape
import view.tui.TerminalEvent.FunctionKey
import view.tui.TerminalEvent.KeyPress
import view.tui.TerminalEvent.Tab
import view.tui.TerminalSize

/** GH-99: the Save dialog opens prefilled with the current save name - set by a save, a load
 *  (Load dialog, startup restore, Repair & load), cleared by a new game. Real BoardImpl +
 *  ViewModelImpl + FileSaveRepository (temp dir), driven through FakeTerminal in keyboard mode,
 *  as in UnsolvableSaveScenarioTest. */
class SaveNamePrefillScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private class Run(val terminal: FakeTerminal, val analytics: RecordingAnalyticsService) {
        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }

        /** The last frame showing the Save dialog. */
        fun lastSaveDialog(): String = terminal.frames.last { it.contains("Save game") }
    }

    private fun session(events: List<TerminalEvent>, savesDir: Path, sizeChosenAtLaunch: Boolean = true): Run = runBlocking {
        val terminal = FakeTerminal(events.toMutableList(), terminalSize)
        val analytics = RecordingAnalyticsService()
        testGameSession(
            initialMode = InputMode.KEYBOARD,
            board = BoardImpl(4),
            saves = FileSaveRepository(savesDir),
            analytics = analytics,
            sizeChosenAtLaunch = sizeChosenAtLaunch,
            terminalFactory = { terminal },
            mordie = Mordie(FakeMordieSearchFactory()),
        ).run()
        Run(terminal, analytics)
    }

    private fun savesDir(vararg files: Pair<String, String>): Path = createTempDirectory("mordovorot-gh99").also { dir ->
        for ((name, content) in files) dir.resolve("$name.save").writeText(content)
    }

    private val fooContent = "4\n1 0 ${(2 until 16).joinToString(" ")}\n0\n"
    private val fooSave = "foo" to fooContent
    private val badSave = "bad" to "not a save"
    private val oddSave = "odd" to "3\n2 1 0 3 4 5 6 7 8\n5\n"

    /** F6, select the [index]th of [count] rows, then reach Load and press Enter. Focusing a row
     *  selects it, so Load is reached without passing another row: Tab from the last row, or
     *  BackTab twice (via Cancel) from the first. */
    private fun loadRow(index: Int, count: Int): List<TerminalEvent> {
        val toLoad = when (index) {
            count - 1 -> List(index) { Tab } + Tab
            0 -> listOf(TerminalEvent.BackTab, TerminalEvent.BackTab)
            else -> error("row $index of $count can't reach Load without selecting another row")
        }
        return listOf(FunctionKey(6)) + toLoad + Enter
    }

    /** Enter on the cursored arrow. */
    private val shift = listOf(Enter)

    /** Startup restore dialog with one save: Tab to Load, Enter. */
    private val restoreAtStartup = listOf(Tab, Enter)

    /** Size picker: Enter on the focused size. */
    private val pickSize = listOf(Enter)

    private fun typed(text: String) = text.map { KeyPress(it) }

    @Test
    fun `load then save prefills the loaded name and overwrites it`() {
        val dir = savesDir(fooSave)

        val run = session(loadRow(0, 1) + shift + FunctionKey(5) + Enter, dir)

        val dialog = run.lastSaveDialog()
        assertTrue(dialog.contains("Name: foo_"))
        assertTrue(dialog.contains("already exists"))
        assertTrue(dir.resolve("foo.save").readText() != fooContent, "overwritten with the shifted board")
        assertEquals(listOf("foo.save"), dir.toFile().list()!!.sorted())
        assertEquals(true, run.events("screen_save_dialog").single()["prefilled"])
        assertEquals("prefill", run.events("save_command_used").single()["name_source"])
    }

    @Test
    fun `saving under a new name makes it the next prefill`() {
        val dir = savesDir(fooSave)

        val run = session(loadRow(0, 1) + FunctionKey(5) + typed("x") + Enter + FunctionKey(5), dir)

        assertTrue(dir.resolve("foox.save").exists())
        assertEquals("edited", run.events("save_command_used").single()["name_source"])
        assertTrue(run.lastSaveDialog().contains("Name: foox_"))
    }

    @Test
    fun `startup restore prefills the restored name`() {
        val run = session(restoreAtStartup + FunctionKey(5), savesDir(fooSave), sizeChosenAtLaunch = false)

        assertTrue(run.lastSaveDialog().contains("Name: foo_"))
    }

    @Test
    fun `repair and load prefills the repaired save name`() {
        val run = session(restoreAtStartup + Enter + FunctionKey(5), savesDir(oddSave), sizeChosenAtLaunch = false)

        assertEquals("repair", run.events("unsolvable_save_resolved").single()["choice"])
        assertTrue(run.lastSaveDialog().contains("Name: odd_"))
    }

    @Test
    fun `new game clears the prefill`() {
        val run = session(loadRow(0, 1) + FunctionKey(9) + pickSize + FunctionKey(5) + Escape, savesDir(fooSave))

        assertTrue(run.lastSaveDialog().contains("Name: _"))
        assertEquals(false, run.events("screen_save_dialog").single()["prefilled"])
    }

    @Test
    fun `new game from the unsolvable dialog clears the prefill`() {
        val chooseNewGame = listOf(Tab, Enter)

        val run = session(loadRow(0, 2) + loadRow(1, 2) + chooseNewGame + pickSize + FunctionKey(5), savesDir(fooSave, oddSave))

        assertEquals("new_game", run.events("unsolvable_save_resolved").single()["choice"])
        assertTrue(run.lastSaveDialog().contains("Name: _"))
    }

    @Test
    fun `exit flow save dialog is prefilled`() {
        val dir = savesDir(fooSave)

        val run = session(loadRow(0, 1) + shift + Escape + Enter + Enter, dir)

        assertTrue(run.lastSaveDialog().contains("Name: foo_"))
        assertTrue(dir.resolve("foo.save").readText() != fooContent, "overwritten with the shifted board")
        assertEquals("saved", run.events("exit_command_used").single()["save_choice"])
        assertTrue(run.terminal.restored)
    }

    @Test
    fun `exit re-prompt shows the explanation and the overwrite warning together`() {
        val run = session(loadRow(0, 1) + Escape + Enter + typed(" b") + Enter, savesDir(fooSave))

        val reprompt = run.lastSaveDialog()
        assertTrue(reprompt.contains("Name: foo_"))
        assertTrue(reprompt.contains("isn't a usable save name"))
        assertTrue(reprompt.contains("already exists"))
    }

    @Test
    fun `clearing the prefilled field cancels without saving`() {
        val dir = savesDir(fooSave)

        val run = session(loadRow(0, 1) + shift + FunctionKey(5) + List(3) { Backspace } + Enter, dir)

        assertTrue(run.lastSaveDialog().contains("Name: _"))
        assertEquals(fooContent, dir.resolve("foo.save").readText())
        assertTrue(run.events("save_command_used").isEmpty())
    }

    @Test
    fun `switching input mode keeps the prefill`() {
        val saveButton = BoardLayout(terminalSize, 4, modeButtons = listOf(InputMode.KEYBOARD)).saveButtonPosition()

        val run = session(
            loadRow(0, 1) + FunctionKey(7) + TerminalEvent.MouseClick(saveButton.first, saveButton.second),
            savesDir(fooSave),
        )

        assertEquals("mouse", run.events("screen_save_dialog").single()["input_method"])
        assertTrue(run.lastSaveDialog().contains("Name: foo_"))
    }

    @Test
    fun `a failed load leaves the prefill unchanged`() {
        val run = session(loadRow(1, 2) + loadRow(0, 2) + FunctionKey(5), savesDir(fooSave, badSave))

        assertEquals(listOf("success", "error"), run.events("load_command_used").map { it["result"] })
        assertTrue(run.lastSaveDialog().contains("Name: foo_"))
        assertFalse(run.lastSaveDialog().contains("Name: bad_"))
    }
}
