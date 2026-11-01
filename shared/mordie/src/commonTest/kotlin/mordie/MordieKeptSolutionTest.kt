package mordie

import testing.FakeMordieSearchFactory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        mordie.resume(board, moves = 8, found = true, solution = path(8), provenShortest = false, ruledOut = 3)

        assertFalse(mordie.provenShortest)
        assertTrue(mordie.revealed)
        assertEquals(MordieLine.FoundUnproven(8), mordie.line())
        assertTrue(factory.started.isEmpty())
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
}
