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
import testing.FakePreferenceRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import testing.testGameSession
import view.tui.BoardLayout
import view.tui.Direction
import view.tui.HitTarget
import view.tui.Terminal
import view.tui.TerminalEvent
import view.tui.TerminalEvent.Arrow
import view.tui.TerminalEvent.Enter
import view.tui.TerminalEvent.Escape
import view.tui.TerminalEvent.KeyPress
import view.tui.TerminalEvent.MouseClick
import view.tui.TerminalEvent.Tab
import view.tui.TerminalSize

/** GH-170: the Controls dialog (`[ Controls 6 ]` / 6, both modes) picks the input mode and the
 *  keyboard layout; the left-handed layout mirrors the hands and is remembered. */
class ControlsScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val layout = BoardLayout(terminalSize, 4)

    /** Tiles 16..1, so every row and column shows different numbers. */
    private val scrambled = IntArray(16) { 15 - it }

    private class Run(val terminal: FakeTerminal, val board: BoardImpl, val analytics: RecordingAnalyticsService) {
        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }
    }

    private fun session(
        mode: InputMode,
        events: List<TerminalEvent>,
        start: IntArray = scrambled,
        preferences: FakePreferenceRepository = FakePreferenceRepository(),
        terminalFactory: ((FakeTerminal) -> Terminal)? = null,
    ): Run = runBlocking {
        val terminal = FakeTerminal(events.toMutableList(), terminalSize)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState(start, 0) }
        testGameSession(
            initialMode = mode,
            board = board,
            saves = FileSaveRepository(createTempDirectory("mordovorot-gh170")),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            terminalFactory = terminalFactory?.let { { it(terminal) } } ?: { terminal },
            mordie = Mordie(FakeMordieSearchFactory()),
            preferences = preferences,
        ).run()
        Run(terminal, board, analytics)
    }

    private fun keys(text: String) = text.map { KeyPress(it) }

    private fun plain(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").replace("\r\n", "\n")

    private fun click(target: HitTarget) = layout.toolbarButtons().first { it.target == target }.let { MouseClick(it.x, it.y) }

    private val shiftRowOneLeft = layout.leftArrowPosition(0).let { (x, y) -> MouseClick(x, y) }

    private fun shiftedLeft() = BoardImpl(4).apply { restoreState(scrambled, 0); shiftLeft(0) }.boardArray

    /** The number on the reverse-video selected tile of [frame]. */
    private fun selectedTile(frame: String) =
        Regex(Regex.escape("\u001B[7m") + "(.*?)" + Regex.escape("\u001B[27m")).find(frame)?.groupValues?.get(1)?.trim()

    /** 6, Tab to `Keyboard keys`, mark Left-handed, OK. */
    private val chooseLeftHanded = keys("6") + Tab + Arrow(Direction.RIGHT) + Enter

    private fun inMouseMode(frame: String) = "[ New ] [ Save ] [ Load ] [ Exit ]" in plain(frame)
    private fun inKeyboardMode(frame: String) = "[New 1] [Save 2] [Load 3] [Exit Esc]" in plain(frame)

    @Test
    fun `6 in mouse mode opens Controls, and Keyboard then OK switches mode keeping the game`() {
        val run = session(InputMode.MOUSE, listOf(shiftRowOneLeft) + keys("123456") + Arrow(Direction.RIGHT) + Enter)

        val frames = run.terminal.frames.map(::plain)
        assertTrue("[ Labels ] [ Controls 6 ]" in frames.first())
        val dialog = frames.first { "Play with" in it }
        assertTrue("Play with      ● Mouse  ○ Keyboard" in dialog)
        assertTrue("Select tile    W A S D" in dialog)
        assertTrue(frames.any { "○ Mouse  ● Keyboard" in it })
        assertTrue(inKeyboardMode(run.terminal.frames.last()))
        assertContentEquals(shiftedLeft(), run.board.boardArray)
        assertEquals(1, run.board.moveCount)

        val opened = run.analytics.events.filter { it.name.startsWith("screen_") && it.name.endsWith("_dialog") }
        assertEquals(listOf("screen_controls_dialog"), opened.map { it.name }, "1-5 do nothing in mouse mode")
        assertEquals("shortcut", opened.single().properties["opened_from"])
        assertEquals("mouse", opened.single().properties["input_method"])
        assertEquals("play", opened.single().properties["screen"])
        val switched = run.events("input_mode_switched").single()
        assertEquals("controls_dialog", switched["trigger"])
        assertEquals("keyboard", switched["to_mode"])
    }

    @Test
    fun `clicking Controls opens it with opened_from toolbar`() {
        val run = session(InputMode.MOUSE, listOf(click(HitTarget.ToolbarControls), Escape))

        assertTrue(run.terminal.frames.any { "Play with" in plain(it) })
        assertEquals("toolbar", run.events("screen_controls_dialog").single()["opened_from"])
    }

    @Test
    fun `Cancel and Escape in Controls change nothing`() {
        val run = session(InputMode.MOUSE, keys("6") + Arrow(Direction.RIGHT) + Escape + keys("6") + Arrow(Direction.RIGHT) + Tab + Tab + Tab + Enter)

        assertTrue(inMouseMode(run.terminal.frames.last()))
        assertEquals(listOf("controls", "controls"), run.events("dialog_cancelled").map { it["dialog"] })
        assertTrue(run.events("input_mode_switched").isEmpty())
    }

    @Test
    fun `the selection survives a switch to mouse mode and back`() {
        // S D D: row 2, column 3; in the dialog, the arrows wrap the Play with mark.
        val toMouse = keys("6") + Arrow(Direction.LEFT) + Enter
        val toKeyboard = keys("6") + Arrow(Direction.RIGHT) + Enter
        val run = session(InputMode.KEYBOARD, keys("sdd") + toMouse + toKeyboard)

        assertEquals(listOf("mouse", "keyboard"), run.events("input_mode_switched").map { it["to_mode"] })
        assertEquals("${scrambled[1 * 4 + 2] + 1}", selectedTile(run.terminal.frames.last()))
    }

    @Test
    fun `a failed switch keeps the game and says why`() {
        var builds = 0
        val run = session(InputMode.MOUSE, listOf(shiftRowOneLeft) + keys("6") + Arrow(Direction.RIGHT) + Enter) { terminal ->
            if (++builds == 2) throw RuntimeException("stty not found")
            terminal
        }

        assertTrue(inMouseMode(run.terminal.frames.last()))
        assertTrue("Could not switch to keyboard mode (stty not found) - staying in mouse mode." in plain(run.terminal.frames.last()))
        assertContentEquals(shiftedLeft(), run.board.boardArray)
        assertEquals(1, run.board.moveCount)
    }

    @Test
    fun `the left-handed layout selects with HJKL and the arrows and shifts with WASD`() {
        // j l: row 2, column 2; d shifts row 2 right, w shifts column 2 up.
        val run = session(InputMode.KEYBOARD, chooseLeftHanded + keys("jldw"))

        val dialog = plain(run.terminal.frames.last { "Left-handed" in plain(it) && "Play with" in plain(it) })
        assertTrue("Keyboard keys  ○ Right-handed  ● Left-handed" in dialog)
        assertTrue("Select tile    ← ↑ ↓ →  H J K L" in dialog)
        assertTrue("Shift row      A D" in dialog)
        val last = plain(run.terminal.frames.last())
        for (hint in listOf("A/D row", "W/S column", "←↑↓→ H/J/K/L select")) assertTrue(hint in last, hint)
        assertFalse("W/A/S/D select" in last)
        val expected = BoardImpl(4).apply { restoreState(scrambled, 0); shiftRight(1); shiftUp(1) }
        assertContentEquals(expected.boardArray, run.board.boardArray)
        assertTrue(run.events("input_mode_switched").isEmpty())
    }

    @Test
    fun `the keyboard layout is remembered by the next session`() {
        val preferences = FakePreferenceRepository()
        session(InputMode.KEYBOARD, chooseLeftHanded, preferences = preferences)

        // h moves the selection from column 1 to column 4 (wrapping) instead of shifting row 1.
        val next = session(InputMode.KEYBOARD, keys("h"), preferences = preferences)

        assertEquals("left_handed", preferences.values["keyboard_layout"])
        assertEquals(0, next.board.moveCount)
        assertEquals("${scrambled[3] + 1}", selectedTile(next.terminal.frames.last()))
    }

    @Test
    fun `a left-handed solve sends board_solved without shift_keys`() {
        val solved = IntArray(16) { it }
        val oneShiftAway = BoardImpl(4).apply { restoreState(solved, 0); shiftLeft(0) }.boardArray

        val run = session(InputMode.KEYBOARD, chooseLeftHanded + keys("d"), start = oneShiftAway)

        val boardSolved = run.events("board_solved").single()
        assertFalse("shift_keys" in boardSolved, "$boardSolved")
    }
}
