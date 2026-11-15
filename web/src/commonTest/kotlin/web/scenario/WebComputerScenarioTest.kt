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
import web.Arrow
import web.Focus
import web.ToolbarButton
import web.WebDialog

/** GH-287: on a computer everything shows and works, with no mode to choose; a touch screen keeps its look. */
class WebComputerScenarioTest {

    private val mixedUp = intArrayOf(5, 1, 2, 3, 4, 9, 6, 7, 8, 13, 10, 11, 12, 0, 14, 15)

    /** Mordie finds a solution, then five shifts reveal it, so Give Up is live. */
    private fun WebHarness.revealGiveUp() {
        mordieSearches.latest.progress = SearchProgress(1, found = true, elapsedMs = 0, solution = listOf(SolutionMove(isRow = true, index = 0, amount = 1)))
        repeat(5) { game.onArrow(Arrow.UP, 3) }
    }

    @Test
    fun `the arrows and key hints show on a computer`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        assertTrue(page.state.computer)
        assertNotNull(page.state.hints)
    }

    @Test
    fun `on a touch screen there are no key hints`() = webTest {
        val page = WebHarness(backgroundScope, touch = true).startOn()

        assertFalse(page.state.computer)
        assertNull(page.state.hints)
    }

    @Test
    fun `every toolbar button shows its digit on a computer`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), ToolbarButton.entries.map { page.state.digitOf(it) })
    }

    // One loop over the digits: splitting it into seven scenarios would repeat the same steps.
    @Test
    fun `each digit works wherever focus is`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.revealGiveUp()
        val dialogs = listOf<Pair<String, (WebDialog?) -> Boolean>>(
            "1" to { it is WebDialog.Size },
            "2" to { it is WebDialog.Save },
            "3" to { it is WebDialog.Load },
            "4" to { it is WebDialog.Labels },
            "5" to { it == WebDialog.GiveUp },
            "6" to { it is WebDialog.Controls },
            "7" to { it is WebDialog.Share },
        )
        for ((digit, opened) in dialogs) {
            assertTrue(page.press(digit, from = Focus.LoadButton), digit)
            assertTrue(opened(page.state.dialog), digit)
            page.game.onEscape()
            assertNull(page.state.dialog, digit)
        }
    }

    @Test
    fun `digits do nothing inside a dialog`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.press("2")
        assertIs<WebDialog.Save>(page.state.dialog)

        assertFalse(page.press("1"))

        assertIs<WebDialog.Save>(page.state.dialog)
    }

    @Test
    fun `on a touch screen no digit shows and none works`() = webTest {
        val page = WebHarness(backgroundScope, touch = true).startOn()

        assertEquals(emptyList(), ToolbarButton.entries.mapNotNull { page.state.digitOf(it) })
        assertFalse(page.press("1"))
        assertNull(page.state.dialog)
    }

    @Test
    fun `the board has focus at load`() = webTest {
        val page = WebHarness(backgroundScope).start()
        page.game.onSizeChosen(null)

        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `the board has focus after a new, loaded or saved game`() = webTest {
        val page = WebHarness(backgroundScope).seed("foo", solved(4)).startOn(mixedUp)

        page.game.onNew()
        page.game.onSizeChosen(5)
        assertEquals(Focus.Board, page.state.focus)

        page.game.onLoad()
        page.game.onLoadChosen("foo")
        assertEquals(Focus.Board, page.state.focus)

        page.game.onSave()
        page.game.onSaveAnswered("bar")
        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `closing a dialog puts focus back on the board`() = webTest {
        val page = WebHarness(backgroundScope).startOn(mixedUp)

        page.press("7", from = Focus.Board)
        assertIs<WebDialog.Share>(page.state.dialog)
        page.game.share.onClosed()

        assertNull(page.state.dialog)
        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `a press on a tile selects it and focuses the board`() = webTest {
        val page = WebHarness(backgroundScope).startOn(mixedUp)

        page.game.onTilePressed(2, 1)

        assertEquals(viewmodel.TileSelection(2, 1), page.state.selection)
        assertEquals(Focus.Board, page.state.focus)
    }
}
