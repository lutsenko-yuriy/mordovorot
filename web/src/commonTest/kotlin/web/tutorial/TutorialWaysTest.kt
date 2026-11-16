package web.tutorial

import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.Direction
import viewmodel.KeyboardLayout
import viewmodel.TileSelection

/** GH-290: the three lines under a step's text on a computer. */
class TutorialWaysTest {

    private fun ways(
        direction: Direction,
        line: Int,
        tiles: Int,
        at: TileSelection? = TileSelection(0, 0),
        layout: KeyboardLayout = KeyboardLayout.RIGHT_HANDED,
    ) = TutorialWays.lines(direction, line, tiles, at, layout)

    @Test
    fun `in the asked line, the keys are the shift only`() {
        assertEquals(listOf("Drag it left", "Click its ◀", "Press H or ←"), ways(Direction.LEFT, 0, 1))
    }

    @Test
    fun `two tiles say so on each line`() {
        assertEquals(
            listOf("Drag it two tiles right", "Click its ▶ twice", "Press L or → twice"),
            ways(Direction.RIGHT, 2, 2, TileSelection(2, 0)),
        )
    }

    @Test
    fun `a column away asks for a select key first`() {
        assertEquals(listOf("Drag it up", "Click its ▲", "Press D, then K or ↑"), ways(Direction.UP, 1, 1))
    }

    @Test
    fun `a row asks only up and down select keys, a column only left and right`() {
        assertEquals("Press S twice, then L or → twice", ways(Direction.RIGHT, 2, 2).last())
        assertEquals("Press D three times, then J or ↓", ways(Direction.DOWN, 3, 1).last())
    }

    @Test
    fun `the select keys go straight, towards the line`() {
        assertEquals("Press W, then H or ←", ways(Direction.LEFT, 0, 1, TileSelection(1, 3)).last())
        assertEquals("Press A twice, then J or ↓", ways(Direction.DOWN, 1, 1, TileSelection(2, 3)).last())
    }

    @Test
    fun `the left hand selects with the other keys and shifts with letters`() {
        val left = KeyboardLayout.LEFT_HANDED
        assertEquals("Press A", ways(Direction.LEFT, 0, 1, layout = left).last())
        assertEquals("Press L or →, then W", ways(Direction.UP, 1, 1, layout = left).last())
    }

    @Test
    fun `tiles left count down`() {
        assertEquals(listOf("Drag it right", "Click its ▶", "Press L or →"), ways(Direction.RIGHT, 2, 1, TileSelection(2, 1)))
    }

    @Test
    fun `no selection leaves the shift keys alone`() {
        assertEquals("Press J or ↓", ways(Direction.DOWN, 3, 1, at = null).last())
    }
}
