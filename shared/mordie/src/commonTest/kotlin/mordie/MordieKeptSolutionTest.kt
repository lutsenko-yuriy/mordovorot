package mordie

import testing.FakeMordieSearchFactory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** GH-224: saves keep whether Mordie's answer is proven shortest, and the length ruled out. */
class MordieKeptSolutionTest {

    private val factory = FakeMordieSearchFactory()
    private val mordie = Mordie(factory)
    private val board = intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8)

    private fun move(amount: Int) = SolutionMove(isRow = true, index = 0, amount = amount)

    private fun path(moves: Int) = List(moves) { move(1) }

    private fun fast(moves: Int) = SearchProgress(
        moves, found = true, elapsedMs = 5, solution = path(moves),
        ruledOut = 3, provenShortest = false, source = SolutionSource.FAST_SOLVER,
    )

    private fun shifts(count: Int) = repeat(count) { mordie.onShift(0) }

    @Test
    fun `a solved save over an unproven answer is beaten by a shorter win`() {
        mordie.restoreSolved(board, moves = 40, found = true, solution = path(40), provenShortest = false, ruledOut = 3)

        assertEquals(MordieOutcome(40, found = true, provenShortest = false), mordie.outcome())
        assertEquals(MordieResult.BEAT_MORDIE, mordie.outcome()!!.result(30))
        assertFalse(mordie.provenShortest)
    }

    @Test
    fun `a solved save over a proven answer is a machine win`() {
        mordie.restoreSolved(board, moves = 12, found = true, solution = path(12))

        assertEquals(MordieResult.MACHINE, mordie.outcome()!!.result(11))
        assertTrue(mordie.provenShortest)
    }

    @Test
    fun `a solved save keeps the flag and R in its snapshot`() {
        mordie.restoreSolved(board, moves = 40, found = true, solution = path(40), provenShortest = false, ruledOut = 3)

        val snapshot = mordie.snapshot()!!

        assertEquals(40, snapshot.moves)
        assertFalse(snapshot.provenShortest)
        assertEquals(3, snapshot.ruledOut)
    }

    @Test
    fun `an answer found then solved past is saved unproven with R`() {
        mordie.startFresh(board)
        factory.latest.progress = fast(8)
        shifts(5)

        mordie.onSolved()
        val snapshot = mordie.snapshot()!!

        assertEquals(8, snapshot.moves)
        assertFalse(snapshot.provenShortest)
        assertEquals(3, snapshot.ruledOut)
    }

    @Test
    fun `a given-up unproven answer is saved unproven with R`() {
        mordie.startFresh(board)
        factory.latest.progress = fast(8)
        shifts(5)
        mordie.giveUp()

        val snapshot = mordie.snapshot()!!

        assertEquals(8, snapshot.moves)
        assertTrue(snapshot.found)
        assertFalse(snapshot.provenShortest)
        assertEquals(3, snapshot.ruledOut)
        assertContentEquals(path(8), snapshot.solution)
    }

    @Test
    fun `a solution-screen save of an unproven answer resumes unproven and starts no search`() {
        mordie.resume(board, moves = 8, found = true, solution = path(8), provenShortest = false, ruledOut = 3, solutionScreen = true)

        assertFalse(mordie.provenShortest)
        assertTrue(mordie.revealed)
        assertEquals(MordieLine.FoundUnproven(8), mordie.line())
        assertTrue(factory.started.isEmpty())
    }

    @Test
    fun `an unproven save is revealed at once and its search starts from R and the kept solution`() {
        mordie.resume(board, moves = 8, found = true, solution = path(8), provenShortest = false, ruledOut = 3)

        assertTrue(mordie.revealed)
        assertFalse(mordie.provenShortest)
        assertEquals(MordieLine.FoundUnproven(8), mordie.line())
        assertContentEquals(path(8), mordie.solution)
        assertEquals(1, factory.started.size)
        assertEquals(3, factory.latest.ruledOut)
        assertContentEquals(path(8), factory.latest.best)
    }

    @Test
    fun `a proven save starts no search`() {
        mordie.resume(board, moves = 8, found = true, solution = path(8))

        assertEquals(MordieLine.Found(8), mordie.line())
        assertTrue(factory.started.isEmpty())
    }

    @Test
    fun `a resumed search that finds a shorter answer announces it`() {
        mordie.resume(board, moves = 8, found = true, solution = path(8), provenShortest = false, ruledOut = 3)
        factory.latest.progress = SearchProgress(6, found = true, elapsedMs = 5, solution = path(6), ruledOut = 4, provenShortest = false)

        shifts(4)
        val check = mordie.onShift(0)

        assertEquals(MordieCheck.Shortened(moves = 6, previous = 8, provenShortest = false), check)
        assertEquals(MordieLine.Shorter(6), mordie.line())
    }

    @Test
    fun `a resumed search proving the kept answer shortest keeps the line`() {
        mordie.resume(board, moves = 8, found = true, solution = path(8), provenShortest = false, ruledOut = 3)
        factory.latest.progress = SearchProgress(8, found = true, elapsedMs = 5, solution = path(8), ruledOut = 7, provenShortest = true)

        shifts(4)

        assertNull(mordie.onShift(0))
        assertTrue(mordie.provenShortest)
        assertEquals(MordieLine.FoundUnproven(8), mordie.line())
    }

    @Test
    fun `saving a resumed unproven answer later keeps the search's newer R`() {
        mordie.resume(board, moves = 8, found = true, solution = path(8), provenShortest = false, ruledOut = 3)
        factory.latest.progress = SearchProgress(8, found = true, elapsedMs = 5, solution = path(8), ruledOut = 5, provenShortest = false)

        val snapshot = mordie.snapshot()!!

        assertTrue(snapshot.found)
        assertEquals(5, snapshot.ruledOut)
        assertContentEquals(path(8), snapshot.solution)
    }

    @Test
    fun `an unproven save without a solution searches from R alone`() {
        mordie.resume(board, moves = 8, found = true, solution = null, provenShortest = false, ruledOut = 3)

        assertEquals(3, factory.latest.ruledOut)
        assertNull(factory.latest.best)
    }

    @Test
    fun `saving a resumed unproven answer again keeps it found, unproven, with R`() {
        mordie.resume(board, moves = 8, found = true, solution = path(8), provenShortest = false, ruledOut = 3)

        val snapshot = mordie.snapshot()!!

        assertEquals(8, snapshot.moves)
        assertTrue(snapshot.found)
        assertFalse(snapshot.provenShortest)
        assertEquals(3, snapshot.ruledOut)
        assertContentEquals(path(8), snapshot.solution)
    }

    @Test
    fun `a proven snapshot says proven`() {
        mordie.restoreSolved(board, moves = 5, found = true, solution = path(5))

        assertTrue(mordie.snapshot()!!.provenShortest)
    }

    @Test
    fun `a one-move answer the search has not confirmed yet is saved as proven`() {
        mordie.startFresh(board)
        factory.latest.progress = SearchProgress(
            1, found = true, elapsedMs = 1, solution = path(1), ruledOut = -1, provenShortest = false, source = SolutionSource.FAST_SOLVER,
        )
        shifts(5)
        mordie.giveUp()

        val snapshot = mordie.snapshot()!!

        assertTrue(snapshot.provenShortest)
        assertEquals(0, snapshot.ruledOut)
    }
}
