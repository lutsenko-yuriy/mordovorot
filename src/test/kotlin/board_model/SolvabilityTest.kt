package board_model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SolvabilityTest {

    private fun solved(side: Int) = IntArray(side * side) { it }

    private fun IntArray.swapped(i: Int, j: Int) = copyOf().also { it[i] = this[j]; it[j] = this[i] }

    @Test
    fun `the solved board is solvable on every size`() {
        for (side in BoardSize.MIN..BoardSize.MAX) assertTrue(Solvability.isSolvable(solved(side)), "side $side")
    }

    @Test
    fun `a single swap is unsolvable on odd sides`() {
        assertFalse(Solvability.isSolvable(solved(3).swapped(0, 1)))
        assertFalse(Solvability.isSolvable(solved(5).swapped(3, 17)))
    }

    @Test
    fun `every 4x4 board is solvable, a single swap included`() {
        assertTrue(Solvability.isSolvable(solved(4).swapped(0, 1)))
    }

    @Test
    fun `every shift keeps an odd-side board solvable`() {
        for (side in listOf(3, 5)) {
            val board = BoardImpl(side).apply { restoreState(solved(side)) }
            for (i in 0 until side) {
                board.shiftLeft(i)
                assertTrue(Solvability.isSolvable(board.boardArray), "side $side after shiftLeft($i)")
                board.shiftDown(i)
                assertTrue(Solvability.isSolvable(board.boardArray), "side $side after shiftDown($i)")
            }
        }
    }

    @Test
    fun `exactly half of all 3x3 boards are solvable`() {
        var solvable = 0
        val state = IntArray(9) { it }
        fun permute(k: Int) {
            if (k == state.size) {
                if (Solvability.isSolvable(state)) solvable++
                return
            }
            for (i in k until state.size) {
                state[k] = state[i].also { state[i] = state[k] }
                permute(k + 1)
                state[k] = state[i].also { state[i] = state[k] }
            }
        }
        permute(0)

        assertEquals(181_440, solvable)
    }

    @Test
    fun `repaired swaps exactly two tiles and makes the board solvable`() {
        val unsolvable = intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8)

        val fixed = Solvability.repaired(unsolvable)

        assertEquals(intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8).toList(), fixed.toList())
        assertTrue(Solvability.isSolvable(fixed))
        assertEquals(intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8).toList(), unsolvable.toList(), "input untouched")
    }

    @Test
    fun `repaired never hands back the solved board`() {
        for (side in listOf(3, 5)) {
            val input = solved(side).swapped(0, 1)

            val fixed = Solvability.repaired(input)

            assertNotEquals(solved(side).toList(), fixed.toList(), "side $side")
            assertTrue(Solvability.isSolvable(fixed))
            assertEquals(2, fixed.indices.count { fixed[it] != input[it] })
        }
    }

    @Test
    fun `repaired rejects a board that is already solvable`() {
        assertFailsWith<IllegalArgumentException> { Solvability.repaired(solved(3)) }
    }
}
