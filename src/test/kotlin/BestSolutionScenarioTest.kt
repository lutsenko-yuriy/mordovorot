import kotlin.test.Test

/** GH-222 scenarios: Mordie keeps the best solution so far, from the fast solver and the exact
 *  search. Same setup as [MordieScenarioTest]: real BoardImpl + ViewModelImpl + FileSaveRepository
 *  (temp dir) in a mouse-mode GameSession over a FakeTerminal, with `Mordie(FakeMordieSearchFactory())`
 *  scripting the search's progress. Stubs: implement replaces each TODO with driver code. */
class BestSolutionScenarioTest {

    @Test
    fun `give up is available at the first check on a 5x5 once the fast solver has answered`() {
        // TODO: 1. Start a 5x5 game with a scripted search.
        // TODO: 2. Script a found, unproven, fast-solver solution of 40 moves.
        // TODO: 3. Make 4 shifts: the Give Up button is still dimmed.
        // TODO: 4. Make the 5th shift: the Found it! line shows and Give Up is enabled.
        // TODO: 5. mordie_solution_found has source=fast_solver, proven_shortest=false, solution_length=40.
    }

    @Test
    fun `Mordie's N drops when the exact search finds a shorter solution`() {
        // TODO: 1. Start a 3x3 game with a scripted search.
        // TODO: 2. Script a fast answer of 8 moves, then 5 shifts: Found it! shows N=8.
        // TODO: 3. Script an exact answer of 6 moves, proven.
        // TODO: 4. Make 5 more shifts: N shows 6.
        // TODO: 5. Give Up; game_given_up has solution_length=6 and solution_proven_shortest=true.
    }

    @Test
    fun `giving up before the exact search finishes shows the fast solution and says so in analytics`() {
        // TODO: 1. Start a 4x4 game with a scripted unproven 12-move fast solution.
        // TODO: 2. Reveal it with 5 shifts, press Give Up, then Yes.
        // TODO: 3. The solution screen opens with 12 steps.
        // TODO: 4. Script a shorter answer after the screen opens: the screen still shows the 12 steps (frozen).
        // TODO: 5. game_given_up.solution_proven_shortest is false.
    }

    @Test
    fun `solving the board reports whether Mordie's answer was proven shortest`() {
        // TODO: 1. Start a 4x4 game one shift from solved, with a scripted unproven solution.
        // TODO: 2. Reveal the solution, then solve with left(1).
        // TODO: 3. board_solved has mordie_moves set to N and mordie_proven_shortest=false.
    }

    @Test
    fun `an unproven answer is not saved as found`() {
        // TODO: 1. Start a game with a scripted unproven fast solution and reveal it.
        // TODO: 2. Save, then load the save in a fresh session.
        // TODO: 3. The loaded Mordie is searching at the saved ruledOut, not found.
        // TODO: 4. The greeting is "we stopped at N moves" and Give Up is dimmed.
        // TODO: 5. Script a new fast answer and make 5 shifts: Found it! returns.
    }

    @Test
    fun `a proven answer is saved as found`() {
        // TODO: 1. Start a game with a scripted proven solution and reveal it.
        // TODO: 2. Save and load in a fresh session.
        // TODO: 3. The loaded game is found, and Give Up is enabled at once.
    }
}
