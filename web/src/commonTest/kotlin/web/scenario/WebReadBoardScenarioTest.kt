package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import mordie.SolutionMove
import viewmodel.TileLabels
import web.Arrow

/** GH-167 scenarios: the board a screen reader reads during play - Read board's words and which
 *  tiles are home. The press itself and the grid's markup are checked by the harness
 *  (`keys_check.sh`, `grid=1`). */
class WebReadBoardScenarioTest {

    private val startWords = "Row 1: 4, 1, 2, 3. Row 2: 5, 6, 7, 8. Row 3: 9, 10, 11, 12. Row 4: 13, 14, 15, 16. 12 tiles in place."
    private val rowOneOut = List(4) { false } + List(12) { true }

    @Test
    fun `the board read-out says each row, then how many tiles are in place`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        assertEquals(startWords, page.state.boardWords)
        assertEquals(rowOneOut, page.state.home)
    }

    @Test
    fun `the board read-out follows a shift`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onArrow(Arrow.RIGHT, 1)

        val words = page.state.boardWords!!
        assertTrue("Row 2: 8, 5, 6, 7." in words, words)
        assertTrue(words.endsWith("8 tiles in place."), words)
        assertEquals(List(4) { false }, page.state.home.subList(4, 8))
    }

    @Test
    fun `mid-drag the board read-out stays on the settled board`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onDragStart(1, 0, 100.0, 100.0, 40.0)
        page.game.onDragMove(160.0, 100.0)
        assertEquals(startWords, page.state.boardWords)
        assertEquals(rowOneOut, page.state.home)

        page.game.onDragEnd()
        assertTrue("Row 2: 7, 8, 5, 6." in page.state.boardWords!!, page.state.boardWords)
    }

    @Test
    fun `the board read-out uses the tile labels shown, previewed ones too`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onLabels()
        page.game.onLabelsSelected(TileLabels.LETTERS)
        assertTrue(page.state.boardWords!!.startsWith("Row 1: delta, alpha, bravo, charlie."), page.state.boardWords)
        page.game.onLabelsAnswered(ok = false)
        assertTrue(page.state.boardWords!!.startsWith("Row 1: 4, 1, 2, 3."), page.state.boardWords)

        page.game.onLabels()
        page.game.onLabelsSelected(TileLabels.LETTERS)
        page.game.onLabelsAnswered(ok = true)
        assertTrue(page.state.boardWords!!.startsWith("Row 1: delta, alpha, bravo, charlie."), page.state.boardWords)
    }

    @Test
    fun `a solved board has no read-out`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onArrow(Arrow.LEFT, 0)

        assertTrue(page.state.solved)
        assertNull(page.state.boardWords)
        assertEquals(emptyList(), page.state.home)
    }

    @Test
    fun `the solution screen has no read-out`() = webTest {
        // As WebGiveUpScenarioTest: Mordie finds a two-move solution, then a move reveals Give Up.
        val original = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
        val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))
        val page = WebHarness(backgroundScope).startOn(original)
        page.mordieSearches.latest.progress = SearchProgress(path.size, found = true, elapsedMs = 0, solution = path)
        repeat(5) { page.game.onArrow(Arrow.UP, 3) }

        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)
        assertTrue(page.state.solution != null)
        assertNull(page.state.boardWords)

        page.game.solution.onReplayNext()
        assertNull(page.state.boardWords)
        assertEquals(emptyList(), page.state.home)
    }
}
