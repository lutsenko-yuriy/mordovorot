package mordie

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import testing.FakeMordieSearchFactory

/** GH-349: `restart()`, the game going back to its start with Mordie keeping his search. */
class MordieRestartTest {

    private val factory = FakeMordieSearchFactory()
    private val mordie = Mordie(factory)
    private val board = intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8)
    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1))

    private fun shifts(count: Int) = repeat(count) { mordie.onShift(0) }
    private fun found(moves: Int = 1, proven: Boolean = true, ruledOut: Int = 0) =
        SearchProgress(moves, found = true, elapsedMs = 0, solution = path, provenShortest = proven, ruledOut = ruledOut)

    @Test
    fun `a game Mordie sits out, or none yet, is left alone`() {
        mordie.restart()
        assertNull(mordie.line())

        mordie.clear()
        shifts(3)
        mordie.restart()

        assertEquals(0, mordie.shifts)
        assertNull(mordie.line())
        assertEquals(0, factory.started.size)
    }

    @Test
    fun `mid-play the shift count starts again but the search is kept running`() {
        mordie.startFresh(board)
        shifts(7)

        mordie.restart()

        assertEquals(0, mordie.shifts)
        assertEquals(MordieLine.Opening, mordie.line())
        assertEquals(1, factory.started.size)
        assertFalse(factory.latest.cancelled)
    }

    @Test
    fun `the game's own opening line comes back, a friend's included`() {
        mordie.startFresh(board, MordieLine.FriendSolving(12))
        shifts(5)

        mordie.restart()

        assertEquals(MordieLine.FriendSolving(12), mordie.line())
    }

    @Test
    fun `a found solution is kept, shown again from the next check, and its reveal markers are gone`() {
        mordie.startFresh(board)
        factory.latest.progress = found()
        shifts(5)
        assertTrue(mordie.revealed)

        mordie.restart()

        assertTrue(mordie.revealed)
        assertEquals(path, mordie.solution)
        assertNull(mordie.shiftsAtReveal)
        assertNull(mordie.playerMovesAtReveal)
        assertEquals(MordieLine.Opening, mordie.line())
        shifts(4)
        assertEquals(MordieLine.Opening, mordie.line())
        shifts(1)
        assertEquals(MordieLine.Found(1), mordie.line())
    }

    @Test
    fun `a restart never binds Mordie again`() {
        mordie.startFresh(board)
        shifts(2)

        mordie.restart()
        mordie.ensureStarted(board)

        assertEquals(1, factory.started.size)
    }

    @Test
    fun `from a won board with a proven answer his result unfreezes and the answer shows again`() {
        mordie.startFresh(board)
        factory.latest.progress = found()
        mordie.onSolved()
        assertTrue(mordie.outcome() != null)

        mordie.restart()

        assertNull(mordie.outcome())
        assertTrue(mordie.revealed)
        assertTrue(mordie.provenShortest)
        assertEquals(MordieLine.Opening, mordie.line())
        assertEquals(1, factory.started.size)
        shifts(5)
        assertEquals(MordieLine.Found(1), mordie.line())
    }

    @Test
    fun `from a won board with an unproven answer the exact search goes on above what it ruled out`() {
        mordie.startFresh(board)
        factory.latest.progress = found(moves = 4, proven = false, ruledOut = 2)
        mordie.onSolved()

        mordie.restart()

        assertEquals(2, factory.started.size)
        assertEquals(2, factory.latest.ruledOut)
        assertEquals(path, factory.latest.best)
        assertFalse(mordie.provenShortest)
        shifts(5)
        assertEquals(MordieLine.FoundUnproven(4), mordie.line())
    }

    @Test
    fun `from a won board with no answer he searches on from what he had ruled out`() {
        mordie.startFresh(board)
        factory.latest.progress = SearchProgress(3, found = false, elapsedMs = 0)
        mordie.onSolved()

        mordie.restart()

        assertNull(mordie.outcome())
        assertFalse(mordie.revealed)
        assertEquals(2, factory.started.size)
        assertEquals(3, factory.latest.ruledOut)
        assertEquals(MordieLine.Opening, mordie.line())
    }
}
