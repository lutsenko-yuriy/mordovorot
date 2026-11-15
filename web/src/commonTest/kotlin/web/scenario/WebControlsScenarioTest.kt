package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.KeyScheme
import viewmodel.KeyboardLayout
import web.Focus
import web.WebDialog
import web.storage.InMemoryKeyValueStore

/** GH-171, GH-287: the web's Controls dialog (`[ Controls 6 ]` / 6) picks the keyboard layout, which is remembered.
 *  There is no input mode to pick: a computer plays by mouse and keys alike. */
class WebControlsScenarioTest {

    private val WebHarness.controls get() = assertIs<WebDialog.Controls>(state.dialog)

    @Test
    fun `Controls opens on the layout, with no mode choice`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        assertTrue(page.press("6"))

        assertEquals(KeyboardLayout.RIGHT_HANDED, page.controls.layout)
        assertEquals(Focus.LayoutOption(KeyboardLayout.RIGHT_HANDED), page.state.focus)
        assertFalse(page.controls.toString().contains("mode"), page.controls.toString())
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
    fun `OK in Controls saves the layout and returns to the opener`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.controls.open()
        page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)

        page.game.controls.onAnswered(ok = true)

        assertNull(page.state.dialog)
        assertEquals("left_handed", page.preferences.get("keyboard_layout"))
        assertEquals(Focus.ControlsButton, page.state.focus)
    }

    @Test
    fun `OK after pressing 6 puts focus on the board`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.press("6", from = Focus.Board)
        page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)

        page.game.controls.onAnswered(ok = true)

        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `Cancel and Escape in Controls change nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        for (close in listOf({ page.game.controls.onAnswered(ok = false) }, { page.game.onEscape() })) {
            page.press("6")
            page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)

            close()

            assertNull(page.state.dialog)
            assertNull(page.preferences.get("keyboard_layout"))
        }
        // Reopened: the mark is the current one again.
        page.press("6")
        assertEquals(KeyboardLayout.RIGHT_HANDED, page.controls.layout)
    }

    @Test
    fun `a first visit is right-handed, and the layout is remembered`() = webTest {
        val store = InMemoryKeyValueStore()
        val first = WebHarness(backgroundScope, store = store).startOn()
        first.press("6")
        assertEquals(KeyboardLayout.RIGHT_HANDED, first.controls.layout)

        first.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)
        first.game.controls.onAnswered(ok = true)

        val reloaded = WebHarness(backgroundScope, store = store).startOn()
        reloaded.press("6")
        assertEquals(KeyboardLayout.LEFT_HANDED, reloaded.controls.layout)
    }

    @Test
    fun `a stored keyboard mode is ignored and the layout stays`() = webTest {
        val page = WebHarness(backgroundScope)
        page.preferences.set("input_mode", "keyboard")
        page.remembering(KeyboardLayout.LEFT_HANDED).startOn()

        page.press("6")

        assertEquals(KeyboardLayout.LEFT_HANDED, page.controls.layout)
        page.game.controls.onAnswered(ok = true)
        assertEquals("keyboard", page.preferences.get("input_mode"))
    }

    @Test
    fun `on a touch screen Controls is hidden and 6 does nothing`() = webTest {
        val page = WebHarness(backgroundScope, touch = true).startOn()

        assertFalse(page.state.computer)
        assertFalse(page.press("6"))
        assertNull(page.state.dialog)
    }

    @Test
    fun `app_launched has no mode`() = webTest {
        val page = WebHarness(backgroundScope).start()

        assertFalse(page.props("app_launched").single().containsKey("mode"))
    }

    @Test
    fun `no input_mode_switched event after OK in Controls`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.press("6")
        page.game.controls.onLayout(KeyboardLayout.LEFT_HANDED)

        page.game.controls.onAnswered(ok = true)

        assertTrue(page.props("input_mode_switched").isEmpty())
    }
}
