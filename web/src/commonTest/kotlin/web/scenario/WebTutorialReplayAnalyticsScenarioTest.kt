package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import web.Arrow
import web.ReplaySaveChoice

/** GH-289: the replay's analytics, as catalogued in `docs/ANALYTICS_EVENTS.md`. */
class WebTutorialReplayAnalyticsScenarioTest {

    /** A game with one move made, unsaved. */
    private fun TestScope.mid() = WebHarness(backgroundScope).startOn().also { it.game.onArrow(Arrow.RIGHT, 1) }

    private fun TestScope.asked() = mid().also { it.game.tutorial.onReplay("button") }

    @Test
    fun `the save question sends screen_replay_save_dialog from a click`() = webTest {
        val page = mid()

        page.game.tutorial.onReplay("button")

        assertEquals("button", page.ownProps("screen_replay_save_dialog").single()["opened_from"])
    }

    @Test
    fun `the save question sends screen_replay_save_dialog from key 9`() = webTest {
        val page = mid()

        page.press("9")

        assertEquals("shortcut", page.ownProps("screen_replay_save_dialog").single()["opened_from"])
    }

    @Test
    fun `the question's event carries the move count`() = webTest {
        val page = mid()
        page.game.onArrow(Arrow.RIGHT, 2)

        page.game.tutorial.onReplay("button")

        assertEquals(mapOf("opened_from" to "button", "move_count" to 2), page.ownProps("screen_replay_save_dialog").single())
    }

    @Test
    fun `Save sends tutorial_replay_decision`() = webTest {
        val page = asked()

        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.SAVE)

        assertEquals(mapOf("decision" to "save"), page.ownProps("tutorial_replay_decision").single())
    }

    @Test
    fun `Don't save sends tutorial_replay_decision`() = webTest {
        val page = asked()

        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.DONT_SAVE)

        assertEquals(mapOf("decision" to "dont_save"), page.ownProps("tutorial_replay_decision").single())
    }

    @Test
    fun `Cancel sends dialog_cancelled, not a decision`() = webTest {
        val page = asked()

        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.CANCEL)

        assertEquals(mapOf("dialog" to "replay_save", "via" to "button"), page.ownProps("dialog_cancelled").single())
        assertTrue(page.props("tutorial_replay_decision").isEmpty())
    }

    @Test
    fun `the Save dialog from the question has its own source`() = webTest {
        val page = asked()

        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.SAVE)

        assertEquals("tutorial_replay", page.ownProps("screen_save_dialog").single()["opened_from"])
    }

    @Test
    fun `a replay with nothing to save sends tutorial_started with source replay`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.tutorial.onReplay("button")

        assertEquals(mapOf("source" to "replay"), page.ownProps("tutorial_started").single())
        assertTrue(page.props("screen_replay_save_dialog").isEmpty())
    }

    @Test
    fun `a replay after Don't save sends tutorial_started with source replay`() = webTest {
        val page = asked()

        page.game.tutorial.onReplayAnswered(ReplaySaveChoice.DONT_SAVE)

        assertEquals(mapOf("source" to "replay"), page.ownProps("tutorial_started").single())
    }

    @Test
    fun `a replay's end sends tutorial_ended with source replay`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.tutorial.onReplay("button")
        page.playSteps()

        waitPause()

        assertEquals(mapOf("source" to "replay", "result" to "completed", "last_step" to 6, "blocked_shifts" to 0), page.ownProps("tutorial_ended").single())
    }

    @Test
    fun `a first launch's end sends tutorial_ended with source first_launch`() = webTest {
        val page = tutorialPage(backgroundScope)
        page.playSteps()

        waitPause()

        assertEquals("first_launch", page.ownProps("tutorial_ended").single()["source"])
    }
}
