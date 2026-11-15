package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import viewmodel.UnsolvableSaveChoice
import web.Focus
import web.WebDialog

/** GH-277 scenarios: in keyboard mode, a dialog that ends in a new or loaded game puts focus on the
 *  board, so the board keys work at once. Cancelling still gives focus back to the opener. */
class WebKeyboardGameFocusScenarioTest {

    private val mixedUp = intArrayOf(5, 1, 2, 3, 4, 9, 6, 7, 8, 13, 10, 11, 12, 0, 14, 15)

    private fun TestScope.keyboardPage() = WebHarness(backgroundScope).remembering().startOn(mixedUp)

    private fun WebHarness.loadUnsolvable() {
        seed("odd", UNSOLVABLE_3X3)
        game.onLoad()
        game.onLoadChosen("odd")
    }

    @Test
    fun `New then a size gives the board focus`() = webTest {
        val page = keyboardPage()

        page.game.onNew()
        page.game.onSizeChosen(5)

        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `a board key works right after New and a size`() = webTest {
        val page = keyboardPage()
        page.game.onNew()
        page.game.onSizeChosen(5)

        assertTrue(page.press("s", from = page.state.focus))
    }

    @Test
    fun `New then Cancel gives focus back to New`() = webTest {
        val page = keyboardPage()

        page.game.onNew()
        page.game.onSizeChosen(null)

        assertEquals(Focus.NewButton, page.state.focus)
    }

    @Test
    fun `Load then a save gives the board focus`() = webTest {
        val page = keyboardPage().seed("foo", solved(4))

        page.game.onLoad()
        page.game.onLoadChosen("foo")

        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `Load then Cancel gives focus back to Load`() = webTest {
        val page = keyboardPage().seed("foo", solved(4))

        page.game.onLoad()
        page.game.onLoadChosen(null)

        assertEquals(Focus.LoadButton, page.state.focus)
    }

    @Test
    fun `Load of a save that is gone gives focus back to Load`() = webTest {
        val page = keyboardPage()

        page.game.onLoad()
        page.game.onLoadChosen("gone")

        assertEquals(Focus.LoadButton, page.state.focus)
    }

    @Test
    fun `an unsolvable save repaired and loaded gives the board focus`() = webTest {
        val page = keyboardPage()

        page.loadUnsolvable()
        page.game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR)

        assertNull(page.state.dialog)
        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `an unsolvable save followed by New game and a size gives the board focus`() = webTest {
        val page = keyboardPage()

        page.loadUnsolvable()
        page.game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME)
        assertTrue(page.state.dialog is WebDialog.Size, "${page.state.dialog}")
        page.game.onSizeChosen(5)

        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `an unsolvable save followed by New game and Cancel on the size gives the board focus`() = webTest {
        val page = keyboardPage()

        page.loadUnsolvable()
        page.game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME)
        page.game.onSizeChosen(null)

        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `the 1 key on a toolbar button then a size gives the board focus`() = webTest {
        val page = keyboardPage()

        assertTrue(page.press("1", from = Focus.SaveButton))
        page.game.onSizeChosen(5)

        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `mouse mode keeps focus on New after a new game`() = webTest {
        val page = WebHarness(backgroundScope).startOn(mixedUp)

        page.game.onNew()
        page.game.onSizeChosen(5)

        assertEquals(Focus.NewButton, page.state.focus)
    }
}
