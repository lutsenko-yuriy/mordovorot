import board_model.BoardImpl
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import mordie.SolutionSource
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
import viewmodel.GiveUpButton

/** GH-222 scenarios: Mordie keeps the best solution so far, from the fast solver and the exact
 *  search. Same setup as [MordieScenarioTest]: real BoardImpl + ViewModelImpl + FileSaveRepository
 *  (temp dir) in a mouse-mode GameSession over a FakeTerminal, with `Mordie(FakeMordieSearchFactory())`
 *  scripting the search's progress. */
class BestSolutionScenarioTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    private val factory = FakeMordieSearchFactory()

    private sealed interface Step
    private class Event(val event: TerminalEvent) : Step
    private class Act(val block: () -> Unit) : Step

    private class Run(val terminal: FakeTerminal, val board: BoardImpl, val analytics: RecordingAnalyticsService) {
        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }
        fun event(name: String) = events(name).single()
    }

    private lateinit var terminal: FakeTerminal
    private val snaps = mutableListOf<String>()

    /** A scrambled [side]x[side] board, never one shift from solved (reversed: even, so solvable). */
    private fun scrambled(side: Int) = IntArray(side * side) { side * side - 1 - it }

    private fun session(
        side: Int,
        steps: List<Step>,
        initial: IntArray = scrambled(side),
        savesDir: Path = savesDir(),
    ): Run = runBlocking {
        val events = mutableListOf<TerminalEvent>()
        val acts = mutableMapOf<Int, () -> Unit>()
        for (step in steps) {
            when (step) {
                is Event -> events += step.event
                is Act -> acts.merge(events.size, step.block) { a, b -> { a(); b() } }
            }
        }
        val terminal = FakeTerminal(events, terminalSize, acts).also { this@BestSolutionScenarioTest.terminal = it }
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(side).apply { restoreState(initial) }
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

    private fun savesDir(): Path = createTempDirectory("mordovorot-gh222")

    // --- steps ---

    private fun snap() = Act { snaps += terminal.frames.last() }
    private fun click(p: Pair<Int, Int>?) = Event(TerminalEvent.MouseClick(p!!.first, p.second))
    private fun key(event: TerminalEvent) = Event(event)

    /** [count] shifts of column 0 up, each followed by a [snap]. */
    private fun snappedShifts(side: Int, count: Int) = List(count) { listOf(click(BoardLayout(terminalSize, side).upArrowPosition(0)), snap()) }.flatten()

    private fun shifts(side: Int, count: Int) = List(count) { click(BoardLayout(terminalSize, side).upArrowPosition(0)) }

    private fun script(progress: SearchProgress) = Act { factory.latest.progress = progress }

    private fun path(length: Int, amount: Int = 1) = List(length) { SolutionMove(isRow = true, index = 0, amount = amount) }

    private fun fast(moves: Int, ruledOut: Int = 3) = SearchProgress(
        moves, found = true, elapsedMs = 4, solution = path(moves),
        ruledOut = ruledOut, provenShortest = false, source = SolutionSource.FAST_SOLVER,
    )

    private fun exact(moves: Int) = SearchProgress(moves, found = true, elapsedMs = 9, solution = path(moves, amount = 2))

    private val confirm = Dialog(
        Dialog.Kind.GIVE_UP, "Give up and see Mordie's solution?",
        buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("cancel", "No")),
    )

    private fun clickButton(dialog: Dialog, id: String): Step {
        val dialogLayout = DialogLayout(dialog, terminalSize)
        val button = dialogLayout.buttons().first { it.target == HitTarget.DialogButton(id) }
        return Event(TerminalEvent.MouseClick(button.x, dialogLayout.buttonsRow()))
    }

    private fun giveUpButton(side: Int) = click(BoardLayout(terminalSize, side, giveUp = GiveUpButton.GIVE_UP).giveUpButtonPosition())

    private fun save(side: Int, name: String): List<Step> =
        listOf(click(BoardLayout(terminalSize, side).saveButtonPosition())) + name.map { key(TerminalEvent.KeyPress(it)) } + key(TerminalEvent.Enter)

    private fun load(side: Int, listed: String): List<Step> {
        val dialog = Dialog(
            Dialog.Kind.LOAD, "Load game", listItems = listOf(listed), selectedIndex = 0,
            buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
        )
        return listOf(click(BoardLayout(terminalSize, side).loadButtonPosition()), clickButton(dialog, "load"))
    }

    // --- reading a frame ---

    private fun rows(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")

    private fun mordieShown(frame: String, side: Int) =
        BoardLayout(terminalSize, side).let { rows(frame).subList(it.mordieRow, it.mordieRow + it.mordieRows) }
            .map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

    private fun giveUpDimmed(frame: String) = "\u001B[2m[ Give Up ]\u001B[22m" in frame

    private fun found(moves: Int) = "Mordie: Found it! I had to make $moves moves to find the solution! Can you beat it?"

    // --- scenarios ---

    @Test
    fun `give up is available at the first check on a 5x5 once the fast solver has answered`() {
        val run = session(5, listOf(script(fast(40))) + snappedShifts(5, 4) + snappedShifts(5, 1))

        assertTrue(giveUpDimmed(snaps[3]))
        assertEquals(found(40), mordieShown(snaps[4], 5))
        assertFalse(giveUpDimmed(snaps[4]))
        val event = run.event("mordie_solution_found")
        assertEquals("fast_solver", event["source"])
        assertEquals(false, event["proven_shortest"])
        assertEquals(40, event["solution_length"])
    }

    @Test
    fun `Mordie's N drops when the exact search finds a shorter solution`() {
        val run = session(
            3,
            listOf(script(fast(8))) + snappedShifts(3, 5) + script(exact(6)) + snappedShifts(3, 5) +
                giveUpButton(3) + clickButton(confirm, "yes"),
        )

        assertEquals(found(8), mordieShown(snaps[4], 3))
        assertEquals(found(6), mordieShown(snaps[9], 3))
        val given = run.event("game_given_up")
        assertEquals(6, given["solution_length"])
        assertEquals(true, given["solution_proven_shortest"])
    }

    @Test
    fun `giving up before the exact search finishes shows the fast solution and says so in analytics`() {
        val run = session(
            4,
            listOf(script(fast(12))) + shifts(4, 5) + giveUpButton(4) + clickButton(confirm, "yes") + snap() +
                script(exact(5)) + snap(),
        )

        assertEquals("Mordie: Here's how I did it in 12 moves!", mordieShown(snaps[0], 4))
        assertEquals("Mordie: Here's how I did it in 12 moves!", mordieShown(snaps[1], 4))
        assertEquals(false, run.event("game_given_up")["solution_proven_shortest"])
        assertEquals(12, run.event("screen_solution")["solution_length"])
    }

    @Test
    fun `solving the board reports whether Mordie's answer was proven shortest`() {
        // Column 0 shifted up twice: five more up-shifts reveal Mordie's answer, the sixth solves.
        val twoShiftsFromSolved = intArrayOf(8, 1, 2, 3, 12, 5, 6, 7, 0, 9, 10, 11, 4, 13, 14, 15)

        val run = session(4, listOf(script(fast(12))) + shifts(4, 6), initial = twoShiftsFromSolved)

        val solved = run.event("board_solved")
        assertEquals(12, solved["mordie_moves"])
        assertEquals(false, solved["mordie_proven_shortest"])
    }

    @Test
    fun `an unproven answer is not saved as found`() {
        val dir = savesDir()
        session(4, listOf(script(fast(12, ruledOut = 7))) + shifts(4, 5) + save(4, "x"), savesDir = dir)

        val run = session(4, load(4, "x (4x4)") + snap() + script(fast(12)) + snappedShifts(4, 5), savesDir = dir)

        run.event("load_command_used")
        assertEquals("Mordie: Okay, so we stopped when I was at 7 moves! What was I doing?..", mordieShown(snaps[0], 4))
        assertTrue(giveUpDimmed(snaps[0]))
        assertEquals(found(12), mordieShown(snaps.last(), 4))
    }

    @Test
    fun `a proven answer is saved as found`() {
        // A save checks that the solution solves the original: 4x4, two moves from solved.
        val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
        val twoMoves = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))
        val dir = savesDir()
        session(
            4,
            listOf(script(SearchProgress(2, found = true, elapsedMs = 9, solution = twoMoves))) + shifts(4, 5) + save(4, "x"),
            initial = original,
            savesDir = dir,
        )

        session(4, load(4, "x (4x4)") + snap() + shifts(4, 1), savesDir = dir)

        assertFalse(giveUpDimmed(snaps[0]))
        assertEquals(found(2), mordieShown(snaps[0], 4))
    }
}
