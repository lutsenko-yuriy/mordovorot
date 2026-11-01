package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import mordie.SolutionSource
import viewmodel.GiveUpButton
import web.Arrow

/** GH-222 scenarios: Mordie keeps the best solution so far, in the browser, through [WebHarness].
 *  Same setup as [WebGiveUpScenarioTest]. */
class WebBestSolutionScenarioTest {

    private fun path(length: Int, amount: Int = 1) = List(length) { SolutionMove(isRow = true, index = 0, amount = amount) }

    private fun fast(moves: Int) = SearchProgress(
        moves, found = true, elapsedMs = 4, solution = path(moves),
        ruledOut = 3, provenShortest = false, source = SolutionSource.FAST_SOLVER,
    )

    private fun exact(moves: Int) = SearchProgress(moves, found = true, elapsedMs = 9, solution = path(moves, amount = 2))

    private fun WebHarness.shifts(count: Int) = repeat(count) { game.onArrow(Arrow.UP, 2) }

    @Test
    fun `give up on a 5x5 right after the fast answer`() = webTest {
        val page = WebHarness(backgroundScope).startOn(IntArray(25) { 24 - it })
        page.mordieSearches.latest.progress = fast(40)
        page.shifts(4)
        assertEquals(GiveUpButton.DIMMED, page.state.giveUp)

        page.shifts(1)
        assertEquals(GiveUpButton.GIVE_UP, page.state.giveUp)
        assertEquals("Found it! I had to make 40 moves to find the solution! Can you beat it?", page.state.mordie)

        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)

        assertEquals(40, page.state.solution!!.moves.size)
        assertTrue(page.mordieSearches.latest.cancelled)
        assertEquals(false, page.props("game_given_up").single()["solution_proven_shortest"])
    }

    @Test
    fun `the search stays alive after the first solution until the answer is proven`() = webTest {
        val page = WebHarness(backgroundScope).startOn(IntArray(16) { 15 - it })
        page.mordieSearches.latest.progress = fast(12)
        page.shifts(5)
        assertFalse(page.mordieSearches.latest.cancelled)
        assertTrue(page.state.mordie!!.contains("12 moves"))

        page.mordieSearches.latest.progress = exact(6)
        page.shifts(5)
        assertTrue(page.state.mordie!!.contains("6 moves"))

        // Proven: later progress is no longer read.
        page.mordieSearches.latest.progress = exact(2)
        page.shifts(5)
        assertTrue(page.state.mordie!!.contains("6 moves"))
        assertNull(page.props("board_solved").firstOrNull())
    }
}
