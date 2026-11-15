package mordie

import testing.FakeMordieSearchFactory
import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-272: a game started from a link that carries the sender's progress opens with the friend's line. */
class MordieFriendLineTest {

    private val factory = FakeMordieSearchFactory()
    private val mordie = Mordie(factory)
    private val board = intArrayOf(1, 0, 2, 3, 4, 5, 6, 7, 8)
    private val friend = MordieLine.FriendSolving(12)

    private fun shifts(count: Int) = repeat(count) { mordie.onShift(0) }

    @Test
    fun `a fresh game with a friend opens with the friend's line`() {
        mordie.startFresh(board, friend)

        assertEquals(friend, mordie.line())
    }

    @Test
    fun `the friend's line stays through shift 4`() {
        mordie.startFresh(board, friend)

        for (shift in 1..4) {
            shifts(1)
            assertEquals(friend, mordie.line(), "after shift $shift")
        }
    }

    @Test
    fun `from shift 5 the normal rules apply`() {
        mordie.startFresh(board, friend)

        shifts(5)

        assertEquals(null, mordie.line())
    }

    @Test
    fun `each friend line shows`() {
        for (line in listOf(MordieLine.FriendSolved(3), MordieLine.FriendGaveUp(3), MordieLine.FriendGaveUp(null))) {
            mordie.startFresh(board, line)

            assertEquals(line, mordie.line())
        }
    }

    @Test
    fun `a fresh game with no friend opens as usual`() {
        mordie.startFresh(board)

        assertEquals(MordieLine.Opening, mordie.line())
    }

    @Test
    fun `the next fresh game forgets the friend`() {
        mordie.startFresh(board, friend)

        mordie.startFresh(board)

        assertEquals(MordieLine.Opening, mordie.line())
    }

    @Test
    fun `resuming a save forgets the friend`() {
        mordie.startFresh(board, friend)

        mordie.resume(board, moves = 4, found = false)

        assertEquals(MordieLine.Resumed(4), mordie.line())
    }

    @Test
    fun `clearing forgets the friend, and the next fresh game opens as usual`() {
        mordie.startFresh(board, friend)
        mordie.clear()

        mordie.startFresh(board)

        assertEquals(MordieLine.Opening, mordie.line())
    }

    @Test
    fun `a revealed solution replaces the friend's line`() {
        mordie.startFresh(board, friend)
        factory.latest.progress = SearchProgress(3, found = true, elapsedMs = 1)

        shifts(5)

        assertEquals(MordieLine.Found(3), mordie.line())
    }
}
