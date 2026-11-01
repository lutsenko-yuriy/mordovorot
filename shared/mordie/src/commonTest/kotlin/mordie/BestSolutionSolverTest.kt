package mordie

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BestSolutionSolverTest {

    /** 3x3, needs 2 moves: row 0 rotated, then column 2 rotated. */
    private val twoMoves = intArrayOf(2, 0, 8, 3, 4, 1, 6, 7, 5)

    /** 5x5 reversed (an even permutation, so solvable) - the exact search never finishes it. */
    private val hard = IntArray(25) { 24 - it }

    private fun BestSolutionSolver.runToProven() {
        while (!step(1_000)) { }
    }

    @Test
    fun `the fast answer arrives on the first step, before the exact search could finish`() {
        val solver = BestSolutionSolver(hard)

        assertFalse(solver.step(10))

        assertTrue(solver.solution!!.isNotEmpty())
        assertEquals(SolutionSource.FAST_SOLVER, solver.source)
        assertFalse(solver.provenShortest)
        assertEquals(solver.solution!!.size, solver.bestLength)
    }

    @Test
    fun `nothing is found before the first step`() {
        val solver = BestSolutionSolver(hard)

        assertNull(solver.bestLength)
        assertNull(solver.solution)
        assertNull(solver.source)
        assertFalse(solver.provenShortest)
    }

    @Test
    fun `the exact search replaces the fast answer with a shorter one`() {
        val solver = BestSolutionSolver(twoMoves)

        solver.step(1)
        val fastLength = solver.bestLength!!
        solver.runToProven()

        assertTrue(fastLength > 2)
        assertEquals(2, solver.bestLength)
        assertEquals(2, solver.solution!!.size)
        assertTrue(solver.provenShortest)
    }

    @Test
    fun `the source stays the one that gave the first solution`() {
        val solver = BestSolutionSolver(twoMoves)

        solver.runToProven()

        assertEquals(SolutionSource.FAST_SOLVER, solver.source)
    }

    @Test
    fun `an answer is proven once the longest length ruled out is one below it`() {
        // The fast solution of a board one move from solved is that one move: ruling out 0 proves it.
        val oneMove = intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)
        val solver = BestSolutionSolver(oneMove)

        solver.runToProven()

        assertEquals(1, solver.bestLength)
        assertTrue(solver.provenShortest)
    }

    @Test
    fun `stepping stops once proven`() {
        val solver = BestSolutionSolver(twoMoves)
        solver.runToProven()
        val ruledOut = solver.ruledOut

        assertTrue(solver.step(1_000))

        assertEquals(ruledOut, solver.ruledOut)
        assertEquals(2, solver.bestLength)
    }

    @Test
    fun `a saved N does not stop the fast solver`() {
        val solver = BestSolutionSolver(hard, ruledOut = 9)

        solver.step(10)

        assertTrue(solver.bestLength!! > 10)
        assertTrue(solver.ruledOut >= 9)
    }

    @Test
    fun `a solved board is proven with no moves and an exact source`() {
        val solver = BestSolutionSolver(IntArray(9) { it })

        assertTrue(solver.step(10))

        assertEquals(0, solver.bestLength)
        assertEquals(emptyList(), solver.solution)
        assertEquals(SolutionSource.EXACT_SEARCH, solver.source)
    }

    @Test
    fun `the best length is the true minimum on every 3x3 board sampled`() {
        ThreeByThreeOracle.solvableStates().filterIndexed { i, _ -> i % 997 == 0 }.forEach { state ->
            val solver = BestSolutionSolver(state)
            solver.runToProven()
            assertEquals(ThreeByThreeOracle.distance(state), solver.bestLength)
        }
    }

    @Test
    fun `progress reports the best length while searching and the ruled out length`() {
        val solver = BestSolutionSolver(hard)
        solver.step(10)

        val progress = solver.progress(elapsedMs = 7)

        assertTrue(progress.found)
        assertEquals(solver.bestLength, progress.moves)
        assertEquals(solver.ruledOut, progress.ruledOut)
        assertFalse(progress.provenShortest)
        assertEquals(SolutionSource.FAST_SOLVER, progress.source)
        assertEquals(7, progress.elapsedMs)
    }

    @Test
    fun `progress before any solution reports the ruled out length as N`() {
        val progress = BestSolutionSolver(hard, ruledOut = 9).progress(elapsedMs = 0)

        assertFalse(progress.found)
        assertEquals(9, progress.moves)
        assertEquals(9, progress.ruledOut)
        assertNull(progress.solution)
    }
}
