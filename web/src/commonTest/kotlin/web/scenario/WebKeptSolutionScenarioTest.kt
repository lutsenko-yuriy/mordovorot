package web.scenario

import kotlin.test.Test

/** GH-224 scenarios: saves keep Mordie's best solution so far and whether it is proven shortest,
 *  in the browser, through [WebHarness]. Each name notes the work unit that turns it green. */
class WebKeptSolutionScenarioTest {

    @Test
    fun `WU1 a solved save won below an unproven answer reloads with the beat-Mordie ending`() = webTest {
        // TODO: 1. Seed a solved 5x5 save with Mordie data: found, N = 40, not proven shortest, the player's moves below 40.
        // TODO: 2. Load it from the Load dialog.
        // TODO: 3. The title is the beat_mordie ending, not `Are you a machine?`.
        // TODO: 4. Shift off the board and solve it again.
        // TODO: 5. board_solved has mordie_result=beat_mordie and mordie_proven_shortest=false.
    }

    @Test
    fun `WU1 a solved save with a proven answer still ends with Are you a machine`() = webTest {
        // TODO: 1. Seed a solved 5x5 save with a proven N and the player's moves below it.
        // TODO: 2. Load it.
        // TODO: 3. The title is `Are you a machine?`.
        // TODO: 4. After solving it again, board_solved has mordie_result=machine and mordie_proven_shortest=true.
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
        // TODO: 1. Start a 5x5 game with an unproven answer, give up, and save on the solution screen.
        // TODO: 2. Load that save.
        // TODO: 3. The solution screen opens at the start of the replay.
        // TODO: 4. Its Mordie line says how many moves Mordie made.
        // TODO: 5. No new search is started.
        // TODO: 6. Loading it and then solving it keeps mordie_proven_shortest=false.
    }
}
