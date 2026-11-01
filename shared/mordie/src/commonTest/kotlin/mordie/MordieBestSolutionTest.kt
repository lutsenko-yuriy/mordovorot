package mordie

import testing.FakeMordieSearchFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** GH-222: Mordie keeps the best solution so far, from the fast solver and the exact search. */
class MordieBestSolutionTest {

    private val factory = FakeMordieSearchFactory()
    private val mordie = Mordie(factory)
    private val board = intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8)

    private fun shifts(count: Int) = repeat(count) { mordie.onShift(0) }

    private fun move(amount: Int) = SolutionMove(isRow = true, index = 0, amount = amount)

    private fun fast(moves: Int, ruledOut: Int = 3) = SearchProgress(
        moves, found = true, elapsedMs = 5, solution = List(moves) { move(1) },
        ruledOut = ruledOut, provenShortest = false, source = SolutionSource.FAST_SOLVER,
    )

    private fun exact(moves: Int) = SearchProgress(moves, found = true, elapsedMs = 5, solution = List(moves) { move(2) })

    private fun start() = mordie.startFresh(board)

    @Test
    fun `the first check that sees a fast answer reveals it, unproven`() {
        start()
        factory.latest.progress = fast(8)

        shifts(4)
        val revealed = mordie.onShift(0)

        assertEquals(fast(8), revealed)
        assertEquals(MordieLine.Found(8), mordie.line())
        assertFalse(mordie.provenShortest)
    }

    @Test
    fun `a later check updates N and the solution when the exact search finds a shorter one`() {
        start()
        factory.latest.progress = fast(8)
        shifts(5)

        factory.latest.progress = exact(6)
        shifts(5)

        assertEquals(MordieLine.Found(6), mordie.line())
        assertEquals(6, mordie.solution?.size)
        assertTrue(mordie.provenShortest)
    }

    @Test
    fun `a later check that sees no change keeps the revealed answer`() {
        start()
        factory.latest.progress = fast(8)
        shifts(10)

        assertEquals(MordieLine.Found(8), mordie.line())
        assertEquals(8, mordie.solution?.size)
        assertFalse(mordie.provenShortest)
    }

    @Test
    fun `a later reveal returns nothing, so mordie_solution_found fires once`() {
        start()
        factory.latest.progress = fast(8)
        shifts(5)

        factory.latest.progress = exact(6)
        shifts(4)

        assertNull(mordie.onShift(0))
    }

    @Test
    fun `checks stop once proven`() {
        start()
        factory.latest.progress = exact(4)
        shifts(5)
        factory.latest.progress = exact(2)

        shifts(5)

        assertEquals(MordieLine.Found(4), mordie.line())
    }

    @Test
    fun `giveUp takes the live best, not the last check's, and stops the search`() {
        start()
        factory.latest.progress = fast(8)
        shifts(5)
        factory.latest.progress = exact(6)

        mordie.giveUp()

        assertEquals(6, mordie.solution?.size)
        assertTrue(mordie.provenShortest)
        assertTrue(factory.latest.cancelled)
    }

    @Test
    fun `solving takes the live best and its proven flag`() {
        start()
        factory.latest.progress = fast(8)
        shifts(5)
        factory.latest.progress = fast(7, ruledOut = 4)

        val outcome = mordie.onSolved()!!

        assertEquals(MordieOutcome(7, found = true, provenShortest = false), outcome)
        assertEquals(7, mordie.solution?.size)
    }

    @Test
    fun `solving before any check reads the live fast answer`() {
        start()
        factory.latest.progress = fast(8)

        val outcome = mordie.onSolved()!!

        assertEquals(8, outcome.moves)
        assertTrue(outcome.found)
        assertFalse(outcome.provenShortest)
    }

    @Test
    fun `a proven snapshot is stored as found with its solution`() {
        start()
        factory.latest.progress = exact(4)
        shifts(5)

        val snapshot = mordie.snapshot()!!

        assertTrue(snapshot.found)
        assertEquals(4, snapshot.moves)
        assertEquals(4, snapshot.solution?.size)
    }

    @Test
    fun `an unproven snapshot is stored as searching at the ruled out length`() {
        start()
        factory.latest.progress = fast(8, ruledOut = 5)
        shifts(5)

        val snapshot = mordie.snapshot()!!

        assertFalse(snapshot.found)
        assertEquals(5, snapshot.moves)
        assertNull(snapshot.solution)
    }

    @Test
    fun `after giving up, the snapshot stores the shown solution as found`() {
        start()
        factory.latest.progress = fast(8, ruledOut = 5)
        shifts(5)
        mordie.giveUp()

        val snapshot = mordie.snapshot()!!

        assertTrue(snapshot.found)
        assertEquals(8, snapshot.moves)
        assertEquals(8, snapshot.solution?.size)
        assertFalse(mordie.provenShortest)
    }

    @Test
    fun `a solved snapshot stores the best as found`() {
        start()
        factory.latest.progress = fast(8)
        shifts(5)
        mordie.onSolved()

        val snapshot = mordie.snapshot()!!

        assertTrue(snapshot.found)
        assertEquals(8, snapshot.moves)
    }

    @Test
    fun `a resumed found save counts as proven`() {
        mordie.resume(board, moves = 4, found = true, solution = List(4) { move(1) })

        assertTrue(mordie.provenShortest)
        assertNotNull(mordie.snapshot())
    }

    @Test
    fun `the outcome's proven flag defaults to found`() {
        assertTrue(MordieOutcome(3, found = true).provenShortest)
        assertFalse(MordieOutcome(3, found = false).provenShortest)
    }
}
