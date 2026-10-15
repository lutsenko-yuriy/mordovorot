import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import board_model.BoardImpl
import kotlinx.coroutines.runBlocking
import storage.FileSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import view.ConsoleCommands
import view.View
import view.ViewImpl
import view.tui.KeyboardInput
import view.tui.TerminalEvent
import view.tui.TerminalSize
import view.tui.TuiView
import viewmodel.ModeSwitcherImpl
import viewmodel.ViewModelImpl
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** GH-67 scenarios: console `help` command, startup hint and specific error messages. Real
 *  BoardImpl + ViewModelImpl + FileSaveRepository (temp dir) driven through the console view via
 *  GameSession, like [SolvedConsoleScenarioTest]; 4x4 with the size chosen at launch. */
class ConsoleHelpScenarioTest {

    private val nl = System.lineSeparator()
    private val hint = ConsoleCommands.HELP_HINT

    /** 4x4, far from solved - no scripted shift below can solve it by accident. */
    private val scrambled = IntArray(16) { 15 - it }

    private class Run(val output: String, val board: BoardImpl, val analytics: RecordingAnalyticsService) {
        /** Every `Moves: N` the console printed, in order - one per board shown. */
        val boardCounts: List<Int> get() = Regex("Moves: (\\d+)").findAll(output).map { it.groupValues[1].toInt() }.toList()

        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }
    }

    private fun session(input: String, events: List<TerminalEvent> = emptyList()): Run = runBlocking {
        val output = ByteArrayOutputStream()
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState(scrambled) }
        val reader = BufferedReader(StringReader(input))
        val terminal = FakeTerminal(events.toMutableList(), TerminalSize(columns = 80, rows = 40))
        GameSession(
            initialMode = InputMode.CONSOLE,
            board = board,
            saves = FileSaveRepository(createTempDirectory("mordovorot-gh67")),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            buildView = { m, b, s, a: AnalyticsService, restoreDone, sizeAtLaunch ->
                val decorated = InputMethodAnalyticsService(a, m.name.lowercase())
                val viewModel = ViewModelImpl(b, s, decorated, restoreDone, sizeAtLaunch)
                val switcher = { v: View -> ModeSwitcherImpl(v, m, a, hasInteractiveTerminal = { true }) }
                when (m) {
                    InputMode.CONSOLE -> ViewImpl.create(reader, PrintStream(output), switcher, viewModel, decorated)
                    else -> TuiView.create(terminal, decorated, input = KeyboardInput(), modeSwitcherFactory = switcher, viewModel = viewModel)
                }
            },
        ).run()
        Run(output.toString(), board, analytics)
    }

    private fun hintCount(output: String) = Regex(Regex.escape(hint)).findAll(output).count()

    private fun rejected(command: String?, reason: String): Map<String, Any?> =
        buildMap {
            put("reason", reason)
            if (command != null) put("command", command)
            put("input_method", "console")
        }

    @Test
    fun `startup prints the help hint right before the first board`() {
        val run = session("")

        assertTrue(run.output.startsWith("$hint${nl}16\t15\t14\t13\t"), run.output)
        assertFalse(run.output.contains("Commands:"))
    }

    @Test
    fun `help lists every command in place of the board`() {
        val run = session("help\nleft 1\n")

        assertTrue(run.output.contains("Moves: 0$nl$nl${ConsoleCommands.helpText(4)}$nl"), run.output)
        assertEquals(listOf(0, 1), run.boardCounts)
        assertEquals(listOf(mapOf("scope" to "all", "input_method" to "console")), run.events("help_command_used"))
    }

    @Test
    fun `help with a command shows only that command's line`() {
        val run = session("help left\nhelp quit\n")

        assertTrue(run.output.contains("Moves: 0$nl$nl  left <row 1-4>       - shift a row left$nl$nl"), run.output)
        assertTrue(run.output.endsWith("  exit, quit           - quit, offering to save first$nl$nl"), run.output)
        assertFalse(run.output.contains("Commands:"))
        assertEquals(
            listOf(
                mapOf("scope" to "command", "command" to "left", "input_method" to "console"),
                mapOf("scope" to "command", "command" to "exit", "input_method" to "console"),
            ),
            run.events("help_command_used"),
        )
    }

    @Test
    fun `help with an unknown topic shows the full list`() {
        val run = session("help lft\n")

        assertTrue(run.output.endsWith("Moves: 0$nl$nl${ConsoleCommands.helpText(4)}$nl$nl"), run.output)
        assertFalse(run.output.contains("Unknown command"))
        assertEquals(listOf(mapOf("scope" to "unknown_topic", "input_method" to "console")), run.events("help_command_used"))
    }

    @Test
    fun `unknown command shows the error and hint without the board`() {
        val run = session("lft 1\nleft 1\n")

        assertTrue(run.output.contains("Moves: 0$nl${nl}Unknown command: lft$nl$hint$nl$nl"), run.output)
        assertEquals(listOf(0, 1), run.boardCounts)
        assertEquals(listOf(rejected(null, "unknown_command")), run.events("console_command_rejected"))
    }

    @Test
    fun `empty line shows Empty command and the hint`() {
        val run = session("\n")

        assertTrue(run.output.endsWith("Moves: 0$nl${nl}Empty command$nl$hint$nl"), run.output)
        assertEquals(listOf(rejected(null, "empty")), run.events("console_command_rejected"))
    }

    @Test
    fun `misused known commands show the problem, the usage line and the hint`() {
        val run = session("left\nleft x\nleft 9\nreset now\nsize 9\n")

        val expected = listOf(
            "Missing row number for left. Usage: left <row 1-4>",
            "Row must be a number for left. Usage: left <row 1-4>",
            "Row 9 is out of range for left. Usage: left <row 1-4>",
            "Too many arguments for reset. Usage: reset",
            "Size 9 is out of range for size. Usage: size <3-5>",
        ).joinToString("") { "$it$nl$hint$nl" }
        assertTrue(run.output.endsWith("Moves: 0$nl$nl$expected"), run.output)
        assertEquals(listOf(0), run.boardCounts)
        assertEquals(scrambled.toList(), run.board.boardArray.toList())
        assertEquals(0, run.board.moveCount)
        assertEquals(
            listOf(
                rejected("left", "missing_argument"),
                rejected("left", "not_a_number"),
                rejected("left", "out_of_range"),
                rejected("reset", "too_many_arguments"),
                rejected("size", "out_of_range"),
            ),
            run.events("console_command_rejected"),
        )
    }

    @Test
    fun `usage ranges follow the current board size`() {
        val run = session("size 5\nhelp left\nleft 9\n")

        assertTrue(run.output.contains("  left <row 1-5>       - shift a row left$nl"), run.output)
        assertTrue(run.output.endsWith("Row 9 is out of range for left. Usage: left <row 1-5>$nl$hint$nl"), run.output)
    }

    @Test
    fun `loading an unknown save keeps its existing message without the hint`() {
        val run = session("load nope\n")

        assertTrue(run.output.contains("No save named 'nope'"), run.output)
        assertEquals(1, hintCount(run.output))
        assertEquals(listOf(0, 0), run.boardCounts)
        assertTrue(run.events("console_command_rejected").isEmpty())
    }

    /** D2 on the GH-67 plan: the hint shows on every console start, including a switch back. */
}
