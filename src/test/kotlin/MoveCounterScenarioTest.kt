import board_model.BoardImpl
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
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
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.DialogLayout
import view.tui.HitTarget
import view.tui.TerminalEvent
import view.tui.TerminalSize

/** GH-79: GH-62's console move-counter scenarios rewritten for the TUI - real BoardImpl +
 *  ViewModelImpl + FileSaveRepository (temp dir), driven through FakeTerminal, reading the
 *  `Moves:` row under the title. */
class MoveCounterScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val layout = BoardLayout(terminalSize, 4)

    /** 4x4, row 1 rotated right once - a left shift on row 1 solves it. */
    private val oneShiftFromSolved = intArrayOf(3, 0, 1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)

    /** 4x4, far from solved - no scripted click sequence below can solve it by accident. */
    private val scrambled = IntArray(16) { 15 - it }

    private class Run(val terminal: FakeTerminal, val analytics: RecordingAnalyticsService)

    /** A mouse-mode [GameSession] over a real board and a temp-dir save repository, with
     *  `--size` semantics (no startup prompts), wired through `defaultView`. */
    private fun session(
        events: List<TerminalEvent>,
        initial: IntArray = scrambled,
        savesDir: Path = createTempDirectory("mordovorot-gh62"),
    ): Run = runBlocking {
        val terminal = FakeTerminal(events.toMutableList(), terminalSize)
        val analytics = RecordingAnalyticsService()
        testGameSession(
            initialMode = InputMode.MOUSE,
            board = BoardImpl(4).apply { restoreState(initial) },
            saves = FileSaveRepository(savesDir),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            terminalFactory = { terminal },
            mordie = Mordie(FakeMordieSearchFactory()),
        ).run()
        Run(terminal, analytics)
    }

    private fun click(p: Pair<Int, Int>) = TerminalEvent.MouseClick(p.first, p.second)

    private fun left(row: Int) = click(layout.leftArrowPosition(row))
    private fun right(row: Int) = click(layout.rightArrowPosition(row))
    private fun up(col: Int) = click(layout.upArrowPosition(col))

    /** Toolbar Save, type [name], Enter. */
    private fun save(name: String) = listOf(click(layout.saveButtonPosition())) + name.map { TerminalEvent.KeyPress(it) } + TerminalEvent.Enter

    /** Toolbar Load, then the dialog's Load button with the first of [listed] selected. */
    private fun load(vararg listed: String): List<TerminalEvent> {
        val dialog = DialogLayout(
            Dialog(
                Dialog.Kind.LOAD,
                "Load game",
                listItems = listed.toList(),
                selectedIndex = 0,
                buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
            ),
            terminalSize,
        )
        val button = dialog.buttons().first { it.target == HitTarget.DialogButton("load") }
        return listOf(click(layout.loadButtonPosition()), TerminalEvent.MouseClick(button.x, dialog.buttonsRow()))
    }

    /** Toolbar New, then the size dialog's [side] button. */
    private fun newGame(side: Int): List<TerminalEvent> {
        val dialog = DialogLayout(
            Dialog(
                Dialog.Kind.SIZE,
                "New game size",
                buttons = (3..5).map { DialogButtonSpec(it.toString(), "${it}x$it") } + DialogButtonSpec("cancel", "Cancel"),
            ),
            terminalSize,
        )
        val button = dialog.buttons().first { it.target == HitTarget.DialogButton(side.toString()) }
        return listOf(click(layout.newButtonPosition()), TerminalEvent.MouseClick(button.x, dialog.buttonsRow()))
    }

    /** The text on the row directly under the title in a TUI [frame], ANSI escapes stripped. */
    private fun movesShown(frame: String): String {
        val rows = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")
        return rows[layout.movesRow].trim()
    }

    private val Run.lastMoves get() = movesShown(terminal.frames.last())

    @Test
    fun `TUI shows Moves 0 on a fresh game`() {
        val run = session(emptyList())

        assertEquals("Moves: 0", movesShown(run.terminal.frames.first()))
    }

    @Test
    fun `repeated clicks on the same line count as one move, in either direction`() {
        val run = session(listOf(left(0), left(0), right(0)))

        assertEquals("Moves: 1", run.lastMoves)
    }

    @Test
    fun `switching lines starts a new move`() {
        val run = session(listOf(left(0), up(0), left(0)))

        assertEquals("Moves: 3", run.lastMoves)
    }

    @Test
    fun `New resets the count`() {
        val run = session(listOf(left(0)) + newGame(4))

        assertEquals("Moves: 0", run.lastMoves)
    }

    @Test
    fun `save then load restores the count, and the next shift starts a new move`() {
        val run = session(listOf(left(0), up(0)) + save("m") + up(0) + load("m (4x4)") + up(0))

        val frames = run.terminal.frames
        assertEquals("Moves: 2", movesShown(frames[frames.size - 2])) // right after the load
        assertEquals("Moves: 3", run.lastMoves)
    }

    @Test
    fun `a save written before move counting loads with Moves 0`() {
        val dir = createTempDirectory("mordovorot-gh62")
        dir.resolve("old.save").writeText("4\n${scrambled.joinToString(" ")}\n")

        val run = session(listOf(left(0)) + load("old (4x4)"), savesDir = dir)

        assertEquals("Moves: 0", run.lastMoves)
    }

    @Test
    fun `board_solved fires once in the TUI with the final count and size`() {
        val run = session(listOf(left(0)), initial = oneShiftFromSolved)

        assertEquals(
            // GH-83 adds Mordie's result; the fake search is still at 0 moves, not found.
            listOf(
                mapOf(
                    "move_count" to 1,
                    "board_size" to 4,
                    "tile_labels" to "numbers",
                    "mordie_result" to "player_won",
                    "mordie_moves" to 0,
                    "input_method" to "mouse",
                ),
            ),
            run.analytics.events.filter { it.name == "board_solved" }.map { it.properties },
        )
    }

    @Test
    fun `loading an already-solved save does not fire board_solved`() {
        val dir = createTempDirectory("mordovorot-gh62")
        dir.resolve("done.save").writeText("4\n${(0 until 16).joinToString(" ")}\n3\n")

        val run = session(load("done (4x4)"), savesDir = dir)

        assertTrue(run.terminal.frames.last().contains("Congratulations"))
        assertTrue(run.analytics.events.none { it.name == "board_solved" })
    }
}
