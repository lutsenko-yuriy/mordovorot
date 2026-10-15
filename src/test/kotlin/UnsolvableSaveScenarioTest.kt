import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import board_model.BoardImpl
import board_model.Solvability
import kotlinx.coroutines.runBlocking
import storage.FileSaveRepository
import testing.RecordingAnalyticsService
import view.View
import view.ViewImpl
import viewmodel.ModeSwitcherImpl
import viewmodel.ViewModelImpl
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** GH-74 scenarios: unsolvable saves on odd board sizes - the `unsolvable` mark in listings and
 *  the repair / new game / exit choice on load. Real BoardImpl + ViewModelImpl +
 *  FileSaveRepository (temp dir), driven through the console view like [SolvedConsoleScenarioTest].
 *
 *  Seeds:
 *  - `odd.save`: 3x3, state `2 1 0 3 4 5 6 7 8` (tiles 0/2 swapped - odd, unsolvable), 5 moves.
 *    Repair swaps positions 0/1, giving `1 2 0 3 4 5 6 7 8` (solvable, not solved).
 *  - `even.save`: 4x4, state `1 0 2 3 ... 15` (every 4x4 board is solvable). */
class UnsolvableSaveScenarioTest {

    private class Run(val output: String, val board: BoardImpl, val analytics: RecordingAnalyticsService)

    private fun session(input: String, savesDir: Path, sizeChosenAtLaunch: Boolean = false): Run = runBlocking {
        val output = ByteArrayOutputStream()
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4)
        val reader = BufferedReader(StringReader(input))
        GameSession(
            initialMode = InputMode.CONSOLE,
            board = board,
            saves = FileSaveRepository(savesDir),
            analytics = analytics,
            sizeChosenAtLaunch = sizeChosenAtLaunch,
            buildView = { m, b, s, a: AnalyticsService, restoreDone, sizeAtLaunch ->
                val decorated = InputMethodAnalyticsService(a, m.name.lowercase())
                val viewModel = ViewModelImpl(b, s, decorated, restoreDone, sizeAtLaunch)
                val switcher = { v: View -> ModeSwitcherImpl(v, m, a, hasInteractiveTerminal = { true }) }
                ViewImpl.create(reader, PrintStream(output), switcher, viewModel)
            },
        ).run()
        Run(output.toString(), board, analytics)
    }

    private fun savesDir(vararg files: Pair<String, String>): Path = createTempDirectory("mordovorot-gh74").also { dir ->
        for ((name, content) in files) dir.resolve("$name.save").writeText(content)
    }

    private val oddSave = "odd" to "3\n2 1 0 3 4 5 6 7 8\n5\n"
    private val evenSave = "even" to "4\n1 0 ${(2 until 16).joinToString(" ")}\n0\n"

    @Test
    fun `an unsolvable save is marked in the startup restore prompt`() {
        val run = session("", savesDir(oddSave))

        assertTrue(run.output.contains("Restore save 'odd (3x3) unsolvable'? [y/N] "), run.output)
    }

    @Test
    fun `a 4x4 save is never marked unsolvable`() {
        val run = session("", savesDir(evenSave))

        assertTrue(run.output.contains("Restore save 'even (4x4)'? [y/N] "), run.output)
        assertFalse(run.output.contains("unsolvable"), run.output)
    }

    private val unsolvableMessage = "'odd (3x3)' can't be solved - no sequence of shifts reaches the solved board."
    private val choicePrompt = "Repair and load (r), start a new game (n), or exit (e)? "
    private val sizePrompt = "Board size? [3-5, Enter for 4]: "

    private fun Run.event(name: String) = analytics.events.single { it.name == name }.properties

    private fun Path.state(name: String) = resolve("$name.save").readText()

    @Test
    fun `restoring an unsolvable save at startup offers repair, new game or exit`() {
        val run = session("y\n", savesDir(oddSave))

        assertTrue(run.output.contains("$unsolvableMessage\n$choicePrompt"), run.output)
    }

    @Test
    fun `repair at startup loads the fixed board and rewrites the save`() {
        val dir = savesDir(oddSave)

        val run = session("y\nr\n", dir)

        assertTrue(run.output.contains("Repaired 'odd' (two tiles swapped) and loaded it."), run.output)
        assertEquals(3, run.board.squareSide)
        assertTrue(Solvability.isSolvable(run.board.boardArray))
        assertTrue(run.output.contains("Moves: 5"), run.output)
        assertEquals("3\n1 2 0 3 4 5 6 7 8\n5\n", dir.state("odd"))
        assertEquals(mapOf("trigger" to "startup_prompt", "result" to "unsolvable", "input_method" to "console"), run.event("load_command_used"))
        assertEquals(
            mapOf("choice" to "repair", "trigger" to "startup_prompt", "board_size" to 3, "repair_saved" to true, "input_method" to "console"),
            run.event("unsolvable_save_resolved"),
        )
        assertEquals("restored", run.event("startup_restore_decision")["decision"])
    }

    @Test
    fun `new game at startup shows the board-size prompt`() {
        val dir = savesDir(oddSave)

        val run = session("y\nn\n5\n", dir)

        assertTrue(run.output.contains("$choicePrompt$sizePrompt"), run.output)
        assertEquals(5, run.board.squareSide)
        assertTrue(Solvability.isSolvable(run.board.boardArray))
        assertEquals(oddSave.second, dir.state("odd"))
        assertEquals("new_game", run.event("unsolvable_save_resolved")["choice"])
        assertEquals("new_game", run.event("startup_restore_decision")["decision"])
    }

    @Test
    fun `exit at startup quits without asking to save`() {
        val run = session("y\ne\n", savesDir(oddSave))

        assertTrue(run.output.endsWith(choicePrompt), run.output)
        assertEquals("exit", run.event("unsolvable_save_resolved")["choice"])
        assertEquals("quit", run.event("startup_restore_decision")["decision"])
    }

    @Test
    fun `loading an unsolvable save mid-game and choosing new game asks for the size`() {
        val run = session("load odd\nn\n3\n", savesDir(oddSave), sizeChosenAtLaunch = true)

        assertTrue(run.output.contains("$unsolvableMessage\n$choicePrompt$sizePrompt"), run.output)
        assertEquals(3, run.board.squareSide)
        assertTrue(Solvability.isSolvable(run.board.boardArray))
        assertEquals("unsolvable_save", run.event("new_game_size_selected")["trigger"])
    }

    @Test
    fun `exiting from a mid-game unsolvable load runs the normal exit flow`() {
        val run = session("load odd\ne\nn\n", savesDir(oddSave), sizeChosenAtLaunch = true)

        assertTrue(run.output.endsWith("${choicePrompt}Save before quitting? [y/N] "), run.output)
        assertEquals("declined", run.event("exit_command_used")["save_choice"])
        assertEquals(
            mapOf("choice" to "exit", "trigger" to "command", "board_size" to 3, "input_method" to "console"),
            run.event("unsolvable_save_resolved"),
        )
    }

    @Test
    fun `an invalid answer re-prompts for repair, new game or exit`() {
        val run = session("load odd\nx\n\nR\n", savesDir(oddSave), sizeChosenAtLaunch = true)

        assertEquals(3, run.output.split(choicePrompt).size - 1, run.output)
        assertEquals(listOf(1, 2, 0, 3, 4, 5, 6, 7, 8), run.board.boardArray.toList())
    }

    @Test
    fun `load accepts the listed form with the unsolvable mark`() {
        val run = session("load odd (3x3) unsolvable\nr\n", savesDir(oddSave), sizeChosenAtLaunch = true)

        assertTrue(run.output.contains(choicePrompt), run.output)
        assertFalse(run.output.contains("No save named"), run.output)
        assertEquals(listOf(1, 2, 0, 3, 4, 5, 6, 7, 8), run.board.boardArray.toList())
    }
}
