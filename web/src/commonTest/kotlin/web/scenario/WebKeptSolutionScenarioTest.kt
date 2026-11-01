package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import mordie.SearchProgress
import mordie.SolutionMove
import mordie.SolutionSource
import storage.SavedMordie
import storage.SavedMove
import viewmodel.GiveUpButton
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

    /** Row 0 one cell left of solved: 41 steps of "row 0 right once" solve it (41 mod 5 = 1). */
    private val rotated = IntArray(25) { if (it < 5) (it + 1) % 5 else it }

    private fun savedPath(length: Int) = List(length) { SavedMove(isRow = true, index = 0, amount = 1) }

    /** A save `foo` of the rotated board with Mordie's unproven [n]-move answer, R = 3. */
    private fun WebHarness.seedUnproven(n: Int = 41) =
        seed("foo", rotated, moves = 2, mordie = SavedMordie(rotated, n, found = true, solution = savedPath(n), provenShortest = false, ruledOut = 3))

    private fun WebHarness.loadFoo() {
        game.onLoad()
        game.onLoadChosen("foo")
    }

    @Test
    fun `WU2 a save with an unproven answer shows it at once after loading`() = webTest {
        val page = WebHarness(backgroundScope).seedUnproven().startOn(rotated)

        page.loadFoo()

        assertEquals("Found one in 41 moves, but maybe there's a shorter one! Can you beat it?", page.state.mordie)
        assertEquals(GiveUpButton.GIVE_UP, page.state.giveUp)

        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)

        assertEquals(41, page.state.solution!!.moves.size)
        val givenUp = page.props("game_given_up").single()
        assertEquals(41, givenUp["solution_length"])
        assertEquals(false, givenUp["solution_proven_shortest"])
    }

    @Test
    fun `WU2 after that load only the exact search restarts and a shorter answer is announced`() = webTest {
        val page = WebHarness(backgroundScope).seedUnproven().startOn(rotated)
        page.loadFoo()

        val search = page.mordieSearches.latest
        assertEquals(3, search.ruledOut)
        assertEquals(savedPath(41).size, search.best!!.size)

        search.progress = SearchProgress(31, found = true, elapsedMs = 4, solution = path(31), ruledOut = 5, provenShortest = false)
        repeat(5) { page.game.onArrow(Arrow.UP, 2) }

        assertEquals("Wait, I found a shorter one: 31 moves now! Can you beat it?", page.state.mordie)
        assertEquals(41, page.props("mordie_shorter_found").single()["previous_length"])
    }

    @Test
    fun `a save written before this ticket still resumes with the stopped-at line`() = webTest {
        val page = WebHarness(backgroundScope)
            .seed("foo", rotated, moves = 2, mordie = SavedMordie(rotated, 7, found = false))
            .startOn(rotated)

        page.loadFoo()

        assertEquals("Okay, so we stopped when I was at 7 moves! What was I doing?..", page.state.mordie)
        assertEquals(7, page.mordieSearches.latest.ruledOut)
        assertNull(page.mordieSearches.latest.best)
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
