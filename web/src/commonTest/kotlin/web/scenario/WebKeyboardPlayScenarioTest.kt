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
import web.ToolbarButton
import web.WebDialog
import web.a11y.Focus

/** GH-171: the digit toolbar in keyboard mode. The board's keys are in [WebKeyboardBoardScenarioTest]. */
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
    fun `keys with Ctrl, Alt or Meta, and Escape with no dialog, do nothing`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()
        val before = page.frames.size

        assertFalse(page.press("s", modified = true))
        assertFalse(page.press("1", modified = true))
        assertFalse(page.press("Escape"))

        assertEquals(before, page.frames.size)
    }
}
