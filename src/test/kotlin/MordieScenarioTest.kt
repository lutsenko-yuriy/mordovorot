import board_model.BoardImpl
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import mordie.SearchProgress
import storage.FileSaveRepository
import testing.FakeMordieSearchFactory
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import view.tui.BoardLayout
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.DialogLayout
import view.tui.HitTarget
import view.tui.TerminalEvent
import view.tui.TerminalSize
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-83 scenarios: Mordie, the computer opponent racing the player to the shortest solution.
 *  Real BoardImpl + ViewModelImpl + FileSaveRepository (temp dir), driven through a mouse-mode
 *  GameSession (sizeChosenAtLaunch) over an 80x40 FakeTerminal, like [MoveCounterScenarioTest],
 *  with `Mordie(FakeMordieSearchFactory())` scripting the search's progress - no real search runs.
 *  Helpers read the `Mordie:` row(s) under `Moves:` and the title row, ANSI escapes stripped.
 *  Each test names the WU that makes it green. */
class MordieScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val layout = BoardLayout(terminalSize, 4)

    /** 4x4, far from solved. */
    private val scrambled = IntArray(16) { 15 - it }

    /** 4x4, row 1 rotated right once - `left(1)` solves it. */
    private val oneShiftFromSolved = intArrayOf(0, 1, 2, 3, 7, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15)

    private val factory = FakeMordieSearchFactory()

    /** One step of a scripted session: a terminal event, or an [Act] run between events. */
    private sealed interface Step
    private class Event(val event: TerminalEvent) : Step
    private class Act(val block: () -> Unit) : Step

    private class Run(val terminal: FakeTerminal, val board: BoardImpl, val analytics: RecordingAnalyticsService) {
        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }
        fun event(name: String) = events(name).single()
    }

    private fun session(steps: List<Step>, initial: IntArray = scrambled, savesDir: Path = savesDir()): Run = runBlocking {
        val events = mutableListOf<TerminalEvent>()
        val acts = mutableMapOf<Int, () -> Unit>()
        for (step in steps) {
            when (step) {
                is Event -> events += step.event
                is Act -> acts.merge(events.size, step.block) { a, b -> { a(); b() } }
            }
        }
        val terminal = FakeTerminal(events, terminalSize, acts)
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState(initial) }
        GameSession(
            initialMode = InputMode.MOUSE,
            board = board,
            saves = FileSaveRepository(savesDir),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            terminalFactory = { terminal },
            mordie = Mordie(factory),
        ).run()
        Run(terminal, board, analytics)
    }

    private fun savesDir(vararg files: Pair<String, String>): Path = createTempDirectory("mordovorot-gh83").also { dir ->
        for ((name, content) in files) dir.resolve("$name.save").writeText(content)
    }

    private fun tiles(state: IntArray) = state.joinToString(" ")

    private fun click(p: Pair<Int, Int>) = Event(TerminalEvent.MouseClick(p.first, p.second))
    private fun key(event: TerminalEvent) = Event(event)

    private fun left(row: Int) = click(layout.leftArrowPosition(row))
    private fun up(col: Int) = click(layout.upArrowPosition(col))
    private fun down(col: Int) = click(layout.downArrowPosition(col))

    /** [count] shifts that never solve [scrambled]: column 0 up, over and over. */
    private fun shifts(count: Int) = List(count) { up(0) }

    private fun progress(moves: Int, found: Boolean, elapsedMs: Long = 0) =
        Act { factory.latest.progress = SearchProgress(moves, found, elapsedMs) }

    /** Toolbar Save, type [name], Enter. */
    private fun save(name: String): List<Step> =
        listOf(click(layout.saveButtonPosition())) + name.map { key(TerminalEvent.KeyPress(it)) } + key(TerminalEvent.Enter)

    /** A click on [dialog]'s button [id] - mouse-mode dialogs ignore Tab and Enter. */
    private fun clickButton(dialog: Dialog, id: String): Step {
        val dialogLayout = DialogLayout(dialog, terminalSize)
        val button = dialogLayout.buttons().first { it.target == HitTarget.DialogButton(id) }
        return Event(TerminalEvent.MouseClick(button.x, dialogLayout.buttonsRow()))
    }

    /** Toolbar Load, then the dialog's Load button with the first of [listed] selected. */
    private fun load(vararg listed: String): List<Step> {
        val dialog = Dialog(
            Dialog.Kind.LOAD,
            "Load game",
            listItems = listed.toList(),
            selectedIndex = 0,
            buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
        )
        return listOf(click(layout.loadButtonPosition()), clickButton(dialog, "load"))
    }

    /** Toolbar New, then the size dialog's [side] button. */
    private fun newGame(side: Int): List<Step> {
        val dialog = Dialog(
            Dialog.Kind.SIZE,
            "New game size",
            buttons = (3..5).map { DialogButtonSpec(it.toString(), "${it}x$it") } + DialogButtonSpec("cancel", "Cancel"),
        )
        return listOf(click(layout.newButtonPosition()), clickButton(dialog, side.toString()))
    }

    /** Toolbar Exit, then No on Save-before-quitting. */
    private fun exitWithoutSaving(): List<Step> {
        val dialog = Dialog(
            Dialog.Kind.EXIT,
            "Save before quitting?",
            buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel")),
        )
        return listOf(click(layout.exitButtonPosition()), clickButton(dialog, "no"))
    }

    /** Repair & load on the Can't-be-solved dialog for the save shown as [display]. */
    private fun repair(display: String): Step = clickButton(
        Dialog(
            Dialog.Kind.UNSOLVABLE,
            "Can't be solved",
            message = "'$display' can't be solved - no sequence of shifts reaches the solved board.",
            buttons = listOf(
                DialogButtonSpec("repair", "Repair & load"),
                DialogButtonSpec("new", "New game"),
                DialogButtonSpec("exit", "Exit"),
            ),
        ),
        "repair",
    )

    @Test
    fun `new game greets for shifts 0-4, then the line empties`() {
        // WU4
        // TODO: 1. Start a scrambled 4x4; fake progress N=3, not found.
        // TODO: 2. Frame 0 shows "Mordie: Alright! Let's sort this one out!" under "Moves: 0".
        // TODO: 3. After shifts 1-4 the greeting is still shown.
        // TODO: 4. After shift 5 the Mordie row is blank.
    }

    @Test
    fun `progress shows at shifts 20-24, empties at 25, and gets the hard prefix from 60`() {
        // WU4
        // TODO: 1. Fake progress N=6 up to shift 19, N=7 right after the shift-20 check.
        // TODO: 2. Shift 19: the Mordie row is blank.
        // TODO: 3. Shift 20: "Mordie: I have already made 6 moves and have not solved it yet!" (the value at the check).
        // TODO: 4. Shift 24: same text.
        // TODO: 5. Shift 25: blank.
        // TODO: 6. Set N=9 before shift 60; shift 60: "Mordie: This one is hard! I have already made 9 moves and have not solved it yet!"
    }

    @Test
    fun `found is revealed at the next 5-shift check and stays until solved`() {
        // WU3 (analytics) + WU4 (line)
        val run = session(shifts(7) + progress(11, found = true, elapsedMs = 1234) + shifts(23))

        assertEquals(
            mapOf(
                "board_size" to 4,
                "solution_length" to 11,
                "player_shift_count" to 10,
                "search_duration_ms" to 1234L,
                "resumed_from_save" to false,
                "input_method" to "mouse",
            ),
            run.event("mordie_solution_found"),
        )
        // TODO (WU4): shifts 8 and 9: the Mordie row is still blank.
        // TODO (WU4): shift 10: "Mordie: Found it! I had to make 11 moves to find the solution! Can you beat it?"
        // TODO (WU4): shifts 11-30: the same line on every frame.
    }

    @Test
    fun `solving before Mordie finds it - You beat me`() {
        // WU3 (analytics) + WU4 (title)
        val run = session(listOf(progress(0, found = false), left(1)), initial = oneShiftFromSolved)

        assertEquals("player_won", run.event("board_solved")["mordie_result"])
        assertEquals(0, run.event("board_solved")["mordie_moves"])
        // TODO (WU4): title is "You beat me! I was still at 0 moves."; the Mordie row is blank.
    }

    @Test
    fun `matching Mordie - Cool, you matched me`() {
        // WU3 (analytics) + WU4 (title)
        val run = session(listOf(progress(1, found = true), left(1)), initial = oneShiftFromSolved)

        assertEquals("matched", run.event("board_solved")["mordie_result"])
        assertEquals(1, run.event("board_solved")["mordie_moves"])
        // TODO (WU4): title is "Cool, you matched me! 1 move each!" (singular).
    }

    @Test
    fun `more moves than Mordie - Sorry, but I win`() {
        // WU3 (analytics) + WU4 (title)
        // A column shift and back is one move (GH-62), so this solves in 2.
        val run = session(listOf(progress(1, found = true), up(0), down(0), left(1)), initial = oneShiftFromSolved)

        assertEquals(2, run.event("board_solved")["move_count"])
        assertEquals("mordie_won", run.event("board_solved")["mordie_result"])
        assertEquals(1, run.event("board_solved")["mordie_moves"])
        // TODO (WU4): title is "Sorry, but I win: 1 move vs your 2."
    }

    @Test
    fun `fewer moves than Mordie's N - Are you a machine`() {
        // WU3 (analytics) + WU4 (title)
        val dir = savesDir("m" to "4\n${tiles(oneShiftFromSolved)}\n0\n${tiles(oneShiftFromSolved)}\n5 found\n")

        val run = session(load("m (4x4)") + left(1), savesDir = dir)

        assertEquals("machine", run.event("board_solved")["mordie_result"])
        assertEquals(5, run.event("board_solved")["mordie_moves"])
        // TODO (WU4): title is "I made 5 moves and you made 1 move! Are you a machine?"
    }

    @Test
    fun `save and load while searching resumes at the saved N`() {
        // WU3 (save/resume) + WU4 (line)
        val dir = savesDir()

        session(shifts(7) + progress(8, found = false) + save("m") + shifts(1) + load("m (4x4)"), savesDir = dir)

        val lines = dir.resolve("m.save").readLines()
        assertEquals(tiles(scrambled), lines[3])
        assertEquals("8 searching", lines[4])
        assertEquals(2, factory.started.size)
        assertTrue(factory.started[0].cancelled)
        assertContentEquals(scrambled, factory.latest.original)
        assertEquals(8, factory.latest.ruledOut)
        // TODO (WU4): line is "Mordie: Okay, so we stopped when I was at 8 moves! What was I doing?.."
        // TODO (WU4): the shift count restarted: after 5 more shifts the Mordie row is blank.
    }

    @Test
    fun `loading a found save shows Found it immediately and starts no search`() {
        // WU3 + WU4
        val dir = savesDir("m" to "4\n${tiles(oneShiftFromSolved)}\n0\n${tiles(oneShiftFromSolved)}\n11 found\n")

        val run = session(load("m (4x4)") + shifts(5), savesDir = dir)

        assertEquals(1, factory.started.size)
        assertTrue(factory.started[0].cancelled)
        assertTrue(run.events("mordie_solution_found").isEmpty())
        // TODO (WU4): the first frame after the load shows "Mordie: Found it! I had to make 11 moves to find the solution! Can you beat it?"
    }

    @Test
    fun `a pre-Mordie save starts Mordie fresh from the loaded board and keeps the move count`() {
        // WU3 + WU4
        val dir = savesDir("old" to "4\n${tiles(oneShiftFromSolved)}\n4\n")

        val run = session(load("old (4x4)"), savesDir = dir)

        assertEquals(4, run.board.moveCount)
        assertEquals(2, factory.started.size)
        assertContentEquals(oneShiftFromSolved, factory.latest.original)
        assertEquals(-1, factory.latest.ruledOut)
        // TODO (WU4): the frame shows "Moves: 4" and "Mordie: Alright! Let's sort this one out!"
    }

    @Test
    fun `a repaired unsolvable save starts Mordie fresh from the repaired board`() {
        // WU3
        val odd = "2 1 0 3 4 5 6 7 8"
        val dir = savesDir("odd" to "3\n$odd\n5\n$odd\n2 searching\n")

        val run = session(load("odd (3x3) unsolvable") + repair("odd (3x3)"), savesDir = dir)

        assertEquals(listOf(1, 2, 0, 3, 4, 5, 6, 7, 8), run.board.boardArray.toList())
        assertEquals(5, run.board.moveCount)
        assertEquals(2, factory.started.size)
        assertContentEquals(run.board.boardArray, factory.latest.original)
        assertEquals(-1, factory.latest.ruledOut)
        // TODO (WU4): the greeting is the fresh one.
    }

    @Test
    fun `a solved save with Mordie data shows its ending, without data Congratulations`() {
        // WU3 + WU4
        val solved = tiles(IntArray(16) { it })
        val withData = session(load("m (4x4)"), savesDir = savesDir("m" to "4\n$solved\n3\n${tiles(oneShiftFromSolved)}\n3 found\n"))
        val withoutData = session(load("m (4x4)"), savesDir = savesDir("m" to "4\n$solved\n3\n"))

        assertTrue(withData.events("board_solved").isEmpty())
        assertTrue(withoutData.events("board_solved").isEmpty())
        // TODO (WU4): with data, the title is "Cool, you matched me! 3 moves each!"
        // TODO (WU4): without data, the title is "Congratulations ✓".
    }

    @Test
    fun `switching input mode keeps the shift count and line text`() {
        // WU4
        // TODO: 1. Make 3 shifts, then click [ Keyboard ].
        // TODO: 2. The first keyboard-mode frame shows the same greeting.
        // TODO: 3. After 2 keyboard shifts (shift 5 overall) the Mordie row is blank.
        // TODO: 4. The factory was started only once.
    }

    @Test
    fun `New cancels the running search and Exit stops it`() {
        // WU3
        val run = session(newGame(4) + exitWithoutSaving())

        assertEquals(2, factory.started.size)
        assertTrue(factory.started[0].cancelled)
        assertContentEquals(run.board.boardArray, factory.latest.original)
        assertTrue(factory.latest.cancelled)
        assertEquals("declined", run.event("exit_command_used")["save_choice"])
    }
}
