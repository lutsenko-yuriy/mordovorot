import board_model.BoardImpl
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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

        /** Fails unless the session's one load succeeded - so a load scenario can't pass without it. */
        fun assertLoaded() = assertEquals("success", event("load_command_used")["result"])
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
        val terminal = FakeTerminal(events, terminalSize, acts).also { this@MordieScenarioTest.terminal = it }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(4).apply { restoreState(initial) }
        testGameSession(
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

    /** The terminal of the session running now, for [snap]. */
    private lateinit var terminal: FakeTerminal

    /** The frames [snap] captured, in order. */
    private val snaps = mutableListOf<String>()

    /** Captures the frame on screen now - after the previous event, before the next one. */
    private fun snap() = Act { snaps += terminal.frames.last() }

    /** [count] shifts, each followed by a [snap]. */
    private fun snappedShifts(count: Int) = List(count) { listOf(up(0), snap()) }.flatten()

    private fun rows(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")

    /** The `Mordie:` rows' text joined into one line, `""` when they are blank. */
    private fun mordieShown(frame: String, side: Int = 4) =
        BoardLayout(terminalSize, side).let { rows(frame).subList(it.mordieRow, it.mordieRow + it.mordieRows) }.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

    private fun titleShown(frame: String) = rows(frame)[layout.titleRow].trim()

    private val Run.lastFrame get() = terminal.frames.last()

    private val greeting = "Mordie: Alright! Let's sort this one out!"
    private val found11 = "Mordie: Found it! I had to make 11 moves to find the solution! Can you beat it?"

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
        session(listOf(progress(3, found = false), snap()) + snappedShifts(5))

        assertEquals("Moves: 0", rows(snaps[0])[layout.movesRow].trim())
        for (shift in 0..4) assertEquals(greeting, mordieShown(snaps[shift]), "shift $shift")
        assertEquals("", mordieShown(snaps[5]))
    }

    @Test
    fun `progress shows at shifts 20-24, empties at 25, and gets the hard prefix from 60`() {
        session(
            listOf(progress(6, found = false)) + shifts(18) + snappedShifts(2) + progress(7, found = false) +
                shifts(3) + snappedShifts(2) + shifts(34) + progress(9, found = false) + snappedShifts(1),
        )

        val progress6 = "Mordie: I have already made 6 moves and have not solved it yet!"
        assertEquals(listOf("", progress6, progress6, ""), snaps.take(4).map(::mordieShown)) // shifts 19, 20, 24, 25
        assertEquals("Mordie: This one is hard! I have already made 9 moves and have not solved it yet!", mordieShown(snaps[4]))
    }

    @Test
    fun `progress is skipped when Mordie's N hasn't changed since it last said it`() {
        session(listOf(progress(6, found = false)) + shifts(19) + snappedShifts(1) + shifts(19) + snappedShifts(1))

        assertEquals("Mordie: I have already made 6 moves and have not solved it yet!", mordieShown(snaps[0])) // shift 20
        assertEquals("", mordieShown(snaps[1])) // shift 40, still 6
    }

    @Test
    fun `found is revealed at the next 5-shift check and stays until solved`() {
        // WU3 (analytics) + WU4 (line)
        val run = session(shifts(7) + progress(11, found = true, elapsedMs = 1234) + snappedShifts(23))

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
        assertEquals(listOf("", ""), snaps.take(2).map(::mordieShown)) // shifts 8, 9
        for (shift in 10..30) assertEquals(found11, mordieShown(snaps[shift - 8]), "shift $shift")
    }

    @Test
    fun `solving before Mordie finds it - You beat me`() {
        // WU3 (analytics) + WU4 (title)
        val run = session(listOf(progress(0, found = false), left(1)), initial = oneShiftFromSolved)

        assertEquals("player_won", run.event("board_solved")["mordie_result"])
        assertEquals(0, run.event("board_solved")["mordie_moves"])
        assertEquals("You beat me! I was still at 0 moves.", titleShown(run.lastFrame))
        assertEquals("", mordieShown(run.lastFrame))
    }

    @Test
    fun `matching Mordie - Cool, you matched me`() {
        // WU3 (analytics) + WU4 (title)
        val run = session(listOf(progress(1, found = true), left(1)), initial = oneShiftFromSolved)

        assertEquals("matched", run.event("board_solved")["mordie_result"])
        assertEquals(1, run.event("board_solved")["mordie_moves"])
        assertEquals("Cool, you matched me! 1 move each!", titleShown(run.lastFrame))
    }

    @Test
    fun `more moves than Mordie - Sorry, but I win`() {
        // WU3 (analytics) + WU4 (title)
        // A column shift and back is one move (GH-62), so this solves in 2.
        val run = session(listOf(progress(1, found = true), up(0), down(0), left(1)), initial = oneShiftFromSolved)

        assertEquals(2, run.event("board_solved")["move_count"])
        assertEquals("mordie_won", run.event("board_solved")["mordie_result"])
        assertEquals(1, run.event("board_solved")["mordie_moves"])
        assertEquals("Sorry, but I win: 1 move vs your 2.", titleShown(run.lastFrame))
    }

    @Test
    fun `fewer moves than Mordie's N - Are you a machine`() {
        // WU3 (analytics) + WU4 (title)
        val dir = savesDir("m" to "4\n${tiles(oneShiftFromSolved)}\n0\n${tiles(oneShiftFromSolved)}\n5 found\n")

        val run = session(load("m (4x4)") + left(1), savesDir = dir)

        assertEquals("machine", run.event("board_solved")["mordie_result"])
        assertEquals(5, run.event("board_solved")["mordie_moves"])
        assertEquals("I made 5 moves and you made 1 move! Are you a machine?", titleShown(run.lastFrame))
    }

    @Test
    fun `save and load while searching resumes at the saved N`() {
        // WU3 (save/resume) + WU4 (line)
        val dir = savesDir()

        session(
            shifts(7) + progress(8, found = false) + save("m") + shifts(1) + load("m (4x4)") + snap() + snappedShifts(5),
            savesDir = dir,
        )

        val lines = dir.resolve("m.save").readLines()
        assertEquals(tiles(scrambled), lines[3])
        assertEquals("8 searching", lines[4])
        assertEquals(2, factory.started.size)
        assertTrue(factory.started[0].cancelled)
        assertContentEquals(scrambled, factory.latest.original)
        assertEquals(8, factory.latest.ruledOut)
        val resumed = "Mordie: Okay, so we stopped when I was at 8 moves! What was I doing?.."
        for (shift in 0..4) assertEquals(resumed, mordieShown(snaps[shift]), "shift $shift after the load")
        assertEquals("", mordieShown(snaps[5])) // the shift count restarted at the load
    }

    @Test
    fun `loading a found save shows Found it immediately and starts no search`() {
        // WU3 + WU4
        val dir = savesDir("m" to "4\n${tiles(oneShiftFromSolved)}\n0\n${tiles(oneShiftFromSolved)}\n11 found\n")

        val run = session(load("m (4x4)") + snap() + shifts(5), savesDir = dir)

        run.assertLoaded()
        assertEquals(1, factory.started.size)
        assertTrue(factory.started[0].cancelled)
        assertTrue(run.events("mordie_solution_found").isEmpty())
        assertEquals(found11, mordieShown(snaps.single()))
    }

    @Test
    fun `an unplayed pre-Mordie save starts Mordie fresh from the loaded board`() {
        // WU3 + WU4
        val dir = savesDir("old" to "4\n${tiles(oneShiftFromSolved)}\n0\n")

        val run = session(load("old (4x4)"), savesDir = dir)

        assertEquals(2, factory.started.size)
        assertContentEquals(oneShiftFromSolved, factory.latest.original)
        assertEquals(-1, factory.latest.ruledOut)
        assertEquals(greeting, mordieShown(run.lastFrame))
    }

    @Test
    fun `Mordie sits out a pre-Mordie save that already has moves`() {
        // WU3 + WU4 - its N would cover only the moves after the load (audit on PR #88).
        val dir = savesDir("old" to "4\n${tiles(oneShiftFromSolved)}\n4\n")

        val run = session(load("old (4x4)") + snap() + left(1), savesDir = dir)

        assertEquals(5, run.board.moveCount)
        assertEquals(1, factory.started.size)
        assertTrue(factory.started[0].cancelled)
        assertFalse("mordie_result" in run.event("board_solved"))
        assertFalse("mordie_moves" in run.event("board_solved"))
        assertEquals("", mordieShown(snaps.single()))
        assertEquals("Congratulations ✓", titleShown(run.lastFrame))
    }

    @Test
    fun `a repaired unsolvable save starts Mordie fresh from the repaired board`() {
        // WU3 - a repaired save that already has moves gets no Mordie, like a pre-Mordie one.
        val odd = "2 1 0 3 4 5 6 7 8"
        val dir = savesDir("odd" to "3\n$odd\n0\n$odd\n2 searching\n")

        val run = session(load("odd (3x3) unsolvable") + repair("odd (3x3)"), savesDir = dir)

        assertEquals(listOf(1, 2, 0, 3, 4, 5, 6, 7, 8), run.board.boardArray.toList())
        assertEquals(2, factory.started.size)
        assertContentEquals(run.board.boardArray, factory.latest.original)
        assertEquals(-1, factory.latest.ruledOut)
        assertEquals(greeting, mordieShown(run.lastFrame, side = 3))
    }

    @Test
    fun `a solved save with Mordie data shows its ending, without data Congratulations`() {
        // WU3 + WU4
        val solved = tiles(IntArray(16) { it })
        val withData = session(load("m (4x4)"), savesDir = savesDir("m" to "4\n$solved\n3\n${tiles(oneShiftFromSolved)}\n3 found\n"))
        val withoutData = session(load("m (4x4)"), savesDir = savesDir("m" to "4\n$solved\n3\n"))

        withData.assertLoaded()
        withoutData.assertLoaded()
        assertTrue(withData.events("board_solved").isEmpty())
        assertTrue(withoutData.events("board_solved").isEmpty())
        assertEquals("Cool, you matched me! 3 moves each!", titleShown(withData.lastFrame))
        assertEquals("Congratulations ✓", titleShown(withoutData.lastFrame))
    }

    @Test
    fun `switching input mode keeps the shift count and line text`() {
        val keyboardButton = BoardLayout(terminalSize, 4, modeButtons = listOf(InputMode.KEYBOARD))
            .toolbarButtons().first { it.target == HitTarget.ToolbarMode(InputMode.KEYBOARD) }

        session(
            shifts(3) + snap() + click(keyboardButton.x to keyboardButton.y) + snap() +
                key(TerminalEvent.Enter) + snap() + key(TerminalEvent.Enter) + snap(),
        )

        assertEquals(greeting, mordieShown(snaps[0]))
        assertTrue(rows(snaps[1]).any { "Arrows: move" in it }) // keyboard mode
        assertEquals(listOf(greeting, greeting, ""), snaps.drop(1).map(::mordieShown)) // shifts 3, 4, 5
        assertEquals(1, factory.started.size)
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
