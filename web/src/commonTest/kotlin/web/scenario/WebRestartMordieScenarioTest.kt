package web.scenario

import kotlin.test.Test

/** GH-349: what Mordie does after a restart. Stubs: WU2 fills them in. */
class WebRestartMordieScenarioTest {
    @Test
    fun mordiesLineGoesBackToTheOpeningLine() {
        // TODO: 1. Play past 5 shifts so his line moves on
        // TODO: 2. Restart
        // TODO: 3. His line is the opening line
    }

    @Test
    fun aSharedGameShowsTheFriendLineAgain() {
        // TODO: 1. Restart a shared game
        // TODO: 2. His line is the friend line
    }

    @Test
    fun aLoadedGameShowsItsResumedLineAgain() {
        // TODO: 1. Restart a loaded game
        // TODO: 2. His line is the "we stopped at N moves" line
    }

    @Test
    fun aFoundSolutionStaysAndGiveUpStaysLive() {
        // TODO: 1. Let him find a solution
        // TODO: 2. Restart
        // TODO: 3. `[ Give Up ]` is live
    }

    @Test
    fun hisFoundLineReturnsAtTheNextCheck() {
        // TODO: 1. Let him find a solution, then restart
        // TODO: 2. Make 5 shifts
        // TODO: 3. His found line returns
        // TODO: 4. No new `mordie_solution_found` event is sent
    }

    @Test
    fun fromAWonBoardHisRaceGoesOn() {
        // TODO: 1. Solve the board and restart
        // TODO: 2. The Give Up row shows a live `[ Give Up ]`
    }

    @Test
    fun aSecondSolveShowsAFreshEnding() {
        // TODO: 1. Solve, restart, solve again
        // TODO: 2. A new ending shows
    }

    @Test
    fun giveUpAfterARestartLeavesOutTheSinceFoundFields() {
        // TODO: 1. Restart after he found a solution
        // TODO: 2. Give up
        // TODO: 3. `game_given_up` has no `moves_since_solution_found`
    }
}
