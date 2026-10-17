import board_model.BoardImpl
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
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
import view.tui.Direction
import view.tui.HitTarget
import view.tui.TerminalEvent
import view.tui.TerminalEvent.Arrow
import view.tui.TerminalEvent.Enter
import view.tui.TerminalEvent.Escape
import view.tui.TerminalEvent.FunctionKey
import view.tui.TerminalEvent.KeyPress
import view.tui.TerminalSize

/** GH-101: keyboard mode's line selection - WASD selects a row/column (wrapping, switching
 *  back to the remembered line), HJKL/arrows shift it, no arrows drawn, the active line's
 *  tiles highlighted. Real BoardImpl + ViewModelImpl driven through FakeTerminal in keyboard
 *  mode. Mouse mode is unchanged (its own tests run unmodified). */
class KeyboardLineSelectionScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    /** Tiles 16..1, so every row and column shows different numbers. */
    private val scrambled = IntArray(16) { 15 - it }

    private class Run(val terminal: FakeTerminal, val board: BoardImpl, val analytics: RecordingAnalyticsService) {
        fun event(name: String) = analytics.events.single { it.name == name }.properties
    }

    private fun session(events: List<TerminalEvent>, start: IntArray = scrambled, side: Int = 4): Run = runBlocking {
        val terminal = FakeTerminal(events.toMutableList(), terminalSize)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(side).apply { restoreState(start, 0) }
        testGameSession(
            initialMode = InputMode.KEYBOARD,
            board = board,
            saves = FileSaveRepository(createTempDirectory("mordovorot-gh101")),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            terminalFactory = { terminal },
            mordie = Mordie(FakeMordieSearchFactory()),
        ).run()
        Run(terminal, board, analytics)
    }

    private fun keys(text: String) = text.map { KeyPress(it) }

    /** The reverse-video cell texts of [frame], in drawing order. */
    private fun highlighted(frame: String) =
        Regex("\u001B\\[7m(.*?)\u001B\\[27m").findAll(frame).map { it.groupValues[1].trim() }.toList()

    private fun Run.lastHighlight() = highlighted(terminal.frames.last())

    /** The displayed (1-based) tiles of 0-based [row] / [col] of [state]. */
    private fun rowTiles(state: IntArray, row: Int, side: Int = 4) = (0 until side).map { "${state[row * side + it] + 1}" }
    private fun colTiles(state: IntArray, col: Int, side: Int = 4) = (0 until side).map { "${state[it * side + col] + 1}" }

    private fun shifted(start: IntArray = scrambled, block: BoardImpl.() -> Unit): IntArray =
        BoardImpl(4).apply { restoreState(start, 0); block() }.boardArray

    private fun Run.movesShown() = terminal.frames.last().contains("Moves: ${board.moveCount}")

    @Test
    fun `keyboard mode shows the selected row highlighted and no arrows`() {
        val run = session(emptyList())

        val frame = run.terminal.frames.first()
        for (glyph in listOf("◀", "▶", "▲", "▼")) assertTrue(run.terminal.frames.none { glyph in it }, glyph)
        assertEquals(rowTiles(scrambled, 0), highlighted(frame))
        for (hint in listOf("W/S  row", "A/D  column", "←/→ H/L  shift row", "↑/↓ K/J  shift column")) {
            assertTrue(hint in frame, hint)
        }
        assertTrue("F5/F6/F9 Save/Load/New · F7 Mouse · Esc" in frame)
        assertFalse("Enter" in frame)
    }

    @Test
    fun `W and S move the row selection with wraparound`() {
        assertEquals(rowTiles(scrambled, 1), session(keys("s")).lastHighlight())
        assertEquals(rowTiles(scrambled, 3), session(keys("sww")).lastHighlight())
    }

    @Test
    fun `A and D switch to the column, then move it with wraparound`() {
        assertEquals(colTiles(scrambled, 0), session(keys("d")).lastHighlight())
        assertEquals(colTiles(scrambled, 3), session(keys("da")).lastHighlight())
    }

    @Test
    fun `switching back returns to the remembered line`() {
        assertEquals(rowTiles(scrambled, 1), session(keys("sddw")).lastHighlight())
    }

    @Test
    fun `H L and the left right arrows shift the selected row`() {
        val right = session(keys("sl"))
        assertContentEquals(shifted { shiftRight(1) }, right.board.boardArray)

        val run = session(keys("sl") + Arrow(Direction.LEFT) + keys("h"))

        assertContentEquals(shifted { shiftLeft(1) }, run.board.boardArray)
        assertEquals(1, run.board.moveCount)
        assertTrue(run.movesShown())
    }

    @Test
    fun `J K and the up down arrows shift the selected column`() {
        assertContentEquals(shifted { shiftDown(0) }, session(keys("dj")).board.boardArray)
        assertContentEquals(shifted { shiftUp(0) }, session(keys("dj") + Arrow(Direction.UP) + keys("k")).board.boardArray)
    }

    @Test
    fun `a shift key on the other axis switches without shifting`() {
        val toColumn = session(listOf(Arrow(Direction.DOWN)))
        assertEquals(colTiles(scrambled, 0), toColumn.lastHighlight())
        assertContentEquals(scrambled, toColumn.board.boardArray)

        val back = session(listOf(Arrow(Direction.DOWN)) + keys("l"))
        assertEquals(rowTiles(scrambled, 0), back.lastHighlight())
        assertContentEquals(scrambled, back.board.boardArray)
        assertEquals(0, back.board.moveCount)
    }

    @Test
    fun `upper case letters work too`() {
        val run = session(keys("SL"))

        val expected = shifted { shiftRight(1) }
        assertContentEquals(expected, run.board.boardArray)
        assertEquals(rowTiles(expected, 1), run.lastHighlight())
    }

    @Test
    fun `Enter and Space no longer shift`() {
        val run = session(listOf(Enter, KeyPress(' ')))

        assertContentEquals(scrambled, run.board.boardArray)
        assertEquals(0, run.board.moveCount)
    }

    @Test
    fun `letters typed in the Save dialog go into the name field`() {
        val run = session(listOf(FunctionKey(5)) + keys("wasd") + Escape)

        assertTrue(run.terminal.frames.any { "Name: wasd_" in it })
        assertContentEquals(scrambled, run.board.boardArray)
        assertEquals(rowTiles(scrambled, 0), run.lastHighlight())
    }

    @Test
    fun `the selection survives a switch to mouse mode and back`() {
        val keyboardButton = BoardLayout(terminalSize, 4, modeButtons = listOf(InputMode.KEYBOARD))
            .toolbarButtons().first { it.target == HitTarget.ToolbarMode(InputMode.KEYBOARD) }

        val run = session(keys("ss") + FunctionKey(7) + TerminalEvent.MouseClick(keyboardButton.x, keyboardButton.y))

        val mouseFrame = run.terminal.frames.last { "[ Keyboard ]" in it }
        for (glyph in listOf("◀", "▶", "▲", "▼")) assertTrue(glyph in mouseFrame, glyph)
        assertEquals(rowTiles(scrambled, 2), run.lastHighlight())
    }

    @Test
    fun `a smaller new game clamps the selection`() {
        val five = IntArray(25) { 24 - it }
        // Column 5 (D, A wraps), then row 5 (W switches, W wraps); F9 + Enter picks 3x3.
        val toThree = keys("daww") + FunctionKey(9) + Enter

        val rowRun = session(toThree, start = five, side = 5)
        assertEquals(rowTiles(rowRun.board.boardArray, 2, side = 3), rowRun.lastHighlight())

        val colRun = session(toThree + keys("d"), start = five, side = 5)
        assertEquals(colTiles(colRun.board.boardArray, 2, side = 3), colRun.lastHighlight())
    }

    @Test
    fun `a solved board hides the highlight and ignores selection and shift keys`() {
        val solved = IntArray(16) { it }
        val oneLeftOfSolved = shifted(solved) { shiftRight(0) }

        val run = session(keys("h") + keys("sl") + FunctionKey(5), start = oneLeftOfSolved)

        assertContentEquals(solved, run.board.boardArray)
        val beforeSave = run.terminal.frames.takeWhile { "Save game" !in it }
        assertFalse("Mordovorot" in beforeSave.last())
        assertEquals(emptyList(), highlighted(beforeSave.last()))
        assertTrue(beforeSave.size < run.terminal.frames.size, "F5 still opens the Save dialog")
        assertEquals("hjkl", run.event("board_solved")["shift_keys"])
    }

    @Test
    fun `shift_keys reports mixed when both kinds of key were used`() {
        val solved = IntArray(16) { it }
        val twoFromSolved = shifted(solved) { shiftRight(0); shiftDown(0) }

        // D: column 1; Up arrow: shift it up; W: back to row 1; H: shift it left.
        val run = session(keys("d") + Arrow(Direction.UP) + keys("wh"), start = twoFromSolved)

        assertContentEquals(solved, run.board.boardArray)
        assertEquals("mixed", run.event("board_solved")["shift_keys"])
    }

    @Test
    fun `shift_keys keeps counting across a switch to mouse mode and back`() {
        val solved = IntArray(16) { it }
        val twoFromSolved = shifted(solved) { shiftRight(0); shiftDown(0) }
        val keyboardButton = BoardLayout(terminalSize, 4, modeButtons = listOf(InputMode.KEYBOARD))
            .toolbarButtons().first { it.target == HitTarget.ToolbarMode(InputMode.KEYBOARD) }

        val run = session(
            keys("d") + Arrow(Direction.UP) + FunctionKey(7) + TerminalEvent.MouseClick(keyboardButton.x, keyboardButton.y) + keys("wh"),
            start = twoFromSolved,
        )

        assertContentEquals(solved, run.board.boardArray)
        assertEquals("mixed", run.event("board_solved")["shift_keys"])
    }
}
