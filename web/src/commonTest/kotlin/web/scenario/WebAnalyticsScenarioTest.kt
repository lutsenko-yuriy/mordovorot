package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import web.Arrow
import web.layoutOf

/** GH-110 scenarios: the web's analytics, per `docs/ANALYTICS_EVENTS.md`. Same setup as
 *  [WebStartupScenarioTest]. */
class WebAnalyticsScenarioTest {

    @Test
    fun `every event carries platform web and input_method rich_ui`() = webTest {
        val page = WebHarness(backgroundScope)
        page.board.restoreState(ONE_SHIFT_FROM_SOLVED, 0)
        page.start()
        page.game.onSizeChosen(null)
        page.game.onArrow(Arrow.LEFT, 0)
        page.game.onNew()
        page.game.onSizeChosen(3)

        val events = page.analytics.events
        assertTrue(events.map { it.name }.containsAll(listOf("app_launched", "new_game_size_selected", "board_solved", "screen_congratulations")))
        assertTrue(events.all { it.properties["platform"] == "web" }, "$events")
        assertTrue(events.filter { it.name != "app_launched" }.all { it.properties["input_method"] == "rich_ui" }, "$events")
        assertEquals("arrows", page.props("board_solved").single()["shift_input"])
    }

    @Test
    fun `app_launched has mode rich_ui and the layout, and no terminal-only properties`() = webTest {
        fun launched(width: Int, height: Int) =
            WebHarness(backgroundScope, layout = layoutOf(width, height)).start().props("app_launched").single()

        assertEquals(mapOf<String, Any?>("mode" to "rich_ui", "layout" to "wide", "platform" to "web"), launched(1200, 800))
        assertEquals(mapOf<String, Any?>("mode" to "rich_ui", "layout" to "tall", "platform" to "web"), launched(800, 800))
    }

    @Test
    fun `no Exit events ever fire on web`() {
        // WU5
        // TODO: 1. Go through every dialog path.
        // TODO: 2. None of exit_command_used, input_mode_switched, app_launch_aborted, screen_exit_dialog,
        //          decision: quit, choice: exit, dialog: exit fired.
    }
}
