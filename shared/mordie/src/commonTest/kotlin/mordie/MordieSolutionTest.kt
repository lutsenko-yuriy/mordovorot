package mordie

import testing.FakeMordieSearchFactory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** GH-126: Mordie keeps its solution's moves and the point it revealed them, for Give Up. */
class MordieSolutionTest {

    private val factory = FakeMordieSearchFactory()
    private val mordie = Mordie(factory)
    private val board = intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 2, amount = 2))

    private fun shifts(count: Int) = repeat(count) { mordie.onShift(0) }

    private fun found(moves: Int, solution: List<SolutionMove>? = null) = SearchProgress(moves, found = true, elapsedMs = 1234, solution)

    @Test
    fun `the reveal keeps Mordie's path and the reveal point`() {
        mordie.startFresh(board)
        shifts(8)
        factory.latest.progress = found(2, path)

        mordie.onShift(3)
        mordie.onShift(4)

        assertEquals(path, mordie.solution)
        assertEquals(10, mordie.shiftsAtReveal)
        assertEquals(4, mordie.playerMovesAtReveal)
        shifts(5)
        assertEquals(10, mordie.shiftsAtReveal)
    }

    @Test
    fun `before the reveal there is no path or reveal point`() {
        mordie.startFresh(board)
        factory.latest.progress = found(2, path)
        shifts(4)

        assertNull(mordie.solution)
        assertNull(mordie.shiftsAtReveal)
        assertNull(mordie.playerMovesAtReveal)
    }

    @Test
    fun `a found progress with no path leaves the solution null`() {
        mordie.startFresh(board)
        factory.latest.progress = found(2)
        shifts(5)

        assertEquals(MordieLine.Found(2), mordie.line())
        assertNull(mordie.solution)
    }

    @Test
    fun `onSolved keeps the path of a search found but not yet revealed`() {
        mordie.startFresh(board)
        factory.latest.progress = found(2, path)

        mordie.onSolved()

        assertEquals(path, mordie.solution)
        assertNull(mordie.shiftsAtReveal)
    }

    @Test
    fun `resume and restoreSolved carry the saved path, with no reveal point`() {
        mordie.resume(board, moves = 2, found = true, solution = path)
        assertEquals(path, mordie.solution)
        assertNull(mordie.shiftsAtReveal)
        assertNull(mordie.playerMovesAtReveal)

        mordie.restoreSolved(board, moves = 2, found = true, solution = path)
        assertEquals(path, mordie.solution)

        mordie.resume(board, moves = 2, found = true)
        assertNull(mordie.solution)
    }

    @Test
    fun `a new game forgets the path and the reveal point`() {
        mordie.startFresh(board)
        factory.latest.progress = found(2, path)
        shifts(5)

        mordie.startFresh(board)

        assertNull(mordie.solution)
        assertNull(mordie.shiftsAtReveal)
        assertNull(mordie.playerMovesAtReveal)
    }

    @Test
    fun `snapshot includes the path, revealed or live`() {
        mordie.startFresh(board)
        assertNull(mordie.snapshot()!!.solution)

        factory.latest.progress = found(2, path)
        assertEquals(path, mordie.snapshot()!!.solution)

        mordie.restoreSolved(board, moves = 2, found = true, solution = path)
        assertEquals(path, mordie.snapshot()!!.solution)
    }

    @Test
    fun `original is a copy of the bound arrangement, null when Mordie sits out`() {
        assertNull(mordie.original())
        mordie.startFresh(board)

        mordie.original()!![0] = 7

        assertContentEquals(board, mordie.original())
        mordie.clear()
        assertNull(mordie.original())
    }
}
