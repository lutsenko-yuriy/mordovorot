package mordie

import kotlin.math.sqrt

/** Admissible lower bound on the moves (GH-62: one row or column rotated by any amount) still
 *  needed to solve a board. A row move never changes a tile's row and changes the column of at
 *  most n tiles, a column move the reverse - so at least ceil(wrong-column tiles / n) row moves
 *  and ceil(wrong-row tiles / n) column moves remain, and the two sets are disjoint. */
object MoveMetricHeuristic {

    fun lowerBound(state: IntArray): Int {
        val n = sideOf(state)
        var wrongRow = 0
        var wrongCol = 0
        for (i in state.indices) {
            if (state[i] / n != i / n) wrongRow++
            if (state[i] % n != i % n) wrongCol++
        }
        return of(n, wrongRow, wrongCol)
    }

    internal fun of(n: Int, wrongRow: Int, wrongCol: Int): Int = (wrongRow + n - 1) / n + (wrongCol + n - 1) / n

    internal fun sideOf(state: IntArray): Int {
        val n = sqrt(state.size.toDouble()).toInt()
        require(n * n == state.size) { "Board state size must be a perfect square, got ${state.size}" }
        return n
    }
}
