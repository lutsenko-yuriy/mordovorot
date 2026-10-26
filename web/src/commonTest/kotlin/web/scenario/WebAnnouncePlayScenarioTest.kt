package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mordie.SearchProgress
import web.Arrow

/** Tiles this far apart, in pixels. */
private const val PITCH = 40.0

/** GH-166 scenarios: what a screen reader hears while playing - shifts, drags and the win. */
class WebAnnouncePlayScenarioTest {

    @Test
    fun `an arrow announces its line and direction, and the move count when a move starts`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onArrow(Arrow.RIGHT, 1)
        page.game.onArrow(Arrow.RIGHT, 1)
        page.game.onArrow(Arrow.UP, 3)

        assertEquals(listOf("Row 2 right, 1 move", "Row 2 right", "Column 4 up, 2 moves"), page.said)
    }

    @Test
    fun `a drag announces once, on release`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onDragStart(0, 0, 100.0, 100.0, PITCH)
        page.game.onDragMove(60.0, 101.0)
        page.game.onDragMove(20.0, 100.0)
        assertEquals(emptyList(), page.said)
        page.game.onDragEnd()

        assertEquals(listOf("Row 1 left 2, 1 move"), page.said)
    }

    @Test
    fun `a drag released where it started announces nothing`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onDragStart(0, 2, 100.0, 100.0, PITCH)
        page.game.onDragMove(140.0, 100.0)
        page.game.onDragMove(100.0, 100.0)
        page.game.onDragEnd()

        assertEquals(emptyList(), page.said)
    }

    @Test
    fun `the winning shift announces only Solved in M moves`() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.game.onArrow(Arrow.LEFT, 0)

        assertTrue(page.state.solved)
        assertEquals(listOf("Solved in 1 move"), page.said)
    }

    @Test
    fun `a solved board announces nothing more`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)
        page.said.clear()

        page.game.onArrow(Arrow.RIGHT, 1)
        page.game.onDragStart(1, 0, 100.0, 100.0, PITCH)
        page.game.onDragMove(140.0, 100.0)
        page.game.onDragEnd()

        assertEquals(emptyList(), page.said)
    }

    @Test
    fun `Mordie's lines are never announced`() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.mordieSearches.latest.progress = SearchProgress(moves = 3, found = false, elapsedMs = 0)
        // Row 2 back and forth: row 1 stays off, so the board never solves.
        fun shifts(count: Int) = repeat(count) { page.game.onArrow(if (it % 2 == 0) Arrow.RIGHT else Arrow.LEFT, 1) }

        shifts(20)
        page.mordieSearches.latest.progress = SearchProgress(moves = 11, found = true, elapsedMs = 0)
        shifts(5)

        val lines = page.frames.mapNotNull { it.mordie }.toSet()
        assertTrue(lines.any { it.startsWith("Found it!") } && lines.any { it.startsWith("I have already made") }, "$lines")
        assertEquals(25, page.said.size)
        assertTrue(page.said.all { it.matches(Regex("""Row 2 (left|right)(, \d+ moves?)?""")) }, "${page.said}")
    }
}
