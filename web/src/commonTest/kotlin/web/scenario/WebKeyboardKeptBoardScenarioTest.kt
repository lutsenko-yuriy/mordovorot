package web.scenario

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import web.Arrow
import web.Focus

/** GH-171: which frames keep keyboard mode's board in place. */
class WebKeyboardKeptBoardScenarioTest {

    private val mixedUp = intArrayOf(5, 1, 2, 3, 4, 9, 6, 7, 8, 13, 10, 11, 12, 0, 14, 15)

    private fun WebHarness.key(key: String) = press(key, from = Focus.Board)

    /** So VoiceOver hears the live region, and moves on to a new tile at once, rather than a refocused board. */
    @Test
    fun `selects, shifts and tile presses keep the board, a dialog or a win doesn't`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        fun kept(action: () -> Unit): Boolean {
            val before = page.state
            action()
            return page.state.keepsBoardFrom(before)
        }

        assertTrue(kept { page.key("d") })
        assertTrue(kept { page.key("h") })
        assertTrue(kept { page.game.onTilePressed(3, 3) })
        assertFalse(kept { page.key("2") })
        assertFalse(kept { page.game.onSaveAnswered(null) }, "the frame the dialog closes in")

        val won = WebHarness(backgroundScope).remembering().startOn()
        val before = won.state
        won.press("h", from = Focus.Board)
        assertFalse(won.state.keepsBoardFrom(before))
    }

    @Test
    fun `a size change doesn't keep the board, nor does a mouse-mode frame`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        assertFalse(page.state.copy(side = 5).keepsBoardFrom(page.state))

        val mouse = WebHarness(backgroundScope).startOn(mixedUp)
        val before = mouse.state
        mouse.game.onArrow(Arrow.LEFT, 0)
        assertFalse(mouse.state.keepsBoardFrom(before))
    }
}
