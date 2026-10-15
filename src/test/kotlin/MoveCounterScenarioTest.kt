import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import board_model.BoardImpl
import kotlinx.coroutines.runBlocking
import storage.FileSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import view.View
import view.ViewImpl
import view.tui.BoardLayout
import view.tui.TerminalEvent
import view.tui.TerminalSize
import view.tui.TuiView
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
import kotlin.test.assertTrue

/** GH-62 scenarios: real BoardImpl + ViewModelImpl + FileSaveRepository (temp dir), driven
 *  through the console view or the TUI via FakeTerminal. */
class MoveCounterScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    /** 4x4, row 1 rotated right once - `left 1` solves it. */
    private val oneShiftFromSolved = intArrayOf(3, 0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

    /** 4x4, far from solved - no scripted shift sequence below can solve it by accident. */
    private val scrambled = IntArray(16) { 15 - it }

    private class Run(val output: String, val terminal: FakeTerminal, val analytics: RecordingAnalyticsService) {
        /** Every `Moves: N` the console printed, in order. */
        val consoleCounts: List<Int> get() = Regex("Moves: (\\d+)").findAll(output).map { it.groupValues[1].toInt() }.toList()
    }

    /** Runs a [GameSession] in [mode] over a real board and a temp-dir save repository, with
     *  `--size` semantics (no startup prompts). Console reads [input]; the TUI reads [events]. */
    private fun session(
        input: String = "",
        events: List<TerminalEvent> = emptyList(),
        initial: IntArray = scrambled,
        mode: InputMode = InputMode.CONSOLE,
        savesDir: Path = createTempDirectory("mordovorot-gh62"),
    ): Run = runBlocking {
        val output = ByteArrayOutputStream()
        val terminal = FakeTerminal(events.toMutableList(), terminalSize)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState(initial) }
        val reader = BufferedReader(StringReader(input))
        GameSession(
            initialMode = mode,
            board = board,
            saves = FileSaveRepository(savesDir),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            buildView = { m, b, s, a: AnalyticsService, restoreDone, sizeAtLaunch ->
                val decorated = InputMethodAnalyticsService(a, m.name.lowercase())
                val viewModel = ViewModelImpl(b, s, decorated, restoreDone, sizeAtLaunch)
                val switcher = { v: View -> ModeSwitcherImpl(v, m, a, hasInteractiveTerminal = { true }) }
                when (m) {
                    InputMode.CONSOLE -> ViewImpl.create(reader, PrintStream(output), switcher, viewModel)
                    else -> TuiView.create(terminal, decorated, modeSwitcherFactory = switcher, viewModel = viewModel)
                }
            },
        ).run()
        Run(output.toString(), terminal, analytics)
    }

    /** The text on the row directly under the title in a TUI [frame], ANSI escapes stripped. */
    private fun rowUnderTitle(frame: String): String {
        val rows = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
        return rows[BoardLayout(terminalSize, 4).movesRow].trim()
    }

    @Test
    fun `console shows Moves 0 on a fresh game`() {
        val run = session()

        assertEquals(listOf(0), run.consoleCounts)
    }

    @Test
    fun `repeated shifts on the same line count as one move, in either direction`() {
        val run = session("left 1\nleft 1\nleft 1\nright 1\nright 1\n")

        assertEquals(1, run.consoleCounts.last())
    }

    @Test
    fun `switching lines starts a new move, including returning to an earlier line and row vs column`() {
        val run = session("left 1\nright 2\nright 1\nup 1\n")

        assertEquals(listOf(0, 1, 2, 3, 4), run.consoleCounts)
    }

    @Test
    fun `a rejected shift does not change the count`() {
        val run = session("left 1\nleft 9\n")

        assertTrue(run.output.contains("Row 9 is out of range for left."))
        assertEquals(listOf(0, 1, 1), run.consoleCounts)
    }

    @Test
    fun `starting a new game resets the count`() {
        val run = session("left 1\nup 2\nsize 3\n")

        assertEquals(listOf(0, 1, 2, 0), run.consoleCounts)
    }

    @Test
    fun `save then load restores the count, and the next shift starts a new move`() {
        val run = session("left 1\nleft 1\nup 2\nsave s1\nup 2\nload s1\nup 2\n")

        // fresh, left, left, up, save, up (same column), load, up (new move after a load)
        assertEquals(listOf(0, 1, 1, 2, 2, 2, 2, 3), run.consoleCounts)
    }

    @Test
    fun `a save written before move counting loads with Moves 0`() {
        val dir = createTempDirectory("mordovorot-gh62")
        dir.resolve("old.save").writeText("4\n${scrambled.joinToString(" ")}\n")

        val run = session("left 1\nload old\n", savesDir = dir)

        assertEquals(listOf(0, 1, 0), run.consoleCounts)
    }

    @Test
    fun `TUI shows Moves under the title and updates on clicks, staying under Congratulations once solved`() {
        val layout = BoardLayout(terminalSize, 4)
        val click = { p: Pair<Int, Int> -> TerminalEvent.MouseClick(p.first, p.second) }
        val run = session(
            events = listOf(
                click(layout.upArrowPosition(1)),
                click(layout.downArrowPosition(1)),
                click(layout.leftArrowPosition(0)),
            ),
            initial = oneShiftFromSolved,
            mode = InputMode.MOUSE,
        )

        assertEquals(listOf("Moves: 0", "Moves: 1", "Moves: 1", "Moves: 2"), run.terminal.frames.take(4).map(::rowUnderTitle))
        assertTrue(run.terminal.frames[3].contains("Congratulations ✓"))
    }

    @Test
    fun `switching input mode keeps the count`() {
        val run = session("left 1\nup 1\nmouse\n")

        assertEquals(2, run.consoleCounts.last())
        assertEquals("Moves: 2", rowUnderTitle(run.terminal.frames.first()))
    }

    @Test
    fun `board_solved fires once in console mode with the final count and size`() {
        val run = session("up 2\ndown 2\nleft 1\n", initial = oneShiftFromSolved)

        assertEquals(
            listOf(mapOf("move_count" to 2, "board_size" to 4, "input_method" to "console")),
            run.analytics.events.filter { it.name == "board_solved" }.map { it.properties },
        )
    }

    @Test
    fun `loading an already-solved save does not fire board_solved`() {
        val dir = createTempDirectory("mordovorot-gh62")
        dir.resolve("done.save").writeText("4\n${(0 until 16).joinToString(" ")}\n3\n")

        val run = session("load done\n", savesDir = dir)

        assertTrue(run.output.contains("Loaded 'done'."))
        assertTrue(run.analytics.events.none { it.name == "board_solved" })
    }
}
