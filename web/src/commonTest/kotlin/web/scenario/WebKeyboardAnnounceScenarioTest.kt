package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import web.Arrow
import web.a11y.Focus

/** GH-171: what a screen reader hears in keyboard mode. The reader says the selected cell itself, on
 *  a selection move and on the board taking focus (`docs/knowledge/notes/GH-171.md`). */
class WebKeyboardAnnounceScenarioTest {

    /** Row 0 is 4 1 2 3, one shift from solved; the second row is out of place too. */
    private val mixedUp = intArrayOf(3, 0, 1, 2, 5, 6, 7, 4) + IntArray(8) { it + 8 }

    private fun WebHarness.key(key: String) = press(key, from = Focus.Board)

    @Test
    fun `moving the selection says nothing of its own`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)

        page.key("d")
        page.key("s")
        assertEquals(emptyList(), page.said)
    }

    @Test
    fun `a shift by key adds the tile now under the selection`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        page.key("s")

        page.key("h")
        assertEquals("Row 2 left, 1 move. Now 7", page.said.last())
        page.key("ArrowLeft")
        assertEquals("Row 2 left. Now 8", page.said.last())
    }

    @Test
    fun `a drag adds it too, on the selection the press made`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)

        page.game.onTilePressed(1, 1)
        page.game.onDragStart(1, 1, 0.0, 0.0, 10.0)
        page.game.onDragMove(10.0, 0.0)
        page.game.onDragEnd()
        assertEquals("Row 2 right 1, 1 move. Now 6", page.said.last())
    }

    @Test
    fun `a solving shift says only the win, and mouse mode adds nothing`() = webTest {
        val solving = WebHarness(backgroundScope).remembering().startOn()
        solving.key("h")
        assertEquals("Solved in 1 move", solving.said.last())

        val mouse = WebHarness(backgroundScope).startOn(mixedUp)
        mouse.game.onArrow(Arrow.LEFT, 1)
        assertEquals("Row 2 left, 1 move", mouse.said.last())
    }

    @Test
    fun `the solution's board is named after the current step, in keyboard mode only`() = webTest {
        val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
        val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))
        fun WebHarness.givenUp(): WebHarness {
            mordieSearches.latest.progress = SearchProgress(path.size, found = true, elapsedMs = 0, solution = path)
            repeat(5) { game.onArrow(Arrow.UP, 3) }
            game.solution.onGiveUp()
            game.solution.onGiveUpAnswered(true)
            return this
        }

        val page = WebHarness(backgroundScope).remembering().startOn(original).givenUp()
        assertEquals("Board, start, step 0 of 2", page.state.boardName)
        page.key("l")
        assertEquals("Board, step 1 of 2: Row 1 right 1", page.state.boardName)

        assertNull(WebHarness(backgroundScope).remembering().startOn(mixedUp).state.boardName, "in play, the grid is plain `Board`")
        assertNull(WebHarness(backgroundScope).startOn(original).givenUp().state.boardName)
    }

    @Test
    fun `a frame's own words stay when focus comes back to the board`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)

        page.key("1")
        page.game.onSizeChosen(4)
        assertEquals("New 4x4 game", page.said.last())
        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `0 reads the board in keyboard mode, during play`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)

        assertTrue(page.press("0"))
        assertEquals(page.state.boardWords, page.said.last().takeIf { it.startsWith("Row 1: 4, 1, 2, 3.") })
        assertTrue(page.press("0", from = Focus.NewButton), "wherever focus is")
        assertEquals(2, page.said.size)
    }

    @Test
    fun `0 says nothing in a dialog, in mouse mode or off play`() = webTest {
        val dialog = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        dialog.key("2")
        assertFalse(dialog.press("0"))
        assertEquals(emptyList(), dialog.said)

        val mouse = WebHarness(backgroundScope).startOn(mixedUp)
        assertFalse(mouse.press("0"))
        assertEquals(emptyList(), mouse.said)

        val won = WebHarness(backgroundScope).remembering().startOn()
        won.key("h")
        val before = won.said.size
        won.press("0")
        assertEquals(before, won.said.size)
    }
}
