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
        assertEquals("Found one in 40 moves, but maybe there's a shorter one! Can you beat it?", page.state.mordie)

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

    // --- GH-223: lines and endings for an answer that may not be the shortest ---

    @Test
    fun `the browser shows the honest line, then the shorter one`() = webTest {
        val page = WebHarness(backgroundScope).startOn(IntArray(25) { 24 - it })
        page.mordieSearches.latest.progress = fast(40)
        page.shifts(5)
        assertEquals("Found one in 40 moves, but maybe there's a shorter one! Can you beat it?", page.state.mordie)

        page.mordieSearches.latest.progress = fast(31)
        page.shifts(5)

        assertEquals("Wait, I found a shorter one: 31 moves now! Can you beat it?", page.state.mordie)
        assertEquals(GiveUpButton.GIVE_UP, page.state.giveUp)
        assertEquals(31, page.props("mordie_shorter_found").single()["solution_length"])
    }

    @Test
    fun `the browser ends a win over an unproven answer`() = webTest {
        val page = WebHarness(backgroundScope).startOn(IntArray(25) { 24 - it })
        page.mordieSearches.latest.progress = fast(40)
        page.shifts(5)

        // Column 0 shifted down once from solved: one up-shift wins.
        page.board.restoreState(IntArray(25) { if (it % 5 == 0) ((it / 5 + 4) % 5) * 5 else it }, 0)
        page.game.onArrow(Arrow.UP, 0)

        assertEquals("You beat me! 40 moves vs your 1. Mine may not have been the shortest.", page.state.mordie)
        assertEquals("beat_mordie", page.props("board_solved").single()["mordie_result"])
    }
}
