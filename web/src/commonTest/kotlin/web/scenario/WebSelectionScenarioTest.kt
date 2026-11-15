package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.TileSelection
import web.Arrow
import web.Focus

/** GH-287 (WU2): the selected row and column show after a select or shift key, and hide after a tile or arrow press.
 *  The selection itself is kept. A hover never hides it (a cursor can't be moved headless). */
class WebSelectionScenarioTest {

    @Test
    fun `the selection is hidden at page load`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        assertFalse(page.state.selectionShown)
        assertEquals(TileSelection(0, 0), page.state.selection)
    }

    @Test
    fun `a select key shows the selection`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.press("d", from = Focus.Board)

        assertTrue(page.state.selectionShown)
        assertEquals(TileSelection(0, 1), page.state.selection)
    }

    @Test
    fun `a shift key shows the selection`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.press("h", from = Focus.Board)

        assertTrue(page.state.selectionShown)
    }

    @Test
    fun `a press on a tile hides the selection and keeps it`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.press("d", from = Focus.Board)
        assertTrue(page.state.selectionShown)

        page.game.onTilePressed(2, 3)

        assertFalse(page.state.selectionShown)
        assertEquals(TileSelection(2, 3), page.state.selection)
        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `an arrow press with the mouse hides the selection`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.press("d", from = Focus.Board)

        page.game.onArrow(Arrow.UP, 1)

        assertFalse(page.state.selectionShown)
        assertEquals(TileSelection(0, 1), page.state.selection)
    }

    // A key press on an arrow button reaches the page as the same click, so this is the click's scenario again.
    @Test
    fun `an arrow pressed by key hides the selection`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.press("d", from = Focus.Board)

        page.game.onArrow(Arrow.LEFT, 0)

        assertFalse(page.state.selectionShown)
    }

    @Test
    fun `a key shows it again where it was`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.press("d", from = Focus.Board)
        page.press("s", from = Focus.Board)
        page.game.onTilePressed(2, 2)

        page.press("a", from = Focus.Board)

        assertTrue(page.state.selectionShown)
        assertEquals(TileSelection(2, 1), page.state.selection)
    }

    @Test
    fun `the selection's visibility carries across a new game`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.press("d", from = Focus.Board)
        page.game.onNew()
        page.game.onSizeChosen(5)
        assertTrue(page.state.selectionShown)

        page.game.onTilePressed(1, 1)
        page.game.onNew()
        page.game.onSizeChosen(4)

        assertFalse(page.state.selectionShown)
    }

    @Test
    fun `the selection never shows on a touch screen`() = webTest {
        val page = WebHarness(backgroundScope, touch = true).startOn()

        page.press("d", from = Focus.Board)
        page.game.onTilePressed(1, 1)

        assertNull(page.state.selection)
    }
}
