package mordie

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-282: the pattern-database bound is a lower bound on the move-metric distance, built in slices. */
class PatternDatabaseTest {

    private fun built(side: Int, slice: Int = Int.MAX_VALUE): PatternDatabase =
        PatternDatabase(side).also { while (!it.done) it.step(slice) }

    private fun solved(side: Int) = IntArray(side * side) { it }

    /** [state] moved by torus translation ([dr], [dc]) and, when [transpose], the transpose: tile labels follow their cells. */
    private fun image(state: IntArray, side: Int, dr: Int, dc: Int, transpose: Boolean): IntArray {
        fun cellOf(cell: Int): Int {
            val r = (cell / side + dr) % side
            val c = (cell % side + dc) % side
            return if (transpose) c * side + r else r * side + c
        }
        val out = IntArray(state.size)
        for (cell in state.indices) out[cellOf(cell)] = cellOf(state[cell])
        return out
    }

    @Test
    fun `the solved board is 0`() {
        assertEquals(0, built(3).lowerBound(solved(3)))
        assertEquals(0, built(4).lowerBound(solved(4)))
    }

    @Test
    fun `one rotation is 1`() {
        assertEquals(1, built(3).lowerBound(intArrayOf(2, 0, 1, 3, 4, 5, 6, 7, 8)))
        assertEquals(1, built(4).lowerBound(IntArray(16) { if (it < 4) (it + 3) % 4 else it }))
    }

    @Test
    fun `the 3x3 bound never overestimates on any board`() {
        val database = built(3)
        var tighter = 0
        for (state in ThreeByThreeOracle.solvableStates()) {
            val bound = database.lowerBound(state)
            val exact = ThreeByThreeOracle.distance(state)
            assertTrue(bound <= exact, "${state.joinToString(" ")}: bound $bound, exact $exact")
            if (bound > MoveMetricHeuristic.lowerBound(state)) tighter++
        }
        assertTrue(tighter > 0)
    }

    @Test
    fun `the 4x4 build sees every state of the pattern, and the farthest is 8 moves away`() {
        val database = built(4)

        assertEquals(524_160, database.seen)
        assertEquals(8, database.farthest)
    }

    @Test
    fun `a build in slices equals a one-shot build`() {
        val sliced = built(4, slice = 1_000)
        val whole = built(4)
        val random = Random(7)
        repeat(300) {
            val state = solved(4).also { it.shuffle(random) }
            assertEquals(whole.lowerBound(state), sliced.lowerBound(state))
        }
    }

    @Test
    fun `the queue is let go once the build is whole, whatever the slice`() {
        for (slice in listOf(200, 1_000, Int.MAX_VALUE)) {
            assertEquals(1, built(4, slice).queueSize, "slice $slice")
        }
    }

    @Test
    fun `a board and its translated or transposed image have the same bound`() {
        val database = built(4)
        val random = Random(11)
        repeat(100) {
            val state = solved(4).also { it.shuffle(random) }
            val bound = database.lowerBound(state)
            for (transpose in listOf(false, true)) for (dr in 0 until 4) for (dc in 0 until 4) {
                assertEquals(bound, database.lowerBound(image(state, 4, dr, dc, transpose)))
            }
        }
    }

    @Test
    fun `the lazy bound stops once over the limit and is otherwise the full bound`() {
        val database = built(4)
        val random = Random(3)
        repeat(300) {
            val state = solved(4).also { it.shuffle(random) }
            val pos = IntArray(16).also { p -> state.forEachIndexed { cell, tile -> p[tile] = cell } }
            val full = database.lowerBound(state)
            assertEquals(full, database.lowerBound(pos, floor = 0, limit = Int.MAX_VALUE))
            for (limit in 0..10) {
                val lazy = database.lowerBound(pos, floor = 0, limit = limit)
                if (full > limit) assertTrue(lazy in limit + 1..full) else assertEquals(full, lazy)
            }
        }
    }
}
