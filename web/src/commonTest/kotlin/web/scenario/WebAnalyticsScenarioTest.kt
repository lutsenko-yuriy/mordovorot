package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import viewmodel.UnsolvableSaveChoice
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
    fun `no Exit events ever fire on web`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", ONE_SHIFT_FROM_SOLVED).seed("odd", UNSOLVABLE_3X3).start()
        page.game.onLoadChosen("odd")
        page.game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME)
        page.game.onSizeChosen(null)
        page.game.onArrow(Arrow.RIGHT, 1)
        page.game.onSave()
        page.game.onSaveAnswered(null)
        page.game.onSave()
        page.game.onSaveAnswered("bar")
        page.game.onLoad()
        page.game.onLoadChosen(null)
        page.game.onLoad()
        page.game.onLoadChosen("odd")
        page.game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR)
        page.game.onLoad()
        page.game.onLoadChosen("foo")
        page.game.onArrow(Arrow.LEFT, 0)
        page.game.onNew()
        page.game.onSizeChosen(null)

        val events = page.analytics.events
        val exitEvents = setOf("exit_command_used", "input_mode_switched", "app_launch_aborted", "screen_exit_dialog")
        assertTrue(events.none { it.name in exitEvents }, "$events")
        assertTrue(page.props("startup_restore_decision").none { it["decision"] == "quit" }, "$events")
        assertTrue(page.props("unsolvable_save_resolved").none { it["choice"] == "exit" }, "$events")
        assertTrue(page.props("dialog_cancelled").none { it["dialog"] == "exit" }, "$events")
        assertTrue(events.map { it.name }.containsAll(listOf("screen_unsolvable_dialog", "save_command_used", "load_command_used", "board_solved")))
    }

}
