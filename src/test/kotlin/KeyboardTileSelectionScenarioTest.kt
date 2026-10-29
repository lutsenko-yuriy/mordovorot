import board_model.BoardImpl
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import storage.FileSaveRepository
import testing.FakeMordieSearchFactory
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import testing.testGameSession
import view.tui.BoardLayout
import view.tui.Direction
import view.tui.TerminalEvent
import view.tui.TerminalEvent.Arrow
import view.tui.TerminalEvent.Enter
import view.tui.TerminalEvent.Escape
import view.tui.TerminalEvent.KeyPress
import view.tui.TerminalInputParser
import view.tui.TerminalSize

/** GH-170: keyboard mode on the shared keyboard scheme (ADR-0003) - a selected tile moved by
 *  WASD and shifted by HJKL/arrows, the digit toolbar, no F-keys, H/L and K/J on the solution
 *  screen. Replaces GH-101's line selection. */
class KeyboardTileSelectionScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    /** Tiles 16..1, so every row and column shows different numbers. */
    private val scrambled = IntArray(16) { 15 - it }

    private class Run(val terminal: FakeTerminal, val board: BoardImpl, val analytics: RecordingAnalyticsService) {
        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }
        val lastFrame get() = terminal.frames.last()
    }

    private fun session(
        events: List<TerminalEvent>,
        start: IntArray = scrambled,
        side: Int = 4,
        beforeEvent: Map<Int, () -> Unit> = emptyMap(),
        factory: FakeMordieSearchFactory = FakeMordieSearchFactory(),
    ): Run = runBlocking {
        val terminal = FakeTerminal(events.toMutableList(), terminalSize, beforeEvent).also { current = it }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(side).apply { restoreState(start, 0) }
        testGameSession(
            initialMode = InputMode.KEYBOARD,
            board = board,
            saves = FileSaveRepository(createTempDirectory("mordovorot-gh170")),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            terminalFactory = { terminal },
            mordie = Mordie(factory),
        ).run()
        Run(terminal, board, analytics)
    }

    /** The terminal of the session running now, for a `beforeEvent` snapshot. */
    private lateinit var current: FakeTerminal

    private fun keys(text: String) = text.map { KeyPress(it) }

    private fun styled(frame: String, on: String, off: String) =
        Regex(Regex.escape(on) + "(.*?)" + Regex.escape(off)).findAll(frame).map { it.groupValues[1].trim() }.toList()

    /** The selected tile (reverse video), the rest of its row, the rest of its column. */
    private fun tile(frame: String) = styled(frame, "\u001B[7m", "\u001B[27m")
    private fun rowRest(frame: String) = styled(frame, "\u001B[30;48;5;230m", "\u001B[39;49m")
    private fun colRest(frame: String) = styled(frame, "\u001B[30;48;5;252m", "\u001B[39;49m")

    private fun rows(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
    private fun plain(frame: String) = rows(frame).joinToString("\n")

    /** The displayed (1-based) tile of 0-based [row], [col] of [state]. */
    private fun at(state: IntArray, row: Int, col: Int, side: Int = 4) = "${state[row * side + col] + 1}"

    /** What [tile], [rowRest] and [colRest] read with ([row], [col]) selected on [state]. */
    private fun marked(state: IntArray, row: Int, col: Int, side: Int = 4) = Triple(
        listOf(at(state, row, col, side)),
        (0 until side).filter { it != col }.map { at(state, row, it, side) },
        (0 until side).filter { it != row }.map { at(state, it, col, side) },
    )

    private fun shown(frame: String) = Triple(tile(frame), rowRest(frame), colRest(frame))

    private fun shifted(start: IntArray = scrambled, block: BoardImpl.() -> Unit): IntArray =
        BoardImpl(4).apply { restoreState(start, 0); block() }.boardArray

    @Test
    fun `keyboard mode marks the selected tile, its row and its column and shows the digit toolbar`() {
        val frame = session(emptyList()).terminal.frames.first()
        val text = plain(frame)

        assertEquals(marked(scrambled, 0, 0), shown(frame))
        for (glyph in listOf("◀", "▶", "▲", "▼")) assertFalse(glyph in text, glyph)
        for (hint in listOf("W/A/S/D select", "←/→ H/L row", "↑/↓ K/J column")) assertTrue(hint in text, hint)
        for (row in listOf("[New 1] [Save 2] [Load 3] [Exit Esc]", "[Labels 4] [Controls 6]")) assertTrue(row in text, row)
        assertTrue("1 New · 2 Save · 3 Load · 4 Labels · 5 Give Up · 6 Controls · Esc Exit" in text)
    }

    @Test
    fun `W A S D move the selected tile with wraparound`() {
        val down = session(keys("sd"))
        assertEquals(marked(scrambled, 1, 1), shown(down.lastFrame))

        val wrapped = session(keys("wa"))
        assertEquals(marked(scrambled, 3, 3), shown(wrapped.lastFrame))
    }

    @Test
    fun `H L and the left and right arrows shift the selected row and the selection stays put`() {
        val right = session(keys("sl"))
        val afterRight = shifted { shiftRight(1) }
        assertContentEquals(afterRight, right.board.boardArray)
        assertEquals(marked(afterRight, 1, 0), shown(right.lastFrame))

        val run = session(keys("sl") + Arrow(Direction.LEFT) + keys("h"))
        assertContentEquals(shifted { shiftLeft(1) }, run.board.boardArray)
        assertEquals(1, run.board.moveCount)
        assertTrue("Moves: 1" in plain(run.lastFrame))
    }

    @Test
    fun `K J and the up and down arrows shift the selected column`() {
        assertContentEquals(shifted { shiftDown(1) }, session(keys("dj")).board.boardArray)
        assertContentEquals(shifted { shiftUp(1) }, session(keys("dj") + Arrow(Direction.UP) + keys("k")).board.boardArray)
    }

    @Test
    fun `upper-case letters work too`() {
        val lower = session(keys("sdl"))
        val upper = session(keys("SDL"))

        assertContentEquals(lower.board.boardArray, upper.board.boardArray)
        assertEquals(shown(lower.lastFrame), shown(upper.lastFrame))
    }

    @Test
    fun `digits open the toolbar's dialogs in keyboard mode`() {
        val run = session(
            keys("1") + Escape + keys("2") + Escape + keys("3") + Escape + keys("4") + Escape + Escape + Escape,
        )

        val titles = listOf("New game size", "Save game", "Load game", "Tile labels", "Exit")
        for (title in titles) assertTrue(run.terminal.frames.any { title in plain(it) }, title)
        for (view in listOf("screen_size_dialog", "screen_save_dialog", "screen_load_dialog", "screen_exit_dialog")) {
            assertEquals(1, run.events(view).size, view)
        }
        assertEquals("shortcut", run.events("screen_labels_dialog").single()["opened_from"])
    }

    @Test
    fun `function keys do nothing`() {
        val fKeys = listOf("\u001BOS", "\u001B[14~", "\u001B[15~", "\u001B[17~", "\u001B[18~", "\u001B[19~", "\u001B[20~")
        val parser = TerminalInputParser()
        val events = fKeys.flatMap { parser.feed(it.toByteArray()) }
        assertEquals(emptyList(), events)

        val run = session(events + keys("x"))
        assertContentEquals(scrambled, run.board.boardArray)
        assertTrue(run.analytics.events.none { it.name.startsWith("screen_") && it.name.endsWith("_dialog") })
        assertEquals(1, run.terminal.frames.map(::plain).toSet().size, "no stray characters drawn")
    }

    @Test
    fun `digits typed in the Save dialog go into the name field`() {
        val run = session(keys("2a12") + Escape)

        assertTrue(run.terminal.frames.any { "Name: a12_" in it })
        assertTrue(run.terminal.frames.none { "Load game" in plain(it) || "Tile labels" in plain(it) })
        assertContentEquals(scrambled, run.board.boardArray)
    }

    @Test
    fun `a smaller new game clamps the selection`() {
        val five = IntArray(25) { 24 - it }
        // W A: row 5, column 5 (both wrap); 1 + the dialog's first button (3x3) via Enter.
        val run = session(keys("wa1") + Enter, start = five, side = 5)

        assertEquals(3, run.board.squareSide)
        assertEquals(marked(run.board.boardArray, 2, 2, side = 3), shown(run.lastFrame))
    }

    @Test
    fun `a solved board hides the selection and ignores the selection and shift keys`() {
        val solved = IntArray(16) { it }
        val oneFromSolved = shifted(solved) { shiftRight(0) }

        val run = session(keys("h") + keys("wasdhljk") + Arrow(Direction.LEFT), start = oneFromSolved)

        assertContentEquals(solved, run.board.boardArray)
        assertEquals(1, run.board.moveCount)
        assertEquals(2, run.terminal.frames.size, "no repaint for an inert key")
        assertEquals(Triple(emptyList<String>(), emptyList<String>(), emptyList<String>()), shown(run.lastFrame))
        assertTrue("W/A/S/D select" in plain(run.lastFrame))
    }

    @Test
    fun `board_solved reports mixed shift_keys when HJKL and the arrows both shifted`() {
        val solved = IntArray(16) { it }
        val twoFromSolved = shifted(solved) { shiftRight(0); shiftDown(0) }

        // The up arrow shifts column 1 up, H row 1 left.
        val run = session(listOf(Arrow(Direction.UP)) + keys("h"), start = twoFromSolved)

        assertContentEquals(solved, run.board.boardArray)
        assertEquals("mixed", run.events("board_solved").single()["shift_keys"])
    }

    @Test
    fun `H L step and K J scroll the solution screen`() {
        val factory = FakeMordieSearchFactory()
        // Twelve one-shift steps - more than a 4x4's nine step rows, so the list scrolls.
        val longPath = List(12) { SolutionMove(isRow = it % 2 == 0, index = it % 4, amount = 1) }
        val layout = BoardLayout(terminalSize, 4)
        fun stepsShown(frame: String) =
            rows(frame).subList(layout.stepListTop, layout.stepListTop + layout.stepRows).map { it.drop(layout.stepListX).trim() }

        // Mordie finds it, five shifts (one move) reveal it, then 5 and Enter give up.
        val prefix = keys("hhhhh5") + Enter
        val script = prefix + keys("llhjjk")
        val snaps = mutableListOf<String>()
        val acts = (prefix.size..script.size).associateWith { { snaps += current.frames.last(); Unit } } +
            (0 to { factory.latest.progress = SearchProgress(longPath.size, found = true, elapsedMs = 0, solution = longPath) })
        val run = session(script, beforeEvent = acts, factory = factory)

        fun step(frame: String) = tile(frame).single().take(3)
        assertTrue("H/L" in plain(snaps[0]) && "K/J" in plain(snaps[0]))
        assertEquals(listOf("01.", "02.", "01."), snaps.subList(1, 4).map(::step))
        val (prev, down, downAgain, up) = snaps.subList(3, 7)
        assertEquals(stepsShown(prev).drop(1), stepsShown(down).dropLast(1))
        assertEquals(stepsShown(down).drop(1), stepsShown(downAgain).dropLast(1))
        assertEquals(stepsShown(down), stepsShown(up))
        assertEquals(1, run.events("screen_solution").size)
    }
}
