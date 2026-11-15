package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import mordie.Mordie
import testing.FakeMordieSearchFactory
import testing.testViewModel

/** GH-272: `newGameFrom` with the sender's progress opens Mordie with the friend's line. */
class ViewModelSharedProgressTest {

    private val shared = intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)
    private val viewModel = testViewModel(BoardImpl(3), mordie = Mordie(FakeMordieSearchFactory()))

    @Test
    fun `a game from a link with progress opens with the friend's line`() {
        viewModel.newGameFrom(shared, SharedProgress.Solving(12))

        assertEquals("Okay, so your friend made 12 moves and did not solve the puzzle yet. Maybe we could help?", viewModel.mordieLine())
    }

    @Test
    fun `each state has its own line`() {
        viewModel.newGameFrom(shared, SharedProgress.Solved(2))
        assertEquals("Okay, so your friend solved this one in 2 moves. Wanna beat this?", viewModel.mordieLine())

        viewModel.newGameFrom(shared, SharedProgress.GaveUp(1))
        assertEquals("So after making 1 move your friend gave up before finding the solution. Can we find one?", viewModel.mordieLine())

        viewModel.newGameFrom(shared, SharedProgress.GaveUp(null))
        assertEquals("So your friend gave up before finding the solution. Can we find one?", viewModel.mordieLine())
    }

    @Test
    fun `a game from a link without progress opens as usual`() {
        viewModel.newGameFrom(shared, null)

        assertEquals("Alright! Let's sort this one out!", viewModel.mordieLine())
    }

    @Test
    fun `the friend's line is that game's only`() {
        viewModel.newGameFrom(shared, SharedProgress.Solving(12))

        viewModel.newGame(3, "toolbar")

        assertEquals("Alright! Let's sort this one out!", viewModel.mordieLine())
    }

    @Test
    fun `the analytics names`() {
        assertEquals("solving", SharedProgress.Solving(1).analyticsName)
        assertEquals("solved", SharedProgress.Solved(1).analyticsName)
        assertEquals("gave_up", SharedProgress.GaveUp(1).analyticsName)
        assertEquals("gave_up", SharedProgress.GaveUp(null).analyticsName)
    }
}
