package board_model

import kotlin.math.sqrt

/** The single home for which boards can be solved by shifts (GH-74). A shift is an n-cycle,
 *  sign (-1)^(n-1): on even sides shifts are odd and every board is solvable; on odd sides
 *  every shift is even, so a board is solvable iff its permutation from the solved order is
 *  even. */
object Solvability {

    /** The side comes from [state]'s size, same as [BoardModel.restoreState]. */
    fun isSolvable(state: IntArray): Boolean {
        val side = sqrt(state.size.toDouble()).toInt()
        return side % 2 == 0 || isEvenPermutation(state)
    }

    /** A copy of the unsolvable [state] with two tiles swapped, which flips parity. Swaps
     *  positions 0/1, or the last two positions if 0/1 would give the solved board. */
    fun repaired(state: IntArray): IntArray {
        require(!isSolvable(state)) { "Board is already solvable" }
        val fixed = state.swapped(0, 1)
        return if (fixed.indices.all { fixed[it] == it }) state.swapped(state.size - 2, state.size - 1) else fixed
    }

    /** Even iff (size - number of cycles) is even. */
    private fun isEvenPermutation(state: IntArray): Boolean {
        val visited = BooleanArray(state.size)
        var cycles = 0
        for (start in state.indices) {
            if (visited[start]) continue
            cycles++
            var i = start
            while (!visited[i]) {
                visited[i] = true
                i = state[i]
            }
        }
        return (state.size - cycles) % 2 == 0
    }

    private fun IntArray.swapped(i: Int, j: Int) = copyOf().also { it[i] = this[j]; it[j] = this[i] }
}
