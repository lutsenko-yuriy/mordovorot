package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import viewmodel.GiveUpButton
import web.Arrow
import web.Focus
import web.ToolbarButton
import web.WebDialog

/** GH-171: playing from the keyboard - the digit toolbar, the selected tile, its shifts in both
 *  layouts, focus and the key hints. The grid's markup is checked by the harness (`keys_check.sh`).
 *  The board's are stubs until WU3 makes them green. */
class WebKeyboardPlayScenarioTest {

    /** Mordie finds a solution, then five shifts reveal it, so Give Up is live. */
    private fun WebHarness.revealGiveUp() {
        mordieSearches.latest.progress = SearchProgress(1, found = true, elapsedMs = 0, solution = listOf(SolutionMove(isRow = true, index = 0, amount = 1)))
        repeat(5) { game.onArrow(Arrow.UP, 3) }
    }

    @Test
    fun `keyboard mode shows the digit toolbar and the Controls row`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()

        assertEquals(listOf(1, 2, 3, 4, 5, 6), ToolbarButton.entries.map { page.state.digitOf(it) })
        assertTrue(page.state.controlsShown)
    }

    @Test
    fun `digits open the toolbar's dialogs wherever focus is`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()
        page.revealGiveUp()
        val dialogs = listOf<Pair<String, (WebDialog?) -> Boolean>>(
            "1" to { it is WebDialog.Size },
            "2" to { it is WebDialog.Save },
            "3" to { it is WebDialog.Load },
            "4" to { it is WebDialog.Labels },
            "5" to { it == WebDialog.GiveUp },
            "6" to { it is WebDialog.Controls },
        )
        for ((digit, opened) in dialogs) {
            assertTrue(page.press(digit, from = Focus.LoadButton), digit)
            assertTrue(opened(page.state.dialog), digit)
            page.game.onEscape()
            assertNull(page.state.dialog)
        }
    }

    @Test
    fun `5 does nothing while Give Up is dimmed`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()
        assertEquals(GiveUpButton.DIMMED, page.state.giveUp)

        page.press("5")

        assertNull(page.state.dialog)
    }

    @Test
    fun `5 opens Mordie's solution after a win`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()
        page.revealGiveUp()
        // Undoes the five shifts of column 3, then the row that solves it.
        repeat(5) { page.game.onArrow(Arrow.DOWN, 3) }
        page.game.onArrow(Arrow.LEFT, 0)
        assertEquals(GiveUpButton.MORDIES_SOLUTION, page.state.giveUp)

        page.press("5")

        assertNotNull(page.state.solution)
    }

    @Test
    fun `a dialog opened by a digit returns focus where it was`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()

        page.press("2", from = Focus.Board)
        page.game.onSaveAnswered(null)
        assertEquals(Focus.Board, page.state.focus)

        page.press("2", from = Focus.LoadButton)
        page.game.onSaveAnswered(null)
        assertEquals(Focus.Restore(Focus.LoadButton.key), page.state.focus)

        page.press("2")
        page.game.onSaveAnswered(null)
        assertEquals(Focus.PageStart, page.state.focus)
    }

    @Test
    fun `digits typed in the Save dialog go into the name field`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()
        page.press("2")

        assertFalse(page.press("3"))

        assertIs<WebDialog.Save>(page.state.dialog)
    }

    @Test
    fun `W A S D move the selected tile with wraparound`() {
        // TODO: starts at row 1, column 1
        // TODO: D, S move it
        // TODO: A, W return it
        // TODO: W from row 1 wraps to the last row; A likewise
    }

    @Test
    fun `H L and the left and right arrows shift the selected row and the selection stays put`() {
        // TODO: press each key
        // TODO: the row's tiles shift; the selection and other rows don't
    }

    @Test
    fun `K J and the up and down arrows shift the selected column`() {
        // TODO: press each key
        // TODO: the column's tiles shift; the selection and other columns don't
    }

    @Test
    fun `the left-handed layout selects with HJKL and the arrows and shifts with WASD`() {
        // TODO: left-handed
        // TODO: H J K L and the arrows move the selection
        // TODO: A D shift the row; W S shift the column
    }

    @Test
    fun `upper-case letters work too`() {
        // TODO: D, S and H as capitals - same effect as lower case
    }

    @Test
    fun `board keys work only while the board has focus`() {
        // TODO: focus on `New` - W and H do nothing
        // TODO: digit 2 still opens Save
    }

    @Test
    fun `keys with Ctrl, Alt or Meta, and Escape with no dialog, do nothing`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()
        val before = page.frames.size

        assertFalse(page.press("s", modified = true))
        assertFalse(page.press("1", modified = true))
        assertFalse(page.press("Escape"))

        assertEquals(before, page.frames.size)
    }

    @Test
    fun `the board takes focus after the startup dialog in keyboard mode`() {
        // TODO: store holds keyboard, with a save
        // TODO: answer the startup dialog
        // TODO: focus on the board
    }

    @Test
    fun `pressing a tile selects it and gives the board focus`() {
        // TODO: press a tile at row 3, column 2
        // TODO: the selection is there and focus is on the board
    }

    @Test
    fun `a drag leaves the selection on the dragged line and the mouse still works`() {
        // TODO: drag row 2
        // TODO: the row shifted and the selection is on row 2
        // TODO: click `Save 2` - it opens
    }

    @Test
    fun `a smaller board clamps the selection and a new game or load keeps it`() {
        // TODO: select 4,4 on a 4x4
        // TODO: new 4x4 - kept
        // TODO: load a 4x4 - kept
        // TODO: new 3x3 - 3,3
    }

    @Test
    fun `switching mode keeps the selection`() {
        // TODO: select 2,3
        // TODO: to mouse and back
        // TODO: still 2,3
    }

    @Test
    fun `a solved board hides the selection and ignores the select and shift keys`() {
        // TODO: solve with a key
        // TODO: selection is null
        // TODO: W and H change nothing; digits still work
    }

    @Test
    fun `keyboard mode shows the key hints for its layout and hides the arrows`() {
        // TODO: right-handed hints
        // TODO: left-handed hints
        // TODO: arrows hidden
        // TODO: mouse mode - no hints
    }
}
