package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import web.Arrow

/** GH-288: the tutorial's analytics events (docs/ANALYTICS_EVENTS.md, GH-288 section). */
class WebTutorialAnalyticsScenarioTest {

    private fun WebHarness.tutorialEvents() = analytics.events.filter { it.name.startsWith("tutorial_") }

    @Test
    fun `tutorial_started is sent on a first launch`() = webTest {
        val page = tutorialPage(backgroundScope)

        assertEquals(listOf(mapOf("source" to "first_launch")), page.ownProps("tutorial_started"))
    }

    @Test
    fun `tutorial_started is sent again after a reload halfway`() = webTest {
        val first = tutorialPage(backgroundScope)
        first.stepByArrow(1)

        val page = tutorialPage(backgroundScope, first.store)

        assertEquals(1, page.props("tutorial_started").size)
        assertEquals(1, first.props("tutorial_started").size)
    }

    @Test
    fun `no tutorial events for a player with saves`() = webTest {
        val page = WebHarness(backgroundScope, firstLaunch = true).seed("foo", solved(4)).start()

        assertTrue(page.tutorialEvents().isEmpty())
    }

    @Test
    fun `tutorial_step_completed is sent for each of steps 1 to 4`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.playSteps()

        assertEquals(listOf(1, 2, 3, 4), page.ownProps("tutorial_step_completed").map { it["step"] })
    }

    @Test
    fun `tutorial_step_completed carries the step's blocked shifts`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.game.onArrow(Arrow.LEFT, 1)
        page.game.onArrow(Arrow.RIGHT, 0)

        page.stepByArrow(1)

        assertEquals(mapOf("step" to 1, "blocked_shifts" to 2), page.ownProps("tutorial_step_completed").single())
    }

    @Test
    fun `tutorial_ended is sent as completed at step 6`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps()
        waitPause()

        assertEquals(mapOf("source" to "first_launch", "result" to "completed", "last_step" to 6, "blocked_shifts" to 0), page.ownProps("tutorial_ended").single())
    }

    @Test
    fun `tutorial_ended is sent as skipped, with the step`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.stepByArrow(1)

        page.game.tutorial.onSkip()

        assertEquals(mapOf("source" to "first_launch", "result" to "skipped", "last_step" to 2, "blocked_shifts" to 0), page.ownProps("tutorial_ended").single())
    }

    @Test
    fun `Controls opened from the panel says so`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.game.tutorial.onControls()

        assertEquals("tutorial", page.props("screen_controls_dialog").last()["opened_from"])
    }

    @Test
    fun `Labels opened from the panel says so`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.game.tutorial.onLabels()

        assertEquals("tutorial", page.props("screen_labels_dialog").last()["opened_from"])
    }

    @Test
    fun `board_solved is flagged as the tutorial's`() = webTest {
        val page = tutorialPage(backgroundScope)

        page.playSteps()

        assertEquals(true, page.props("board_solved").single()["tutorial"])
    }

    @Test
    fun `mordie_solution_found is flagged as the tutorial's`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.mordieSearches.latest.progress = SearchProgress(4, found = true, elapsedMs = 0, solution = List(4) { SolutionMove(isRow = true, index = 0, amount = 1) })

        page.playSteps()

        assertEquals(true, page.props("mordie_solution_found").single()["tutorial"])
    }

    @Test
    fun `a new game after the tutorial is not flagged`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps()
        waitPause()

        page.game.onNew()
        page.game.onSizeChosen(4)
        page.board.restoreState(ONE_SHIFT_FROM_SOLVED, 0)
        page.game.onArrow(Arrow.LEFT, 0)

        assertEquals(listOf(true, false), page.props("board_solved").map { it["tutorial"] })
    }
}
