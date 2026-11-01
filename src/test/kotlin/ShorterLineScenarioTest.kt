import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import mordie.Mordie
import mordie.SearchProgress
import mordie.SolutionMove
import mordie.SolutionSource
import storage.FileSaveRepository
import kotlin.io.path.createTempDirectory
import testing.FakeMordieSearchFactory
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import testing.testGameSession
import view.tui.BoardLayout
import view.tui.TerminalEvent
import view.tui.TerminalSize

/** GH-223 scenarios: Mordie's lines and endings for an answer that may not be the shortest.
 *  Same setup as [BestSolutionScenarioTest]: real BoardImpl + ViewModelImpl in a mouse-mode
 *  GameSession over a FakeTerminal, with `Mordie(FakeMordieSearchFactory())` scripting the search. */
class ShorterLineScenarioTest {

    private val wide = TerminalSize(columns = 80, rows = 40)
    private val factory = FakeMordieSearchFactory()

    private sealed interface Step
    private class Event(val event: TerminalEvent) : Step
    private class Act(val block: () -> Unit) : Step

    private class Run(val analytics: RecordingAnalyticsService) {
        fun events(name: String) = analytics.events.filter { it.name == name }.map { it.properties }
    }

    private lateinit var terminal: FakeTerminal
    private lateinit var board: BoardImpl
    private val snaps = mutableListOf<String>()

    /** A scrambled [side]x[side] board, never one shift from solved (reversed: even, so solvable). */
    private fun scrambled(side: Int) = IntArray(side * side) { side * side - 1 - it }

    /** The solved [side]x[side] board with column 0 shifted down once: one up-shift solves it. */
    private fun oneShiftFromSolved(side: Int) = IntArray(side * side) { it }.also { tiles ->
        for (row in 0 until side) tiles[row * side] = ((row + side - 1) % side) * side
    }

    private fun session(side: Int, steps: List<Step>, size: TerminalSize = wide): Run = runBlocking {
        val events = mutableListOf<TerminalEvent>()
        val acts = mutableMapOf<Int, () -> Unit>()
        for (step in steps) {
            when (step) {
                is Event -> events += step.event
                is Act -> acts.merge(events.size, step.block) { a, b -> { a(); b() } }
            }
        }
        terminal = FakeTerminal(events, size, acts)
        val analytics = RecordingAnalyticsService()
        board = BoardImpl(side).apply { restoreState(scrambled(side)) }
        testGameSession(
            initialMode = InputMode.MOUSE,
            board = board,
            saves = FileSaveRepository(createTempDirectory("mordovorot-gh223")),
            analytics = analytics,
            sizeChosenAtLaunch = true,
            terminalFactory = { terminal },
            mordie = Mordie(factory),
        ).run()
        Run(analytics)
    }

    // --- steps ---

    private fun snap() = Act { snaps += terminal.frames.last() }
    private fun click(p: Pair<Int, Int>) = Event(TerminalEvent.MouseClick(p.first, p.second))
    private fun script(progress: SearchProgress) = Act { factory.latest.progress = progress }

    private fun snappedShifts(side: Int, count: Int, size: TerminalSize = wide) =
        List(count) { listOf(click(BoardLayout(size, side).upArrowPosition(0)), snap()) }.flatten()

    private fun shifts(side: Int, count: Int) = List(count) { click(BoardLayout(wide, side).upArrowPosition(0)) }

    private fun path(length: Int, amount: Int = 1) = List(length) { SolutionMove(isRow = true, index = 0, amount = amount) }

    private fun fast(moves: Int) = SearchProgress(
        moves, found = true, elapsedMs = 4, solution = path(moves),
        ruledOut = 3, provenShortest = false, source = SolutionSource.FAST_SOLVER,
    )

    private fun exact(moves: Int) = SearchProgress(moves, found = true, elapsedMs = 9, solution = path(moves, amount = 2))

    /** The player wins by one up-shift of a board put one shift from solved after the reveal. */
    private fun winAfterReveal(side: Int) =
        shifts(side, 5) + Act { board.restoreState(oneShiftFromSolved(side)) } + shifts(side, 1)

    // --- reading a frame ---

    private fun rows(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")

    private fun mordieShown(frame: String, side: Int, size: TerminalSize = wide) =
        BoardLayout(size, side).let { rows(frame).subList(it.mordieRow, it.mordieRow + it.mordieRows) }
            .map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

    private fun titleShown(frame: String, side: Int) = rows(frame)[BoardLayout(wide, side).titleRow].trim()

    private fun unproven(moves: Int) = "Mordie: Found one in $moves moves, but maybe there's a shorter one! Can you beat it?"

    private fun shorter(moves: Int) = "Mordie: Wait, I found a shorter one: $moves moves now! Can you beat it?"

    // --- scenarios ---

    @Test
    fun `an unproven fast answer shows the honest line`() {
        val run = session(5, listOf(script(fast(40))) + snappedShifts(5, 5))

        assertEquals(unproven(40), mordieShown(snaps[4], 5))
        assertEquals(false, run.events("mordie_solution_found").single()["proven_shortest"])
    }

    @Test
    fun `a proven answer keeps the Found it line`() {
        val run = session(4, listOf(script(exact(12))) + snappedShifts(4, 5))

        assertEquals("Mordie: Found it! I had to make 12 moves to find the solution! Can you beat it?", mordieShown(snaps[4], 4))
        assertEquals(true, run.events("mordie_solution_found").single()["proven_shortest"])
    }

    @Test
    fun `a shorter answer is announced at the next check`() {
        val run = session(
            5,
            listOf(script(fast(40))) + snappedShifts(5, 5) + script(fast(31)) + snappedShifts(5, 5) +
                script(fast(25)) + snappedShifts(5, 5),
        )

        assertEquals(unproven(40), mordieShown(snaps[8], 5))
        assertEquals(shorter(31), mordieShown(snaps[9], 5))
        assertEquals(shorter(31), mordieShown(snaps[13], 5))
        assertEquals(shorter(25), mordieShown(snaps[14], 5))
        val drops = run.events("mordie_shorter_found")
        assertEquals(31, drops[0]["solution_length"])
        assertEquals(40, drops[0]["previous_length"])
        assertEquals(false, drops[0]["proven_shortest"])
        assertEquals(2, drops.size)
    }

    @Test
    fun `a check with no drop announces nothing`() {
        val run = session(5, listOf(script(fast(40))) + snappedShifts(5, 5) + script(exact(40)) + snappedShifts(5, 5))

        assertEquals(unproven(40), mordieShown(snaps[9], 5))
        assertEquals(0, run.events("mordie_shorter_found").size)
    }

    @Test
    fun `beating an unproven answer ends with the new ending`() {
        val run = session(5, listOf(script(fast(40))) + winAfterReveal(5))

        val title = titleShown(terminal.frames.last(), 5)
        assertEquals("You beat me! 40 moves vs your 1. Mine may not have been the shortest.", title)
        assertEquals("beat_mordie", run.events("board_solved").single()["mordie_result"])
        assertTrue(title.length <= wide.columns)
    }

    @Test
    fun `beating a proven answer is still a machine`() {
        val run = session(5, listOf(script(exact(12))) + winAfterReveal(5))

        assertEquals("I made 12 moves and you made 1 move! Are you a machine?", titleShown(terminal.frames.last(), 5))
        assertEquals("machine", run.events("board_solved").single()["mordie_result"])
    }

    @Test
    fun `a narrow terminal shows the whole unproven line at 99 moves`() {
        val narrow = TerminalSize(columns = 30, rows = 40)

        session(4, listOf(script(fast(99))) + snappedShifts(4, 5, narrow), size = narrow)

        assertEquals(unproven(99), mordieShown(snaps[4], 4, narrow))
    }
}
