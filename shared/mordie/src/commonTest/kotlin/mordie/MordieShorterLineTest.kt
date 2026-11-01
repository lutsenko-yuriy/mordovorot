package mordie

import testing.FakeMordieSearchFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** GH-223: the lines Mordie announces for an answer that may not be the shortest. */
class MordieShorterLineTest {

    private val factory = FakeMordieSearchFactory()
    private val mordie = Mordie(factory)
    private val board = intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8)

    private fun shifts(count: Int) = repeat(count) { mordie.onShift(0) }

    private fun move(amount: Int) = SolutionMove(isRow = true, index = 0, amount = amount)

    private fun fast(moves: Int) = SearchProgress(
        moves, found = true, elapsedMs = 5, solution = List(moves) { move(1) },
        ruledOut = 3, provenShortest = false, source = SolutionSource.FAST_SOLVER,
    )

    private fun exact(moves: Int) = SearchProgress(moves, found = true, elapsedMs = 5, solution = List(moves) { move(2) })

    private fun start() = mordie.startFresh(board)

    @Test
    fun `an unproven reveal returns Revealed and says the unproven line`() {
        start()
        factory.latest.progress = fast(8)
        shifts(4)

        val check = mordie.onShift(0)

        assertEquals(MordieCheck.Revealed(fast(8)), check)
        assertEquals(MordieLine.FoundUnproven(8), mordie.line())
    }

    @Test
    fun `a proven reveal says Found it`() {
        start()
        factory.latest.progress = exact(5)
        shifts(4)

        val check = mordie.onShift(0)

        assertEquals(MordieCheck.Revealed(exact(5)), check)
        assertEquals(MordieLine.Found(5), mordie.line())
    }

    @Test
    fun `a later check that sees a shorter answer announces it`() {
        start()
        factory.latest.progress = fast(8)
        shifts(5)
        factory.latest.progress = SearchProgress(6, found = true, elapsedMs = 5, solution = List(6) { move(2) }, ruledOut = 2, provenShortest = false)

        val check = shifts(4).let { mordie.onShift(0) }

        assertEquals(MordieCheck.Shortened(moves = 6, previous = 8, provenShortest = false), check)
        assertEquals(MordieLine.Shorter(6), mordie.line())
    }

    @Test
    fun `the drop is not announced before the next check`() {
        start()
        factory.latest.progress = fast(8)
        shifts(5)
        factory.latest.progress = exact(6)

        shifts(4)

        assertEquals(MordieLine.FoundUnproven(8), mordie.line())
    }

    @Test
    fun `a second drop is announced too`() {
        start()
        factory.latest.progress = fast(8)
        shifts(5)
        factory.latest.progress = SearchProgress(6, found = true, elapsedMs = 5, solution = List(6) { move(2) }, ruledOut = 2, provenShortest = false)
        shifts(5)

        factory.latest.progress = exact(4)
        val check = shifts(4).let { mordie.onShift(0) }

        assertEquals(MordieCheck.Shortened(moves = 4, previous = 6, provenShortest = true), check)
        assertEquals(MordieLine.Shorter(4), mordie.line())
    }

    @Test
    fun `becoming proven with no drop announces nothing`() {
        start()
        factory.latest.progress = fast(8)
        shifts(5)
        factory.latest.progress = exact(8)

        val check = shifts(4).let { mordie.onShift(0) }

        assertNull(check)
        assertTrue(mordie.provenShortest)
        assertEquals(MordieLine.FoundUnproven(8), mordie.line())
    }

    @Test
    fun `a check with the same unproven answer announces nothing`() {
        start()
        factory.latest.progress = fast(8)
        shifts(5)

        assertNull(shifts(4).let { mordie.onShift(0) })
        assertEquals(MordieLine.FoundUnproven(8), mordie.line())
    }

    @Test
    fun `giving up and solving take a shorter best without announcing it`() {
        start()
        factory.latest.progress = fast(8)
        shifts(5)
        factory.latest.progress = exact(6)

        mordie.giveUp()

        assertEquals(6, mordie.solution?.size)
        assertEquals(MordieLine.FoundUnproven(8), mordie.line())
    }

    @Test
    fun `revealed is true under every found line`() {
        start()
        assertFalse(mordie.revealed)
        factory.latest.progress = fast(8)
        shifts(5)
        assertTrue(mordie.revealed)
        factory.latest.progress = SearchProgress(6, found = true, elapsedMs = 5, solution = List(6) { move(2) }, ruledOut = 2, provenShortest = false)
        shifts(5)
        assertTrue(mordie.revealed)
    }

    @Test
    fun `a save that was found resumes with Found it`() {
        mordie.resume(board, moves = 7, found = true)

        assertEquals(MordieLine.Found(7), mordie.line())
        assertTrue(mordie.revealed)
    }

    @Test
    fun `the result is beat_mordie below an unproven N and machine below a proven one`() {
        val unproven = MordieOutcome(40, found = true, provenShortest = false)
        val proven = MordieOutcome(12, found = true, provenShortest = true)

        assertEquals(MordieResult.BEAT_MORDIE, unproven.result(30))
        assertEquals(MordieResult.MATCHED, unproven.result(40))
        assertEquals(MordieResult.MORDIE_WON, unproven.result(41))
        assertEquals(MordieResult.MACHINE, proven.result(11))
        assertEquals(MordieResult.PLAYER_WON, MordieOutcome(9, found = false).result(9))
        assertEquals(MordieResult.MACHINE, MordieOutcome(9, found = false).result(8))
    }
}
