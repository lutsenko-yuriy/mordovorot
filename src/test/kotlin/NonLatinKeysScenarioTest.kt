import board_model.BoardImpl
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import storage.FileSaveRepository
import testing.FakeMordieSearchFactory
import testing.FakePreferenceRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import testing.testGameSession
import view.tui.BoardLayout
import view.tui.Direction
import view.tui.TerminalEvent
import view.tui.TerminalEvent.Arrow
import view.tui.TerminalEvent.Backspace
import view.tui.TerminalEvent.Enter
import view.tui.TerminalEvent.KeyPress
import view.tui.TerminalEvent.Tab
import view.tui.TerminalInputParser
import view.tui.TerminalSize

/** GH-200: keyboard mode's letter keys on Ukrainian and Russian layouts, and those letters in save
 *  names. Real UTF-8 bytes go through `TerminalInputParser`, so its decoding is under test too. */
class NonLatinKeysScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    /** Tiles 16..1, so every row and column shows different numbers. */
    private val scrambled = IntArray(16) { 15 - it }

    private class Run(val terminal: FakeTerminal, val board: BoardImpl, val analytics: RecordingAnalyticsService) {
        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }
        val lastFrame get() = terminal.frames.last()
    }

    /** The terminal of the session running now, for a `beforeEvent` snapshot. */
    private lateinit var current: FakeTerminal

    private fun session(
        events: List<TerminalEvent>,
        start: IntArray = scrambled,
        mode: InputMode = InputMode.KEYBOARD,
        savesDir: Path = createTempDirectory("mordovorot-gh200"),
        sizeChosenAtLaunch: Boolean = true,
        leftHanded: Boolean = false,
        beforeEvent: Map<Int, () -> Unit> = emptyMap(),
        factory: FakeMordieSearchFactory = FakeMordieSearchFactory(),
    ): Run = runBlocking {
        val terminal = FakeTerminal(events.toMutableList(), terminalSize, beforeEvent).also { current = it }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState(start, 0) }
        val preferences = if (leftHanded) mutableMapOf("keyboard_layout" to "left_handed") else mutableMapOf()
        testGameSession(
            initialMode = mode,
            board = board,
            saves = FileSaveRepository(savesDir),
            analytics = analytics,
            sizeChosenAtLaunch = sizeChosenAtLaunch,
            terminalFactory = { terminal },
            mordie = Mordie(factory),
            preferences = FakePreferenceRepository(preferences),
        ).run()
        Run(terminal, board, analytics)
    }

    /** What a terminal sends for [text], decoded as in the real game. */
    private fun typed(text: String) = TerminalInputParser().feed(text.toByteArray())

    private fun plain(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "")

    /** The selected tile, or the solution screen's current step: what's in reverse video. */
    private fun reversed(frame: String) =
        Regex("\u001B\\[7m(.*?)\u001B\\[27m").findAll(frame).map { it.groupValues[1].trim() }.toList()

    /** The displayed (1-based) tile of 0-based [row], [col] of [state]. */
    private fun at(state: IntArray, row: Int, col: Int) = listOf("${state[row * 4 + col] + 1}")

    private fun shifted(start: IntArray = scrambled, block: BoardImpl.() -> Unit): IntArray =
        BoardImpl(4).apply { restoreState(start, 0); block() }.boardArray

    @Test
    fun `Cyrillic keys select and shift in the right-handed layout`() {
        assertEquals(at(scrambled, 0, 1), reversed(session(typed("в")).lastFrame))
        assertEquals(at(scrambled, 0, 3), reversed(session(typed("вфф")).lastFrame))

        val run = session(typed("вффрЛ"))

        val expected = shifted { shiftLeft(0); shiftUp(3) }
        assertContentEquals(expected, run.board.boardArray)
        assertEquals(at(expected, 0, 3), reversed(run.lastFrame))
    }

    @Test
    fun `Cyrillic keys swap jobs in the left-handed layout`() {
        assertEquals(at(scrambled, 0, 1), reversed(session(typed("д"), leftHanded = true).lastFrame))

        val run = session(typed("дфц"), leftHanded = true)

        val expected = shifted { shiftLeft(0); shiftUp(1) }
        assertContentEquals(expected, run.board.boardArray)
        assertEquals(at(expected, 0, 1), reversed(run.lastFrame))
    }

    @Test
    fun `Ukrainian і and Russian ы both act as S`() {
        assertEquals(at(scrambled, 1, 0), reversed(session(typed("і")).lastFrame))
        assertEquals(at(scrambled, 2, 0), reversed(session(typed("іы")).lastFrame))
    }

    @Test
    fun `a shift with р counts as hjkl in analytics`() {
        val solved = IntArray(16) { it }

        val run = session(typed("р"), start = shifted(solved) { shiftRight(0) })

        assertContentEquals(solved, run.board.boardArray)
        assertEquals("hjkl", run.events("board_solved").single()["shift_keys"])
    }

    @Test
    fun `Cyrillic keys step through the solution screen`() {
        val factory = FakeMordieSearchFactory()
        // Twelve one-shift steps - more than a 4x4's nine step rows, so the list scrolls.
        val longPath = List(12) { SolutionMove(isRow = it % 2 == 0, index = it % 4, amount = 1) }
        val layout = BoardLayout(terminalSize, 4)
        fun stepsShown(frame: String) = plain(frame).split("\r\n")
            .subList(layout.stepListTop, layout.stepListTop + layout.stepRows).map { it.drop(layout.stepListX).trim() }

        // Mordie finds it, five shifts (one move) reveal it, then 5 and Enter give up.
        val prefix = typed("ррррр5") + Enter
        val script = prefix + typed("дфдкы")
        val snaps = mutableListOf<String>()
        val acts = (prefix.size..script.size).associateWith { { snaps += current.frames.last(); Unit } } +
            (0 to { factory.latest.progress = SearchProgress(longPath.size, found = true, elapsedMs = 0, solution = longPath) })
        session(script, beforeEvent = acts, factory = factory)

        fun step(frame: String) = reversed(frame).singleOrNull()?.take(3)
        assertEquals(listOf(null, "01.", null, "01.", null), snaps.subList(0, 5).map(::step))
        assertEquals(stepsShown(snaps[4]).drop(1), stepsShown(snaps[5]).dropLast(1))
    }

    @Test
    fun `other non-Latin letters do nothing`() {
        val events = typed("ωש")
        assertEquals(emptyList(), events)

        val run = session(events + KeyPress('x'))

        assertContentEquals(scrambled, run.board.boardArray)
        assertEquals(0, run.board.moveCount)
        assertEquals(1, run.terminal.frames.map(::plain).toSet().size, "no stray characters drawn")
    }

    @Test
    fun `a Cyrillic save name is saved, listed and loaded`() {
        val dir = createTempDirectory("mordovorot-gh200")

        val run = session(typed("2цй") + Enter + typed("h3") + Tab + Enter, savesDir = dir)

        assertTrue(run.terminal.frames.any { "Name: цй_" in plain(it) })
        assertTrue(dir.resolve("цй.save").exists())
        assertTrue(run.terminal.frames.any { "Saved as 'цй'" in plain(it) })
        assertTrue(run.terminal.frames.any { "цй (4x4)" in plain(it) })
        assertContentEquals(scrambled, run.board.boardArray)
    }

    private fun Run.lastSaveDialog() = plain(terminal.frames.last { "Save game" in plain(it) })

    @Test
    fun `Backspace removes a whole Cyrillic letter`() {
        val run = session(typed("2цй") + Backspace)

        assertTrue("Name: ц_" in run.lastSaveDialog())
    }

    @Test
    fun `other non-Latin letters don't enter the Save name`() {
        val run = session(typed("2цωü"))

        assertTrue("Name: ц_" in run.lastSaveDialog())
    }

    @Test
    fun `the startup restore lists a Cyrillic save`() {
        val dir = createTempDirectory("mordovorot-gh200")
        val saved = intArrayOf(1, 0) + (2 until 16).toList()
        dir.resolve("ц.save").writeText("4\n${saved.joinToString(" ")}\n0\n")

        val run = session(listOf(Tab, Enter), savesDir = dir, sizeChosenAtLaunch = false)

        assertTrue(run.terminal.frames.any { "Restore a saved game?" in plain(it) && "ц (4x4)" in plain(it) })
        assertContentEquals(saved, run.board.boardArray)
    }

    @Test
    fun `the Controls key list ends with the layouts line, in both modes`() {
        val line = "Letters: Latin, Ukrainian or Russian layout"
        fun Run.lastControls() = plain(terminal.frames.last { "Play with" in plain(it) })
        fun endsWithIt(dialog: String): Boolean {
            val rows = dialog.split("\r\n")
            return line in rows[rows.indexOfFirst { "↑ ↓ W S K J scroll" in it } + 1]
        }

        for (mode in InputMode.entries) {
            assertTrue(endsWithIt(session(typed("6"), mode = mode).lastControls()), "$mode")
            val leftHanded = session(typed("6") + Tab + Arrow(Direction.RIGHT), mode = mode).lastControls()
            assertTrue("● Left-handed" in leftHanded, "$mode")
            assertTrue(endsWithIt(leftHanded), "$mode, left-handed")
        }
    }
}
