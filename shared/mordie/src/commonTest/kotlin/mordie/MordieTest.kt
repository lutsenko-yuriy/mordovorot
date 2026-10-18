package mordie

import testing.FakeMordieSearchFactory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MordieTest {

    private val factory = FakeMordieSearchFactory()
    private val mordie = Mordie(factory)
    private val board = intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8)

    private fun shifts(count: Int) = repeat(count) { mordie.onShift(0) }

    private val path = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 2, amount = 2))

    private fun found(moves: Int, solution: List<SolutionMove>? = null) = SearchProgress(moves, found = true, elapsedMs = 1234, solution)
    private fun searching(moves: Int) = SearchProgress(moves, found = false, elapsedMs = 0)

    // --- binding ---

    @Test
    fun `an unbound Mordie has no line, outcome or snapshot and ignores shifts`() {
        assertNull(mordie.onShift(0))
        assertNull(mordie.line())
        assertNull(mordie.onSolved())
        assertNull(mordie.outcome())
        assertNull(mordie.snapshot())
        assertEquals(0, mordie.shifts)
    }

    @Test
    fun `startFresh starts a fresh search of a copy of the arrangement`() {
        mordie.startFresh(board)
        board[0] = 99

        assertEquals(1, factory.started.size)
        assertContentEquals(intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8), factory.latest.original)
        assertEquals(-1, factory.latest.ruledOut)
        assertFalse(mordie.resumedFromSave)
    }

    @Test
    fun `startFresh cancels the previous search and restarts the shift count`() {
        mordie.startFresh(board)
        val first = factory.latest
        shifts(3)

        mordie.startFresh(board)

        assertTrue(first.cancelled)
        assertEquals(2, factory.started.size)
        assertEquals(0, mordie.shifts)
        assertEquals(MordieLine.Opening, mordie.line())
    }

    @Test
    fun `ensureStarted starts a fresh search only when nothing is bound yet`() {
        mordie.ensureStarted(board)
        mordie.ensureStarted(board)

        assertEquals(1, factory.started.size)
    }

    @Test
    fun `ensureStarted keeps a cleared Mordie cleared`() {
        mordie.clear()

        mordie.ensureStarted(board)

        assertEquals(0, factory.started.size)
        assertNull(mordie.line())
    }

    @Test
    fun `resume while searching restarts the search above the saved N`() {
        mordie.startFresh(board)
        val first = factory.latest

        mordie.resume(board, moves = 8, found = false)

        assertTrue(first.cancelled)
        assertEquals(8, factory.latest.ruledOut)
        assertContentEquals(board, factory.latest.original)
        assertEquals(MordieLine.Resumed(8), mordie.line())
        assertTrue(mordie.resumedFromSave)
    }

    @Test
    fun `resume of a found save reveals Found at once and starts no search`() {
        mordie.resume(board, moves = 11, found = true)

        assertEquals(0, factory.started.size)
        assertEquals(MordieLine.Found(11), mordie.line())
        shifts(5)
        assertEquals(MordieLine.Found(11), mordie.line())
    }

    @Test
    fun `restoreSolved freezes the saved result with no search and no line`() {
        mordie.startFresh(board)
        val first = factory.latest

        mordie.restoreSolved(board, moves = 3, found = true)

        assertTrue(first.cancelled)
        assertEquals(1, factory.started.size)
        assertNull(mordie.line())
        assertEquals(MordieOutcome(3, found = true), mordie.outcome())
        assertEquals(MordieOutcome(3, found = true), mordie.onSolved())
    }

    @Test
    fun `clear cancels the search and unbinds the line, outcome and snapshot`() {
        mordie.startFresh(board)
        val first = factory.latest

        mordie.clear()

        assertTrue(first.cancelled)
        assertNull(mordie.line())
        assertNull(mordie.onSolved())
        assertNull(mordie.snapshot())
    }

    // --- the line by shift count ---

    @Test
    fun `the greeting shows for shifts 0-4 and is gone at 5`() {
        mordie.startFresh(board)
        assertEquals(MordieLine.Opening, mordie.line())
        shifts(4)
        assertEquals(MordieLine.Opening, mordie.line())
        shifts(1)
        assertNull(mordie.line())
    }

    @Test
    fun `the resumed opening also lasts for shifts 0-4`() {
        mordie.resume(board, moves = 8, found = false)
        shifts(4)
        assertEquals(MordieLine.Resumed(8), mordie.line())
        shifts(1)
        assertNull(mordie.line())
    }

    @Test
    fun `progress shows at shifts 20-24 with the N from the shift-20 check`() {
        mordie.startFresh(board)
        factory.latest.progress = searching(6)
        shifts(19)
        assertNull(mordie.line())

        shifts(1)
        factory.latest.progress = searching(7)
        assertEquals(MordieLine.Progress(6, hard = false), mordie.line())
        shifts(4)
        assertEquals(MordieLine.Progress(6, hard = false), mordie.line())
        shifts(1)
        assertNull(mordie.line())
    }

    @Test
    fun `progress repeats every 20 shifts and is hard from shift 60`() {
        mordie.startFresh(board)
        factory.latest.progress = searching(7)
        shifts(20)
        factory.latest.progress = searching(8)
        shifts(20)
        assertEquals(MordieLine.Progress(8, hard = false), mordie.line())

        factory.latest.progress = searching(9)
        shifts(19)
        assertNull(mordie.line())
        shifts(1)
        assertEquals(MordieLine.Progress(9, hard = true), mordie.line())
    }

    @Test
    fun `progress is skipped when N hasn't changed since Mordie last said it`() {
        mordie.startFresh(board)
        factory.latest.progress = searching(6)
        shifts(20)
        assertEquals(MordieLine.Progress(6, hard = false), mordie.line())

        shifts(20)
        assertNull(mordie.line()) // still 6 at shift 40
        shifts(4)
        assertNull(mordie.line())

        factory.latest.progress = searching(7)
        shifts(16)
        assertEquals(MordieLine.Progress(7, hard = true), mordie.line())
    }

    @Test
    fun `a fresh search still at 0 moves says nothing at shift 20`() {
        mordie.startFresh(board)
        factory.latest.progress = searching(0)
        shifts(20)

        assertNull(mordie.line())
    }

    @Test
    fun `a resumed search still at the saved N says nothing at shift 20`() {
        mordie.resume(board, moves = 8, found = false)
        factory.latest.progress = searching(8)
        shifts(20)
        assertNull(mordie.line())

        factory.latest.progress = searching(9)
        shifts(20)
        assertEquals(MordieLine.Progress(9, hard = false), mordie.line())
    }

    @Test
    fun `found is revealed only at a multiple of 5 and then stays`() {
        mordie.startFresh(board)
        shifts(7)
        factory.latest.progress = found(11)

        assertNull(mordie.onShift(0))
        assertNull(mordie.onShift(0))
        assertNull(mordie.line())

        assertEquals(found(11), mordie.onShift(0))
        assertEquals(MordieLine.Found(11), mordie.line())
        repeat(20) { assertNull(mordie.onShift(0)) }
        assertEquals(MordieLine.Found(11), mordie.line())
    }

    @Test
    fun `onShift counts shifts`() {
        mordie.startFresh(board)
        shifts(7)

        assertEquals(7, mordie.shifts)
    }

    // --- solving ---

    @Test
    fun `onSolved uses live progress, cancels the search and freezes the outcome`() {
        mordie.startFresh(board)
        factory.latest.progress = searching(4)
        shifts(5)
        factory.latest.progress = found(6)

        assertEquals(MordieOutcome(6, found = true), mordie.onSolved())
        assertTrue(factory.latest.cancelled)
        assertNull(mordie.line())
        factory.latest.progress = found(9)
        assertEquals(MordieOutcome(6, found = true), mordie.outcome())
        assertEquals(MordieOutcome(6, found = true), mordie.onSolved())
    }

    @Test
    fun `onSolved after a found resume reports the saved N`() {
        mordie.resume(board, moves = 5, found = true)

        assertEquals(MordieOutcome(5, found = true), mordie.onSolved())
    }

    @Test
    fun `a fresh search below 0 moves reports 0`() {
        mordie.startFresh(board)

        assertEquals(MordieOutcome(0, found = false), mordie.onSolved())
    }

    // --- snapshot and stop ---

    @Test
    fun `snapshot holds the original and the live progress`() {
        mordie.startFresh(board)
        shifts(2)
        factory.latest.progress = searching(8)

        val snapshot = mordie.snapshot()!!

        assertContentEquals(board, snapshot.original)
        assertEquals(8, snapshot.moves)
        assertFalse(snapshot.found)
    }

    @Test
    fun `snapshot of a solved game holds its outcome`() {
        mordie.restoreSolved(board, moves = 3, found = true)

        val snapshot = mordie.snapshot()!!

        assertEquals(3, snapshot.moves)
        assertTrue(snapshot.found)
    }

    @Test
    fun `stop cancels the search and keeps the state`() {
        mordie.startFresh(board)
        factory.latest.progress = searching(4)

        mordie.stop()

        assertTrue(factory.latest.cancelled)
        assertEquals(MordieLine.Opening, mordie.line())
        assertEquals(4, mordie.snapshot()!!.moves)
    }

    // --- the solution path (GH-126) ---

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

    // --- outcome classification ---

    @Test
    fun `result classifies the player's move count against Mordie's N`() {
        assertEquals(MordieResult.PLAYER_WON, MordieOutcome(5, found = false).result(7))
        assertEquals(MordieResult.PLAYER_WON, MordieOutcome(5, found = false).result(5))
        assertEquals(MordieResult.MATCHED, MordieOutcome(5, found = true).result(5))
        assertEquals(MordieResult.MORDIE_WON, MordieOutcome(5, found = true).result(6))
        assertEquals(MordieResult.MACHINE, MordieOutcome(5, found = true).result(4))
        assertEquals(MordieResult.MACHINE, MordieOutcome(5, found = false).result(4))
    }
}
