import board_model.BoardImpl
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
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
 *  keyboard layout; the left-handed layout mirrors the hands and is remembered. Stubs until
 *  WU3 (layout) makes the last three green. */
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
        terminalFactory: ((FakeTerminal) -> Terminal)? = null,
    ): Run = runBlocking {
        val terminal = FakeTerminal(events.toMutableList(), terminalSize)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState(scrambled, 0) }
        testGameSession(
            initialMode = mode,
            board = board,
            saves = FileSaveRepository(createTempDirectory("mordovorot-gh170")),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            terminalFactory = terminalFactory?.let { { it(terminal) } } ?: { terminal },
            mordie = Mordie(FakeMordieSearchFactory()),
        ).run()
        Run(terminal, board, analytics)
    }

    private fun keys(text: String) = text.map { KeyPress(it) }

    private fun plain(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").replace("\r\n", "\n")

    private fun click(target: HitTarget) = layout.toolbarButtons().first { it.target == target }.let { MouseClick(it.x, it.y) }

    private val shiftRowOneLeft = layout.leftArrowPosition(0).let { (x, y) -> MouseClick(x, y) }

    private fun shiftedLeft() = BoardImpl(4).apply { restoreState(scrambled, 0); shiftLeft(0) }.boardArray

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
        val run = session(InputMode.MOUSE, keys("6") + Arrow(Direction.RIGHT) + Escape + keys("6") + Arrow(Direction.RIGHT) + Tab + Tab + Enter)

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
        val last = run.terminal.frames.last()
        val selected = Regex(Regex.escape("\u001B[7m") + "(.*?)" + Regex.escape("\u001B[27m")).find(last)?.groupValues?.get(1)?.trim()
        assertEquals("${scrambled[1 * 4 + 2] + 1}", selected)
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
        // TODO: 6, Tab, → - Left-handed marked, the key list shows the mirrored keys
        // TODO: Enter
        // TODO: hints read `A/D row`, `W/S column` on the left and `←↑↓→ H/J/K/L select` on the right
        // TODO: j, l select row 2, column 2; d shifts row 2 right; w shifts column 2 up
    }

    @Test
    fun `the keyboard layout is remembered by the next session`() {
        // TODO: pick Left-handed, OK
        // TODO: a fresh session over the same preference store starts left-handed: h moves the selection and doesn't shift
    }

    @Test
    fun `a left-handed solve sends board_solved without shift_keys`() {
        // TODO: left-handed, start one shift away from solved, solve it with a/d/w/s
        // TODO: board_solved has no shift_keys
    }
}
