package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.KeyScheme
import viewmodel.KeyboardLayout
import web.Arrow
import web.Focus
import web.PlayMode
import web.ToolbarButton
import web.WebDialog
import web.storage.InMemoryKeyValueStore

/** GH-171: the web's Controls dialog (`[ Controls 6 ]` / 6, both modes) picks the input mode and the
 *  keyboard layout, and both are remembered. */
class WebControlsScenarioTest {

    private val WebHarness.controls get() = assertIs<WebDialog.Controls>(state.dialog)

    @Test
    fun `6 in mouse mode opens Controls, and Keyboard then OK switches mode keeping the game`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.UP, 3)
        val tiles = page.state.tiles
        assertEquals(PlayMode.MOUSE, page.state.mode)

        assertTrue(page.press("6"))
        page.game.controls.onMode(PlayMode.KEYBOARD)
        // Applied only on OK.
        assertEquals(PlayMode.MOUSE, page.state.mode)
        page.game.controls.onAnswered(ok = true)

        assertEquals(PlayMode.KEYBOARD, page.state.mode)
        assertEquals(tiles, page.state.tiles)
        assertEquals(1, page.state.moves)
        assertNull(page.state.dialog)
    }

    @Test
    fun `Controls opens focused on the marked mode`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.controls.open()
        assertEquals(Focus.ModeOption(PlayMode.MOUSE), page.state.focus)
        page.game.controls.onAnswered(ok = false)

        page.press("6")
        assertEquals(Focus.ModeOption(PlayMode.MOUSE), page.state.focus)
    }

    @Test
    fun `arrows move the mark and the key list follows the marked layout`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.press("6")
        assertEquals(KeyScheme.boardKeyLines(KeyboardLayout.RIGHT_HANDED), page.controls.keyList.take(3))

        page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)
        assertEquals(KeyScheme.boardKeyLines(KeyboardLayout.LEFT_HANDED), page.controls.keyList.take(3))
        assertEquals(Focus.LayoutOption(KeyboardLayout.LEFT_HANDED), page.state.focus)

        page.game.controls.onLayout(KeyboardLayout.RIGHT_HANDED)
        assertEquals(KeyScheme.boardKeyLines(KeyboardLayout.RIGHT_HANDED), page.controls.keyList.take(3))
    }

    @Test
    fun `Cancel and Escape in Controls change nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        for (close in listOf({ page.game.controls.onAnswered(ok = false) }, { page.game.onEscape() })) {
            page.press("6")
            page.game.controls.onMode(PlayMode.KEYBOARD)
            page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)

            close()

            assertNull(page.state.dialog)
            assertEquals(PlayMode.MOUSE, page.state.mode)
            assertNull(page.preferences.get("input_mode"))
            assertNull(page.preferences.get("keyboard_layout"))
        }
        // Reopened: the marks are the current ones again.
        page.press("6")
        assertEquals(PlayMode.MOUSE, page.controls.mode)
        assertEquals(KeyboardLayout.RIGHT_HANDED, page.controls.layout)
    }

    @Test
    fun `the layout can be picked with Mouse marked`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.press("6")
        page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)

        page.game.controls.onAnswered(ok = true)

        assertEquals(PlayMode.MOUSE, page.state.mode)
        assertEquals("left_handed", page.preferences.get("keyboard_layout"))
        assertNull(page.preferences.get("input_mode"))
    }

    @Test
    fun `OK focuses the board on entering keyboard mode, otherwise goes back`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.controls.open()
        page.game.controls.onMode(PlayMode.KEYBOARD)
        page.game.controls.onAnswered(ok = true)
        assertEquals(Focus.Board, page.state.focus)

        page.game.controls.open()
        page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)
        page.game.controls.onAnswered(ok = true)
        assertEquals(Focus.ControlsButton, page.state.focus)
    }

    @Test
    fun `mode and layout are remembered, and a first visit is mouse and right-handed`() = webTest {
        val store = InMemoryKeyValueStore()
        val first = WebHarness(backgroundScope, store = store).startOn()
        assertEquals(PlayMode.MOUSE, first.state.mode)
        first.press("6")
        assertEquals(KeyboardLayout.RIGHT_HANDED, first.controls.layout)

        first.game.controls.onMode(PlayMode.KEYBOARD)
        first.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)
        first.game.controls.onAnswered(ok = true)

        val reloaded = WebHarness(backgroundScope, store = store).startOn()
        assertEquals(PlayMode.KEYBOARD, reloaded.state.mode)
        reloaded.press("6")
        assertEquals(KeyboardLayout.LEFT_HANDED, reloaded.controls.layout)
    }

    @Test
    fun `in mouse mode only 6 works and only its button shows a digit`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        for (digit in "12345") {
            assertFalse(page.press(digit.toString()))
            assertNull(page.state.dialog)
        }

        assertEquals(listOf(6), ToolbarButton.entries.mapNotNull { page.state.digitOf(it) })
        assertTrue(page.state.controlsShown)
    }

    @Test
    fun `on a touch screen Controls is hidden and 6 does nothing`() = webTest {
        val page = WebHarness(backgroundScope, touch = true).startOn()

        assertFalse(page.state.controlsShown)
        assertFalse(page.press("6"))
        assertNull(page.state.dialog)
    }

    @Test
    fun `a remembered keyboard mode starts as mouse on a touch screen and stays stored`() = webTest {
        val page = WebHarness(backgroundScope, touch = true).remembering().startOn()

        assertEquals(PlayMode.MOUSE, page.state.mode)
        assertEquals("keyboard", page.preferences.get("input_mode"))
    }

    /** WU4 deletes this with the flag (D3). */
    @Test
    fun `with the flag off Controls is hidden, 6 is ignored and the mode is mouse`() = webTest {
        val page = WebHarness(backgroundScope, keyboardModeEnabled = false).remembering().startOn()

        assertEquals(PlayMode.MOUSE, page.state.mode)
        assertFalse(page.state.controlsShown)
        assertFalse(page.press("6"))
        assertNull(page.state.dialog)
        assertEquals("keyboard", page.preferences.get("input_mode"))
    }
}
