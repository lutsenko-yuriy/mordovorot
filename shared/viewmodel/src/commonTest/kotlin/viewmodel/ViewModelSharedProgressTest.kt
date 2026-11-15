package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import mordie.Mordie
import testing.FakeMordieSearchFactory
import testing.testViewModel

/** GH-272: `newGameFrom` with the sender's progress opens Mordie with the friend's line; `sharedProgress()` is
 *  the progress a share link can carry. */
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

    @Test
    fun `at 0 moves there is no progress to share`() {
        viewModel.newGameFrom(shared)

        assertNull(viewModel.sharedProgress())
    }

    @Test
    fun `after moves the game is solving`() {
        viewModel.newGameFrom(shared)
        viewModel.shiftLeft(1)
        viewModel.shiftLeft(2)

        assertEquals(SharedProgress.Solving(2), viewModel.sharedProgress())
    }

    @Test
    fun `a solved board is solved in its move count`() {
        viewModel.newGameFrom(intArrayOf(2, 0, 1, 3, 4, 5, 6, 7, 8))
        viewModel.shiftLeft(0)

        assertEquals(SharedProgress.Solved(1), viewModel.sharedProgress())
    }

    @Test
    fun `on the solution screen the result decides`() {
        val screen = SolutionScreen()
        val vm = testViewModel(BoardImpl(3), mordie = Mordie(FakeMordieSearchFactory()), solutionScreen = screen)
        vm.newGameFrom(shared)
        screen.replay = SolutionReplay(shared, 3, listOf(SolutionStep(isRow = true, index = 0, forward = true, count = 1)))

        screen.result = PlayerResult(gaveUp = true, moves = 3)
        assertEquals(SharedProgress.GaveUp(3), vm.sharedProgress())

        screen.result = PlayerResult(gaveUp = false, moves = 2)
        assertEquals(SharedProgress.Solved(2), vm.sharedProgress())

        screen.result = null
        assertEquals(SharedProgress.GaveUp(null), vm.sharedProgress())

        screen.result = PlayerResult(gaveUp = true, moves = 0)
        assertNull(vm.sharedProgress())
    }
}
