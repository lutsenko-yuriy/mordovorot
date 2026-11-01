package mordie

import board_model.BoardImpl
import board_model.Solvability
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThreeCycleSolverTest {

    private fun solve(state: IntArray): ThreeCycleSolver =
        ThreeCycleSolver(state).also { assertTrue(it.step(Int.MAX_VALUE)) }

    private fun sideOf(state: IntArray) = sqrt(state.size.toDouble()).toInt()

    /** A seeded random board of [side] that is solvable (any arrangement on an even side, an even one on an odd side). */
    private fun randomBoard(side: Int, random: Random): IntArray {
        val state = IntArray(side * side) { it }.also { it.shuffle(random) }
        return if (Solvability.isSolvable(state)) state else Solvability.repaired(state)
    }

    /** True when [state] is an odd permutation: the case that costs an even side its parity shift. */
    private fun isOdd(state: IntArray): Boolean {
        var inversions = 0
        for (i in state.indices) for (j in i + 1 until state.size) if (state[i] > state[j]) inversions++
        return inversions % 2 == 1
    }

    /** Plays [solver]'s solution on a real board from [state]. */
    private fun replay(state: IntArray, solver: ThreeCycleSolver): BoardImpl {
        val side = sideOf(state)
        val board = BoardImpl(side, maxSide = 9).apply { restoreState(state) }
        for (move in solver.solution()) repeat(move.amount) {
            if (move.isRow) board.shiftRight(move.index) else board.shiftDown(move.index)
        }
        return board
    }

    /** Plays [moves] on a plain array: much faster than a [BoardImpl] for the exhaustive 3x3 test. */
    private fun replayOnArray(state: IntArray, moves: List<SolutionMove>): IntArray {
        val n = sideOf(state)
        var board = state.copyOf()
        for (move in moves) {
            val next = board.copyOf()
            for (k in 0 until n) {
                if (move.isRow) next[move.index * n + (k + move.amount) % n] = board[move.index * n + k]
                else next[((k + move.amount) % n) * n + move.index] = board[k * n + move.index]
            }
            board = next
        }
        return board
    }

    private fun lengthBound(side: Int) = (if (side % 2 == 0) 1 else 0) + 4 * side * (side - 1) + 8 * (side - 2)

    @Test
    fun `a solved board needs 0 moves`() {
        val solver = ThreeCycleSolver(IntArray(16) { it })

        assertTrue(solver.step(1))
        assertEquals(0, solver.solutionLength)
        assertTrue(solver.solution().isEmpty())
    }

    @Test
    fun `an unsolvable odd-side board is rejected`() {
        assertFailsWith<IllegalArgumentException> { ThreeCycleSolver(intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8)) }
    }

    @Test
    fun `a state whose size is not a square is rejected`() {
        assertFailsWith<IllegalArgumentException> { ThreeCycleSolver(IntArray(8) { it }) }
    }

    @Test
    fun `an odd arrangement on an even side starts with a shift of row 0`() {
        val state = IntArray(16) { it }.also { it[0] = 1; it[1] = 0 }

        val solver = solve(state)

        val first = solver.solution().first()
        assertTrue(first.isRow && first.index == 0, "the shift may merge with the next move on row 0: $first")
        assertTrue(replay(state, solver).isCorrect())
    }

    @Test
    fun `an even arrangement needs no parity shift`() {
        val state = IntArray(16) { it }.also { it[0] = 1; it[1] = 2; it[2] = 0 }

        val solver = solve(state)

        assertTrue(solver.solutionLength!! < lengthBound(4))
        assertTrue(replay(state, solver).isCorrect())
    }

    @Test
    fun `a 3-cycle inside the last row alone is solved by the direct construction`() {
        val state = IntArray(25) { it }.also { it[20] = 22; it[22] = 21; it[21] = 20 }

        val solver = solve(state)

        assertTrue(solver.solutionLength!! <= 16)
        assertTrue(replay(state, solver).isCorrect())
    }

    @Test
    fun `solves random boards of every size from 3x3 to 9x9 and both parities`() {
        for (side in 3..9) {
            val random = Random(side)
            val parities = mutableSetOf<Boolean>()
            repeat(40) {
                val state = randomBoard(side, random)
                parities += isOdd(state)

                val solver = solve(state)
                val board = replay(state, solver)

                assertTrue(board.isCorrect(), "side $side: ${state.joinToString(" ")}")
                assertEquals(solver.solutionLength, board.moveCount, "moves are merged into one per run, side $side")
                assertTrue(solver.solutionLength!! <= lengthBound(side), "side $side: ${solver.solutionLength}")
                assertTrue(solver.solution().all { it.amount in 1 until side })
            }
            if (side % 2 == 0) assertEquals(setOf(true, false), parities, "side $side sample covers both parities")
        }
    }

    @Test
    fun `solves every solvable 3x3 board`() {
        var boards = 0
        for (state in ThreeByThreeOracle.solvableStates()) {
            val solver = solve(state)

            assertContentEquals(IntArray(9) { it }, replayOnArray(state, solver.solution()), state.joinToString(" "))
            assertTrue(solver.solutionLength!! >= ThreeByThreeOracle.distance(state))
            assertTrue(solver.solutionLength!! <= lengthBound(3))
            boards++
        }
        assertEquals(181_440, boards)
    }

    @Test
    fun `places at most one tile per step`() {
        val state = randomBoard(9, Random(1))
        val solver = ThreeCycleSolver(state)

        assertFalse(solver.step(1))
        assertNull(solver.solutionLength)
    }

    @Test
    fun `a solve takes at most one step per tile`() {
        val state = randomBoard(9, Random(2))
        val solver = ThreeCycleSolver(state)

        var steps = 0
        while (!solver.step(1)) steps++

        assertTrue(steps < 81 - 2)
    }

    @Test
    fun `the slice size does not change the solution`() {
        for (side in listOf(4, 5)) {
            val state = randomBoard(side, Random(3))
            val whole = solve(state).solution()
            val sliced = ThreeCycleSolver(state).also { while (!it.step(1)) { } }.solution()

            assertEquals(whole, sliced)
        }
    }

    @Test
    fun `the solution is not available before the solver is done`() {
        val solver = ThreeCycleSolver(randomBoard(5, Random(4)))

        assertFailsWith<IllegalStateException> { solver.solution() }
    }

    @Test
    fun `leaves the original array alone`() {
        val state = randomBoard(6, Random(5))
        val copy = state.copyOf()

        solve(state)

        assertContentEquals(copy, state)
    }
}
