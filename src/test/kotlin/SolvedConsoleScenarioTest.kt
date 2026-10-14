import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import board_model.BoardImpl
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
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** GH-66 scenarios: console solved state - final board, Congratulations message, and the
 *  `Start a new game?` prompt. Real BoardImpl + ViewModelImpl + FileSaveRepository (temp dir),
 *  driven through the console view like [MoveCounterScenarioTest]. */
class SolvedConsoleScenarioTest {

    private val nl = System.lineSeparator()
    private val prompt = "Start a new game? [y/N] "

    /** 4x4, row 1 rotated right once - `left 1` solves it. */
    private val oneShiftFromSolved = intArrayOf(3, 0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

    private val scrambled = IntArray(16) { 15 - it }

    private val solvedBoardText = (0 until 4).joinToString("") { r -> (1..4).joinToString("") { "${r * 4 + it}\t" } + nl }

    private class Run(val output: String, val board: BoardImpl, val analytics: RecordingAnalyticsService) {
        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }
    }

    private fun session(
        input: String,
        initial: IntArray = oneShiftFromSolved,
        savesDir: Path = createTempDirectory("mordovorot-gh66"),
        sizeChosenAtLaunch: Boolean = true,
    ): Run = runBlocking {
        val output = ByteArrayOutputStream()
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState(initial) }
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

    /** A solved 4x4 save stored with 3 moves - loading restores that count (GH-62). */
    private fun solvedSave(): Path = createTempDirectory("mordovorot-gh66").also {
        it.resolve("done.save").writeText("4\n${(0 until 16).joinToString(" ")}\n3\n")
    }

    @Test
    fun `solving shows the final board, its move count and the congratulations message`() {
        val run = session("up 2\ndown 2\nleft 1\n")

        assertTrue(
            run.output.endsWith(
                "${solvedBoardText}Moves: 2$nl$nl" +
                    "Congratulations, you solved this puzzle in 2 moves!$nl$prompt",
            ),
            run.output,
        )
    }

    @Test
    fun `a one-move solve uses the singular`() {
        val run = session("left 1\n")

        assertTrue(run.output.contains("in 1 move!"))
        assertFalse(run.output.contains("1 moves"))
    }

    @Test
    fun `accepting a new game asks for the board size and starts a fresh game at that size`() {
        val run = session("left 1\ny\n3\n")

        val afterPrompt = run.output.substringAfter(prompt)
        assertTrue(afterPrompt.startsWith("Board size? [3-5, Enter for 4]: "), afterPrompt)
        assertEquals(3, run.board.squareSide)
        assertTrue(afterPrompt.contains("Moves: 0"))
        assertEquals(listOf(mapOf("decision" to "new_game", "input_method" to "console")), run.events("play_again_decision"))
        assertEquals(
            listOf(mapOf("size" to 3, "trigger" to "play_again", "input_method" to "console")),
            run.events("new_game_size_selected"),
        )
    }

    @Test
    fun `declining a new game ends the session`() {
        for (input in listOf("left 1\nn\nleft 1\n", "left 1\n\nleft 1\n", "left 1\n")) {
            val run = session(input)

            assertTrue(run.output.endsWith(prompt), "input ${input.lines()}: ${run.output}")
            assertEquals(listOf(mapOf("decision" to "quit", "input_method" to "console")), run.events("play_again_decision"))
        }
    }

    @Test
    fun `loading an already-solved save shows congratulations with its saved count and the prompt`() {
        val run = session("load done\n", initial = scrambled, savesDir = solvedSave())

        assertTrue(run.output.endsWith("in 3 moves!$nl$prompt"), run.output)
        assertTrue(run.events("board_solved").isEmpty())
        assertEquals(listOf(mapOf("decision" to "quit", "input_method" to "console")), run.events("play_again_decision"))
    }

    @Test
    fun `restoring an already-solved save at startup goes straight to congratulations`() {
        val run = session("y\n", initial = scrambled, savesDir = solvedSave(), sizeChosenAtLaunch = false)

        assertTrue(
            run.output.endsWith("${solvedBoardText}Moves: 3$nl$nl" + "Congratulations, you solved this puzzle in 3 moves!$nl$prompt"),
            run.output,
        )
    }
}
