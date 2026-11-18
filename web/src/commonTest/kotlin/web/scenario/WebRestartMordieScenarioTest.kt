package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import storage.SavedMordie
import texts.texts
import viewmodel.GiveUpButton
import web.Arrow

/** GH-349: what Mordie does after a restart. */
class WebRestartMordieScenarioTest {
    private val mordieTexts = texts().mordie

    @Test
    fun mordiesLineGoesBackToTheOpeningLine() = webTest {
        val page = onTwoMoves()
        page.mordieFinds()
        page.fiveShifts()
        assertEquals(mordieTexts.found(2), page.state.mordie)

        page.restartDiscarding()

        assertEquals(mordieTexts.opening, page.state.mordie)
    }

    @Test
    fun aSharedGameShowsTheFriendLineAgain() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, ONE_SHIFT_FROM_SOLVED, progress = "n12")
        repeat(5) { page.game.onArrow(Arrow.UP, 3) }

        page.restartDiscarding()

        assertEquals(mordieTexts.friendSolving(12), page.state.mordie)
    }

    @Test
    fun aLoadedGameShowsItsResumedLineAgain() = webTest {
        val page = WebHarness(backgroundScope).startOn().seed("foo", RESTART_MOVED, moves = 7, mordie = SavedMordie(ONE_SHIFT_FROM_SOLVED, 7, found = false))
        page.game.onLoad()
        page.game.onLoadChosen("foo")
        repeat(5) { page.game.onArrow(Arrow.UP, 3) }

        page.restartDiscarding()

        assertEquals(mordieTexts.resumed(7), page.state.mordie)
    }

    @Test
    fun aFoundSolutionStaysAndGiveUpStaysLive() = webTest {
        val page = onTwoMoves()
        page.mordieFinds()
        page.fiveShifts()
        assertEquals(GiveUpButton.GIVE_UP, page.state.giveUp)

        page.restartDiscarding()

        assertEquals(GiveUpButton.GIVE_UP, page.state.giveUp)
    }

    @Test
    fun hisFoundLineReturnsAtTheNextCheck() = webTest {
        val page = onTwoMoves()
        page.mordieFinds()
        page.fiveShifts()
        page.restartDiscarding()
        assertEquals(mordieTexts.opening, page.state.mordie)
        page.analytics.events.clear()

        page.fiveShifts()

        assertEquals(mordieTexts.found(2), page.state.mordie)
        assertTrue(page.props("mordie_solution_found").isEmpty())
    }

    @Test
    fun fromAWonBoardHisRaceGoesOn() = webTest {
        val page = onTwoMoves()
        page.mordieFinds()
        page.solveTwoMoves()
        assertEquals(GiveUpButton.MORDIES_SOLUTION, page.state.giveUp)

        page.game.restart.onRestart("button")

        assertFalse(page.state.solved)
        assertEquals(GiveUpButton.GIVE_UP, page.state.giveUp)
    }

    @Test
    fun aSecondSolveShowsAFreshEnding() = webTest {
        val page = onTwoMoves()
        page.solveTwoMoves()
        val first = page.state.mordie
        page.game.restart.onRestart("button")
        assertNotEquals(first, page.state.mordie)

        page.solveTwoMoves()

        assertTrue(page.state.solved)
        assertEquals(first, page.state.mordie)
        assertEquals(2, page.props("board_solved").size)
    }

    @Test
    fun giveUpAfterARestartLeavesOutTheSinceFoundFields() = webTest {
        val page = onTwoMoves()
        page.mordieFinds()
        page.fiveShifts()
        page.restartDiscarding()

        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)

        val gaveUp = page.props("game_given_up").single()
        assertFalse("moves_since_solution_found" in gaveUp)
        assertFalse("shifts_since_solution_found" in gaveUp)
    }
}
