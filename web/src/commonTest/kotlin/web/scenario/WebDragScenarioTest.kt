package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import web.Arrow
import web.drag.Axis
import web.drag.LineOffset

/** Tiles this far apart, in pixels. */
private const val PITCH = 40.0

/** Presses the tile at [row], [col] at (100, 100), moves through [moves] and releases. */
private fun WebHarness.drag(row: Int, col: Int, vararg moves: Pair<Double, Double>) {
    game.onDragStart(row, col, 100.0, 100.0, PITCH)
    moves.forEach { (x, y) -> game.onDragMove(x, y) }
    game.onDragEnd()
}

/** GH-110 scenarios: dragging a row or column. Same setup as [WebStartupScenarioTest], with drags
 *  given to `WebGame` as press/move/release in pixels. */
class WebDragScenarioTest {

    @Test
    fun `a drag of k tiles is k shifts and one move`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onDragStart(1, 0, 100.0, 100.0, PITCH)
        page.game.onDragMove(130.0, 102.0)
        page.game.onDragMove(180.0, 101.0)
        assertEquals(LineOffset(Axis.ROW, line = 1, tiles = 2.0), page.state.drag)
        assertEquals(listOf(5, 6, 7, 8), page.state.tiles.subList(4, 8))
        page.game.onDragEnd()

        assertEquals(listOf(7, 8, 5, 6), page.state.tiles.subList(4, 8))
        assertEquals(1, page.state.moves)
        assertEquals(2, page.mordie.shifts)
        assertNull(page.state.drag)
    }

    @Test
    fun `releasing at the starting position changes nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val before = page.state

        page.drag(0, 2, 101.0 to 120.0, 100.0 to 100.0)

        assertEquals(before, page.state)
        assertEquals(0, page.mordie.shifts)
    }

    @Test
    fun `dragging on a solved board does nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)
        val solved = page.state

        page.game.onDragStart(1, 0, 100.0, 100.0, PITCH)
        page.game.onDragMove(140.0, 100.0)
        assertNull(page.state.drag)
        page.game.onDragEnd()

        assertEquals(solved, page.state)
    }

    @Test
    fun `a drag while a dialog is open does nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onNew()
        val before = page.state

        page.drag(1, 0, 140.0 to 100.0)

        assertEquals(before, page.state)
    }

    @Test
    fun `toolbar and arrows wait until the drag is released`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onDragStart(1, 0, 100.0, 100.0, PITCH)
        page.game.onDragMove(140.0, 100.0)

        page.game.onNew()
        page.game.onArrow(Arrow.RIGHT, 2)
        page.game.onDragEnd()

        assertNull(page.state.dialog)
        assertEquals(listOf(8, 5, 6, 7), page.state.tiles.subList(4, 8))
        assertEquals(listOf(9, 10, 11, 12), page.state.tiles.subList(8, 12))
    }

    @Test
    fun `a cancelled drag changes nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        val before = page.state

        page.game.onDragStart(1, 0, 100.0, 100.0, PITCH)
        page.game.onDragMove(180.0, 100.0)
        page.game.onDragCancel()

        assertEquals(before, page.state)
    }

    @Test
    fun `shift_input on board_solved - arrows, drag or mixed`() = webTest {
        val arrows = WebHarness(backgroundScope).startOn()
        arrows.game.onArrow(Arrow.LEFT, 0)

        val drags = WebHarness(backgroundScope).startOn()
        drags.drag(0, 0, 60.0 to 100.0)

        val mixed = WebHarness(backgroundScope).startOn()
        mixed.drag(1, 0, 140.0 to 100.0)
        mixed.game.onArrow(Arrow.LEFT, 1)
        mixed.game.onArrow(Arrow.LEFT, 0)

        val solves = listOf(arrows, drags, mixed).map { it.props("board_solved").single() }
        assertEquals(listOf("arrows", "drag", "mixed"), solves.map { it["shift_input"] })
        assertTrue(solves.none { "shift_keys" in it }, "$solves")
        assertEquals(1, drags.props("screen_congratulations").size)
    }
}
