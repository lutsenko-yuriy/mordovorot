package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import mordie.SearchProgress
import mordie.SolutionMove
import mordie.SolutionSource
import storage.SavedMordie
import web.Arrow

/** GH-224 scenarios: saves keep Mordie's best solution so far and whether it is proven shortest,
 *  in the browser, through [WebHarness]. Each name notes the work unit that turns it green. */
class WebKeptSolutionScenarioTest {

    private val five = IntArray(25) { 24 - it }
    private fun path(length: Int) = List(length) { SolutionMove(isRow = true, index = 0, amount = 1) }
    private fun fast(moves: Int) = SearchProgress(
        moves, found = true, elapsedMs = 4, solution = path(moves),
        ruledOut = 3, provenShortest = false, source = SolutionSource.FAST_SOLVER,
    )

    /** A solved 5x5 save with Mordie's [n]-move answer, and the player's one move. */
    private fun WebHarness.seedSolved(n: Int, provenShortest: Boolean) =
        seed("won", solved(5), moves = 1, mordie = SavedMordie(five, n, found = true, provenShortest = provenShortest, ruledOut = if (provenShortest) 0 else 3))

    private fun WebHarness.loadWon() {
        game.onLoad()
        game.onLoadChosen("won")
    }

    @Test
    fun `WU1 a solved save won below an unproven answer reloads with the beat-Mordie ending`() = webTest {
        val page = WebHarness(backgroundScope).seedSolved(40, provenShortest = false).startOn(five)

        page.loadWon()
        assertEquals("You beat me! 40 moves vs your 1. Mine may not have been the shortest.", page.state.mordie)
        assertEquals(emptyList(), page.props("board_solved"))
    }

    @Test
    fun `WU1 a solved save with a proven answer still ends with Are you a machine`() = webTest {
        val page = WebHarness(backgroundScope).seedSolved(12, provenShortest = true).startOn(five)

        page.loadWon()
        assertEquals("I made 12 moves and you made 1 move! Are you a machine?", page.state.mordie)
    }

    @Test
    fun `WU2 a save with an unproven answer shows it at once after loading`() = webTest {
        // TODO: 1. Start a 5x5 game and script the fast answer (40 moves, not proven).
        // TODO: 2. Make five shifts so a check reveals the answer, then save as `foo`.
        // TODO: 3. Load `foo`.
        // TODO: 4. The unproven found line shows at once and Give Up is lit, with no wait for a check.
        // TODO: 5. Give Up: the solution screen shows the kept 40 moves; game_given_up has solution_length=40 and solution_proven_shortest=false.
    }

    @Test
    fun `WU2 after that load only the exact search restarts and a shorter answer is announced`() = webTest {
        // TODO: 1. Do steps 1-3 of the previous scenario.
        // TODO: 2. The newest search was started above R with the kept solution; the fast solver did not run.
        // TODO: 3. Script a shorter exact answer (31 moves) and make five shifts.
        // TODO: 4. The `Wait, I found a shorter one` line shows and mordie_shorter_found has previous_length=40.
    }

    @Test
    fun `a save written before this ticket still resumes with the stopped-at line`() = webTest {
        // TODO: 1. Seed an old-style save: `<R> searching`, no line 9.
        // TODO: 2. Load it.
        // TODO: 3. The first line is `Okay, so we stopped when I was at R moves!...`.
        // TODO: 4. A search starts above R with no kept solution.
    }

    @Test
    fun `WU1 and WU2 a solution-screen save of an unproven answer reopens the screen and starts no search`() = webTest {
        // Row 0 one cell left of solved: 41 steps of "row 0 right once" solve it (41 mod 5 = 1).
        val page = WebHarness(backgroundScope).startOn(IntArray(25) { if (it < 5) (it + 1) % 5 else it })
        page.mordieSearches.latest.progress = fast(41)
        repeat(5) { page.game.onArrow(Arrow.UP, 2) }
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)
        page.game.onSave()
        page.game.onSaveAnswered("foo")
        assertEquals(listOf("foo"), page.saves.listSaves())
        val searches = page.mordieSearches.started.size

        page.game.onLoad()
        page.game.onLoadChosen("foo")

        assertNotNull(page.state.solution)
        assertEquals(searches, page.mordieSearches.started.size)
        assertFalse(page.saves.load("foo")!!.mordie!!.provenShortest)
        page.game.onSave()
        page.game.onSaveAnswered("bar")
        assertFalse(page.saves.load("bar")!!.mordie!!.provenShortest)
    }
}
