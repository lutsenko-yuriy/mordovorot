package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import viewmodel.UnsolvableSaveChoice
import web.Arrow
import web.Host
import web.layoutOf
import web.storage.InMemoryKeyValueStore

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
    fun `app_launched also carries app_version and host`() = webTest {
        fun launched(width: Int, height: Int) = WebHarness(
            backgroundScope, layout = layoutOf(width, height), appVersion = "9.9.9", host = Host.LOCALHOST,
        ).start().props("app_launched").single()

        fun expected(layout: String) = mapOf<String, Any?>(
            "mode" to "rich_ui", "layout" to layout, "platform" to "web", "app_version" to "9.9.9", "host" to "localhost",
            "tile_labels" to "numbers",
        )
        assertEquals(expected("wide"), launched(1200, 800))
        assertEquals(expected("tall"), launched(800, 800))
    }

    @Test
    fun `app_launched carries the remembered tile labels, GH-139`() = webTest {
        val store = InMemoryKeyValueStore(mutableMapOf("mordovorot.pref.tile_labels" to "letter_pairs"))

        val launched = WebHarness(backgroundScope, store = store).start().props("app_launched").single()

        assertEquals("letter_pairs", launched["tile_labels"])
    }

    @Test
    fun `app_launched reports the host bucket, never the hostname`() = webTest {
        val pages = Host.entries.associateWith { WebHarness(backgroundScope, host = it).start() }

        assertEquals(
            mapOf(Host.PUBLIC_SITE to "public_site", Host.LOCALHOST to "localhost", Host.OTHER to "other"),
            pages.mapValues { (_, page) -> page.props("app_launched").single()["host"] },
        )
        val values = pages.values.flatMap { page -> page.analytics.events.flatMap { it.properties.values } }
        assertTrue(values.none { "github.io" in it.toString() }, "$values")
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

    @Test
    fun `every web dialog_cancelled carries via`() = webTest {
        // TODO: 1. Cancel New, Save, Load and Labels with their buttons, Give Up with No.
        // TODO: 2. Every dialog_cancelled has via, and it is button.
    }
}
