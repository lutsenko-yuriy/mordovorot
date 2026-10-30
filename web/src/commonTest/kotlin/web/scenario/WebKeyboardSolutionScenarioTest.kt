package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import mordie.SearchProgress
import mordie.SolutionMove
import viewmodel.GiveUpButton
import web.Arrow
import web.Focus
import web.fallbackFocus

/** GH-171: the solution screen from the keyboard, and where focus goes on the way there. The screen's
 *  are stubs until WU4 makes them green. */
class WebKeyboardSolutionScenarioTest {

    @Test
    fun `the solution keys step, restart and scroll in both layouts`() {
        // TODO: Give Up, Yes
        // TODO: right arrow, D, L, Space and Enter go next
        // TODO: left arrow, A, H go prev
        // TODO: R restarts
        // TODO: up and down arrows, W, S, K, J ask to scroll
        // TODO: repeat left-handed
    }

    @Test
    fun `mouse mode has no solution keys`() {
        // TODO: open the solution in mouse mode
        // TODO: L and R do nothing
    }

    @Test
    fun `Give Up's Yes and Mordie's solution put focus on the board in keyboard mode`() {
        // TODO: Give Up, Yes - focus on the board
        // TODO: win, then `Mordie's solution` - focus on the board
    }

    @Test
    fun `a solving key shift passes focus to Mordie's solution, else New`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()
        page.press("h", from = Focus.Board)
        assertEquals(GiveUpButton.HIDDEN, page.state.giveUp)
        assertEquals(Focus.NewButton, fallbackFocus(Focus.Board.key, page.state))

        val revealed = WebHarness(backgroundScope).remembering().startOn()
        revealed.mordieSearches.latest.progress = SearchProgress(1, found = true, elapsedMs = 0, solution = listOf(SolutionMove(isRow = true, index = 0, amount = 1)))
        repeat(5) { revealed.game.onArrow(Arrow.UP, 3) }
        repeat(5) { revealed.game.onArrow(Arrow.DOWN, 3) }
        revealed.press("h", from = Focus.Board)
        assertEquals(GiveUpButton.MORDIES_SOLUTION, revealed.state.giveUp)
        assertEquals(Focus.GiveUpRow, fallbackFocus(Focus.Board.key, revealed.state))
    }
}
