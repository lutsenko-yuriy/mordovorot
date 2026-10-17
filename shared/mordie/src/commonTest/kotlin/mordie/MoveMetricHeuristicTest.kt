package mordie

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoveMetricHeuristicTest {

    @Test
    fun `the solved board is 0`() {
        assertEquals(0, MoveMetricHeuristic.lowerBound(IntArray(16) { it }))
    }

    @Test
    fun `one row rotation is 1`() {
        // 4x4, row 1 rotated right once - four tiles in the wrong column, none in the wrong row.
        val state = intArrayOf(0, 1, 2, 3, 7, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15)

        assertEquals(1, MoveMetricHeuristic.lowerBound(state))
    }

    @Test
    fun `wrong-row and wrong-column tiles each round up per line`() {
        // 3x3, tiles 0 and 4 swapped: both in the wrong row and the wrong column.
        val state = intArrayOf(4, 1, 2, 3, 0, 5, 6, 7, 8)

        assertEquals(2, MoveMetricHeuristic.lowerBound(state))
    }

    @Test
    fun `never overestimates on any solvable 3x3 board`() {
        var checked = 0
        ThreeByThreeOracle.solvableStates().forEach { state ->
            val bound = MoveMetricHeuristic.lowerBound(state)
            val exact = ThreeByThreeOracle.distance(state)
            assertTrue(bound <= exact, "bound $bound > distance $exact for ${state.joinToString(" ")}")
            checked++
        }
        assertEquals(181_440, checked)
    }
}
