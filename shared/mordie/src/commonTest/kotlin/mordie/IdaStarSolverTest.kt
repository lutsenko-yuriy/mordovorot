package mordie

import board_model.BoardImpl
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdaStarSolverTest {

    private fun solve(state: IntArray, ruledOut: Int = -1): IdaStarSolver =
        IdaStarSolver(state, ruledOut).also { while (!it.step(10_000)) { } }

    /** [moves] random rotations from the solved [side]x[side] board. */
    private fun scramble(side: Int, moves: Int, random: Random): IntArray {
        val board = BoardImpl(side).apply { restoreState(IntArray(side * side) { it }) }
        repeat(moves) {
            val line = random.nextInt(side)
            if (random.nextBoolean()) board.shiftRight(line) else board.shiftDown(line)
        }
        return board.boardArray.copyOf()
    }

    /** Plays [solver]'s solution on a real board from [state]. */
    private fun replay(state: IntArray, solver: IdaStarSolver): BoardImpl {
        val side = sqrt(state.size.toDouble()).toInt()
        val board = BoardImpl(side).apply { restoreState(state) }
        for (move in solver.solution()) repeat(move.amount) {
            if (move.isRow) board.shiftRight(move.index) else board.shiftDown(move.index)
        }
        return board
    }

    @Test
    fun `a solved board needs 0 moves`() {
        val solver = IdaStarSolver(IntArray(9) { it })

        assertTrue(solver.step(1))
        assertEquals(0, solver.solutionLength)
    }

    @Test
    fun `one rotation needs 1 move`() {
        assertEquals(1, solve(intArrayOf(2, 0, 1, 3, 4, 5, 6, 7, 8)).solutionLength)
    }

    @Test
    fun `finds the exact minimum on sampled 3x3 boards`() {
        val states = ThreeByThreeOracle.solvableStates().toList()
        val random = Random(42)
        repeat(300) {
            val state = states[random.nextInt(states.size)]
            assertEquals(ThreeByThreeOracle.distance(state), solve(state).solutionLength, state.joinToString(" "))
        }
    }

    @Test
    fun `a 4x4 scramble of k moves is solved in at most k`() {
        val random = Random(7)
        repeat(5) {
            val state = scramble(4, 6, random)
            val length = solve(state).solutionLength!!
            assertTrue(length <= 6, "length $length for ${state.joinToString(" ")}")
        }
    }

    @Test
    fun `the solution replayed on BoardImpl solves it in exactly solutionLength moves`() {
        val random = Random(3)
        repeat(5) {
            val state = scramble(4, 6, random)
            val solver = solve(state)

            val board = replay(state, solver)

            assertTrue(board.isCorrect())
            assertEquals(solver.solutionLength, board.moveCount)
        }
    }

    @Test
    fun `ruledOut starts below the heuristic and never decreases`() {
        val state = scramble(4, 8, Random(11))
        val solver = IdaStarSolver(state)
        assertEquals(MoveMetricHeuristic.lowerBound(state) - 1, solver.ruledOut)

        var previous = solver.ruledOut
        while (!solver.step(1)) {
            assertTrue(solver.ruledOut >= previous)
            previous = solver.ruledOut
        }
        assertEquals(solver.solutionLength!! - 1, solver.ruledOut)
    }

    @Test
    fun `a resumed search finds the same length and never reports below the saved N`() {
        val state = scramble(4, 6, Random(5))
        val fresh = solve(state).solutionLength!!

        val resumed = IdaStarSolver(state, ruledOut = fresh - 1)
        assertEquals(fresh - 1, resumed.ruledOut)
        while (!resumed.step(1)) assertTrue(resumed.ruledOut >= fresh - 1)

        assertEquals(fresh, resumed.solutionLength)
    }

    @Test
    fun `stepping is deterministic`() {
        val state = scramble(4, 7, Random(9))
        val a = IdaStarSolver(state)
        val b = IdaStarSolver(state)
        repeat(50) {
            a.step(97)
            b.step(97)
            assertEquals(a.ruledOut, b.ruledOut)
        }
        assertEquals(a.solutionLength, b.solutionLength)
    }

    @Test
    fun `the original arrangement is not mutated`() {
        val state = scramble(4, 6, Random(1))
        val copy = state.copyOf()

        solve(state)

        assertContentEquals(copy, state)
    }

    @Test
    fun `an unfinished search has no solution yet`() {
        val solver = IdaStarSolver(scramble(5, 20, Random(2)))

        solver.step(10)

        assertNull(solver.solutionLength)
    }
}
