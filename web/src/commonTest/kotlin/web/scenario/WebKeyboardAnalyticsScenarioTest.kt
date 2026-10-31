package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.KeyboardLayout
import web.Arrow
import web.Focus
import web.PlayMode

/** GH-171: analytics in keyboard play (`docs/ANALYTICS_EVENTS.md`). */
class WebKeyboardAnalyticsScenarioTest {

    @Test
    fun `input_method follows the press, and the launch and Controls events are sent`() = webTest {
        val page = WebHarness(backgroundScope).remembering().start()
        assertEquals("keyboard", page.props("app_launched").single()["mode"])
        page.game.onSizeChosen(null)
        page.analytics.events.clear()

        page.press("6")
        assertEquals(
            mapOf("opened_from" to "shortcut", "screen" to "play", "platform" to "web", "input_method" to "keyboard"),
            page.props("screen_controls_dialog").last(),
        )
        page.pressedWith.key()
        page.game.onEscape()
        assertEquals(mapOf("dialog" to "controls", "via" to "escape"), page.ownProps("dialog_cancelled").single())
        assertEquals("keyboard", page.props("dialog_cancelled").single()["input_method"])

        page.pressedWith.pointer()
        page.game.controls.open()
        assertEquals("toolbar", page.props("screen_controls_dialog").last()["opened_from"])
        assertEquals("rich_ui", page.props("screen_controls_dialog").last()["input_method"])

        page.game.controls.onMode(PlayMode.MOUSE)
        page.game.controls.onAnswered(ok = true)
        assertEquals(
            mapOf("from_mode" to "keyboard", "to_mode" to "mouse", "trigger" to "controls_dialog", "result" to "success"),
            page.ownProps("input_mode_switched").single(),
        )
    }

    @Test
    fun `OK with nothing changed sends no switch, and a layout change sends nothing at all`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.press("6")
        page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)
        page.analytics.events.clear()

        page.game.controls.onAnswered(ok = true)

        assertEquals(emptyList(), page.analytics.events)
    }

    @Test
    fun `4 opens the Labels dialog as a shortcut, and its button as the toolbar`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()

        page.press("4")
        assertEquals("shortcut", page.props("screen_labels_dialog").last()["opened_from"])
        page.game.onEscape()

        page.game.onLabels()
        assertEquals("toolbar", page.props("screen_labels_dialog").last()["opened_from"])
    }

    @Test
    fun `board_solved reports keys or mixed, with shift_keys for right-handed shifts only`() = webTest {
        val keys = WebHarness(backgroundScope).remembering().startOn()
        keys.press("h", from = Focus.Board)
        assertEquals(mapOf("shift_input" to "keys", "shift_keys" to "hjkl"), keys.solvedProps())

        val mixed = WebHarness(backgroundScope).remembering().startOn()
        mixed.game.onArrow(Arrow.RIGHT, 0)
        mixed.press("ArrowLeft", from = Focus.Board)
        mixed.press("h", from = Focus.Board)
        assertEquals(mapOf("shift_input" to "mixed", "shift_keys" to "mixed"), mixed.solvedProps())

        val left = WebHarness(backgroundScope).remembering(keys = KeyboardLayout.LEFT_HANDED).startOn()
        left.press("a", from = Focus.Board)
        assertEquals(mapOf("shift_input" to "keys"), left.solvedProps())
    }

    private fun WebHarness.solvedProps() = props("board_solved").single().filterKeys { it.startsWith("shift_") }

    @Test
    fun `selection moves, 0 and mouse-mode keys send nothing`() = webTest {
        val keyboard = WebHarness(backgroundScope).remembering().startOn()
        keyboard.press("w", from = Focus.Board)
        keyboard.press("0", from = Focus.Board)
        assertEquals(emptyList(), keyboard.analytics.events)

        val mouse = WebHarness(backgroundScope).startOn()
        mouse.press("1")
        assertEquals(emptyList(), mouse.analytics.events)
    }

    @Test
    fun `a non-Latin shift counts as hjkl`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()

        page.pressedWith.key()
        page.press("р", from = Focus.Board, code = "KeyH")

        val solved = page.props("board_solved").single()
        assertEquals("hjkl", solved["shift_keys"])
        assertEquals("keyboard", solved["input_method"])
    }
}
