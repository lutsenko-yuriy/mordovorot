package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import viewmodel.KeyboardLayout.LEFT_HANDED
import viewmodel.KeyboardLayout.RIGHT_HANDED

/** ADR-0003's key scheme, shared by the terminal and the web (GH-171). */
class KeySchemeTest {

    private fun letter(char: Char, layout: KeyboardLayout) = KeyScheme.boardKey(char, null, layout)
    private fun arrow(direction: Direction, layout: KeyboardLayout) = KeyScheme.boardKey(null, direction, layout)
    private fun solutionLetter(char: Char) = KeyScheme.solutionKey(char, null, enter = false)

    private val directionsByLetter = listOf(
        "wk" to Direction.UP,
        "sj" to Direction.DOWN,
        "ah" to Direction.LEFT,
        "dl" to Direction.RIGHT,
    )

    @Test
    fun `right-handed - WASD select, HJKL shift with HJKL, the arrows with ARROW`() {
        for ((letters, direction) in directionsByLetter) {
            assertEquals(BoardKey.Select(direction), letter(letters[0], RIGHT_HANDED))
            assertEquals(BoardKey.Shift(direction, ShiftKey.HJKL), letter(letters[1], RIGHT_HANDED))
            assertEquals(BoardKey.Shift(direction, ShiftKey.ARROW), arrow(direction, RIGHT_HANDED))
        }
    }

    @Test
    fun `left-handed - HJKL and the arrows select, WASD shift with no shift key`() {
        for ((letters, direction) in directionsByLetter) {
            assertEquals(BoardKey.Shift(direction, null), letter(letters[0], LEFT_HANDED))
            assertEquals(BoardKey.Select(direction), letter(letters[1], LEFT_HANDED))
            assertEquals(BoardKey.Select(direction), arrow(direction, LEFT_HANDED))
        }
    }

    @Test
    fun `board letters work in either case`() {
        assertEquals(letter('d', RIGHT_HANDED), letter('D', RIGHT_HANDED))
        assertEquals(letter('h', RIGHT_HANDED), letter('H', RIGHT_HANDED))
        assertEquals(letter('w', LEFT_HANDED), letter('W', LEFT_HANDED))
        assertEquals(letter('j', LEFT_HANDED), letter('J', LEFT_HANDED))
    }

    @Test
    fun `other board keys do nothing in either layout`() {
        for (layout in KeyboardLayout.entries) {
            assertNull(letter('x', layout))
            assertNull(letter(' ', layout))
            assertNull(KeyScheme.boardKey(null, null, layout))
        }
    }

    @Test
    fun `solution screen - left steps back, right, Space and Enter step on`() {
        for (char in "ahAH") assertEquals(SolutionKey.PREV, solutionLetter(char))
        for (char in "dlDL ") assertEquals(SolutionKey.NEXT, solutionLetter(char))
        assertEquals(SolutionKey.PREV, KeyScheme.solutionKey(null, Direction.LEFT, enter = false))
        assertEquals(SolutionKey.NEXT, KeyScheme.solutionKey(null, Direction.RIGHT, enter = false))
        assertEquals(SolutionKey.NEXT, KeyScheme.solutionKey(null, null, enter = true))
    }

    @Test
    fun `solution screen - R restarts, up and down scroll, other keys do nothing`() {
        assertEquals(SolutionKey.RESTART, solutionLetter('r'))
        assertEquals(SolutionKey.RESTART, solutionLetter('R'))
        for (char in "wkWK") assertEquals(SolutionKey.SCROLL_UP, solutionLetter(char))
        for (char in "sjSJ") assertEquals(SolutionKey.SCROLL_DOWN, solutionLetter(char))
        assertEquals(SolutionKey.SCROLL_UP, KeyScheme.solutionKey(null, Direction.UP, enter = false))
        assertEquals(SolutionKey.SCROLL_DOWN, KeyScheme.solutionKey(null, Direction.DOWN, enter = false))
        assertNull(solutionLetter('x'))
        assertNull(KeyScheme.solutionKey(null, null, enter = false))
    }

    @Test
    fun `boardKeyLines names each layout's keys`() {
        assertEquals(
            listOf("Select tile    W A S D", "Shift row      ← →  H L", "Shift column   ↑ ↓  K J"),
            KeyScheme.boardKeyLines(RIGHT_HANDED),
        )
        assertEquals(
            listOf("Select tile    ← ↑ ↓ →  H J K L", "Shift row      A D", "Shift column   W S"),
            KeyScheme.boardKeyLines(LEFT_HANDED),
        )
    }

    @Test
    fun `the solution key lines name its keys`() {
        assertEquals(
            listOf("Solution: ← → A D H L Prev/Next ·", "  Space/Enter Next · R Restart ·", "  ↑ ↓ W S K J scroll"),
            KeyScheme.SOLUTION_KEY_LINES,
        )
    }
}
