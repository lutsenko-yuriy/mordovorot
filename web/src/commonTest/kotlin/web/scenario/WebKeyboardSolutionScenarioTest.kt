package web.scenario

import kotlin.test.Test

/** GH-171: the solution screen from the keyboard, and where focus goes on the way there. Stubs until
 *  WU4 makes them green. */
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
    fun `a solving key shift passes focus to Mordie's solution, else New`() {
        // TODO: solve with `Mordie's solution` shown - focus there
        // TODO: solve without it - focus on `New`
    }
}
