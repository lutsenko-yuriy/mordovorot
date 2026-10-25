package web.scenario

import board_model.Solvability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import mordie.SearchProgress
import web.Arrow
import web.WebDialog

/** GH-110 scenarios: shifting with the arrows, New, Moves, Mordie and the solved state. Same setup
 *  as [WebStartupScenarioTest]. */
class WebPlayScenarioTest {

    @Test
    fun `an arrow click shifts the line by one, wrapping around`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onArrow(Arrow.RIGHT, 0)
        assertEquals(listOf(3, 4, 1, 2), page.state.tiles.take(4))

        page.game.onArrow(Arrow.UP, 2)
        assertEquals(listOf(7, 11, 15, 1), (2 until 16 step 4).map { page.state.tiles[it] })
    }

    @Test
    fun `Moves follows the move rule`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onArrow(Arrow.RIGHT, 0)
        page.game.onArrow(Arrow.RIGHT, 0)
        assertEquals(1, page.state.moves)
        page.game.onArrow(Arrow.LEFT, 0)
        assertEquals(1, page.state.moves)
        page.game.onArrow(Arrow.RIGHT, 1)
        assertEquals(2, page.state.moves)
        page.game.onArrow(Arrow.UP, 0)
        assertEquals(3, page.state.moves)
    }

    @Test
    fun `New opens the size picker, and Cancel leaves the game untouched`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.RIGHT, 1)
        page.game.onArrow(Arrow.DOWN, 0)
        val before = page.state

        page.game.onNew()
        assertEquals(WebDialog.Size(listOf(3, 4, 5, 6)), page.state.dialog)
        page.game.onSizeChosen(null)

        assertEquals(before, page.state)
        assertEquals(listOf("toolbar"), page.props("screen_size_dialog").map { it["opened_from"] })
        assertEquals(listOf("size"), page.props("dialog_cancelled").map { it["dialog"] })
    }

    @Test
    fun `New with a size starts a fresh solvable game`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onNew()
        page.game.onSizeChosen(3)

        assertEquals(3, page.state.side)
        assertEquals(0, page.state.moves)
        assertNull(page.state.dialog)
        assertTrue(Solvability.isSolvable(page.state.tiles.map { it - 1 }.toIntArray()))
        assertEquals(listOf(mapOf<String, Any?>("size" to 3, "trigger" to "toolbar")), page.ownProps("new_game_size_selected"))
    }

    @Test
    fun `solving fades the arrows and stops shifts`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onArrow(Arrow.LEFT, 0)
        assertFalse(page.state.arrowsEnabled)
        val solved = page.state

        page.game.onArrow(Arrow.RIGHT, 1)

        assertEquals(solved.tiles, page.state.tiles)
        assertEquals(solved.moves, page.state.moves)
        assertEquals(1, page.props("screen_congratulations").size)
    }

    @Test
    fun `solving a game Mordie sat out shows Congratulations`() = webTest {
        // Mordie sits out only a loaded save with no Mordie data and moves made.
        val page = WebHarness(backgroundScope).seed("s", ONE_SHIFT_FROM_SOLVED, moves = 3).start()
        page.game.onLoadChosen("s")

        page.game.onArrow(Arrow.LEFT, 0)

        assertEquals("Congratulations ✓", page.state.mordie)
    }

    @Test
    fun `Mordie's greeting, progress and Found it! appear in Mordie's spot`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.mordieSearches.latest.progress = SearchProgress(moves = 3, found = false, elapsedMs = 0)
        // Row 1 back and forth: row 0 stays off, so the board never solves.
        fun shifts(count: Int) = repeat(count) { page.game.onArrow(if (it % 2 == 0) Arrow.RIGHT else Arrow.LEFT, 1) }

        shifts(1)
        assertEquals("Alright! Let's sort this one out!", page.state.mordie)

        shifts(19)
        assertEquals("I have already made 3 moves and have not solved it yet!", page.state.mordie)

        page.mordieSearches.latest.progress = SearchProgress(moves = 11, found = true, elapsedMs = 0)
        shifts(5)
        assertEquals("Found it! I had to make 11 moves to find the solution! Can you beat it?", page.state.mordie)
    }

    @Test
    fun `solving before Mordie finds it - Mordie's ending replaces its text`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onArrow(Arrow.LEFT, 0)

        assertEquals("You beat me! I was still at 0 moves.", page.state.mordie)
    }
}
