package web.tutorial

import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.Direction
import viewmodel.KeyboardLayout
import viewmodel.TileSelection

/** GH-290: the three lines under a step's text on a computer. */
class TutorialWaysTest {
    private val tutorial = texts().tutorial

    private fun or(letter: String, arrow: String) = texts().keys.letterOrArrow(letter, arrow)


    private fun ways(
        direction: Direction,
        line: Int,
        tiles: Int,
        at: TileSelection? = TileSelection(0, 0),
        layout: KeyboardLayout = KeyboardLayout.RIGHT_HANDED,
    ) = TutorialWays.lines(direction, line, tiles, at, layout)

    @Test
    fun `in the asked line, the keys are the shift only`() {
        assertEquals(listOf(tutorial.dragLeft(1), tutorial.click("◀", 1), tutorial.press(or("H", "←"), 1)), ways(Direction.LEFT, 0, 1))
    }

    @Test
    fun `two tiles say so on each line`() {
        assertEquals(
            listOf(tutorial.dragRight(2), tutorial.click("▶", 2), tutorial.press(or("L", "→"), 2)),
            ways(Direction.RIGHT, 2, 2, TileSelection(2, 0)),
        )
    }

    @Test
    fun `a column away asks for a select key first`() {
        assertEquals(listOf(tutorial.dragUp(1), tutorial.click("▲", 1), tutorial.pressThen("D", 1, or("K", "↑"), 1)), ways(Direction.UP, 1, 1))
    }

    @Test
    fun `a row asks only up and down select keys, a column only left and right`() {
        assertEquals(tutorial.pressThen("S", 2, or("L", "→"), 2), ways(Direction.RIGHT, 2, 2).last())
        assertEquals(tutorial.pressThen("D", 3, or("J", "↓"), 1), ways(Direction.DOWN, 3, 1).last())
    }

    @Test
    fun `the select keys go straight, towards the line`() {
        assertEquals(tutorial.pressThen("W", 1, or("H", "←"), 1), ways(Direction.LEFT, 0, 1, TileSelection(1, 3)).last())
        assertEquals(tutorial.pressThen("A", 2, or("J", "↓"), 1), ways(Direction.DOWN, 1, 1, TileSelection(2, 3)).last())
    }

    @Test
    fun `the left hand selects with the other keys and shifts with letters`() {
        val left = KeyboardLayout.LEFT_HANDED
        assertEquals(tutorial.press("A", 1), ways(Direction.LEFT, 0, 1, layout = left).last())
        assertEquals(tutorial.pressThen(or("L", "→"), 1, "W", 1), ways(Direction.UP, 1, 1, layout = left).last())
    }

    @Test
    fun `tiles left count down`() {
        assertEquals(listOf(tutorial.dragRight(1), tutorial.click("▶", 1), tutorial.press(or("L", "→"), 1)), ways(Direction.RIGHT, 2, 1, TileSelection(2, 1)))
    }

    @Test
    fun `no selection leaves the shift keys alone`() {
        assertEquals("Press J or ↓", ways(Direction.DOWN, 3, 1, at = null).last())
    }
}
