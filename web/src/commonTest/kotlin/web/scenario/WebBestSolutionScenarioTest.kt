package web.scenario

import kotlin.test.Test

/** GH-222 scenarios: Mordie keeps the best solution so far, in the browser, through [WebHarness].
 *  Same setup as [WebGiveUpScenarioTest]. Stubs: implement replaces each TODO with driver code. */
class WebBestSolutionScenarioTest {

    @Test
    fun `give up on a 5x5 right after the fast answer`() {
        // TODO: 1. Start a 5x5 web game.
        // TODO: 2. The worker search reports a found, unproven solution.
        // TODO: 3. Make 5 shifts: Found it! shows and Give Up is enabled.
        // TODO: 4. Give Up and confirm: the solution screen opens with the fast solution's steps.
        // TODO: 5. The search is stopped (mordieSearches.latest cancelled).
    }

    @Test
    fun `the search stays alive after the first solution until the answer is proven`() {
        // TODO: 1. Start a 4x4 web game; the search reports a found, unproven solution.
        // TODO: 2. Make 5 shifts: the search is not cancelled.
        // TODO: 3. The search reports the proven, shorter solution: the next check shows the new N.
        // TODO: 4. The search is cancelled once it is proven.
    }
}
