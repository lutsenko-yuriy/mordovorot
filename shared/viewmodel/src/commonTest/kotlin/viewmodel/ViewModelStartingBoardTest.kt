package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import mordie.Mordie
import storage.SavedBoard
import testing.FakeMordieSearchFactory
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.testViewModel

/** GH-243: `startingBoard`, the board a share link names. */
class ViewModelStartingBoardTest {

    private val start = intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)

    @Test
    fun `it is the board Mordie started from, whatever the shifts since`() {
        val viewModel = testViewModel(BoardImpl(3), mordie = Mordie(FakeMordieSearchFactory()))
        viewModel.newGameFrom(start)
        viewModel.shiftLeft(0)
        viewModel.shiftUp(1)

        assertContentEquals(start, viewModel.startingBoard())
    }

    @Test
    fun `it is null when Mordie sits the game out - a save with no Mordie data and moves`() = runTest {
        val saves = FakeSaveRepository(mutableMapOf("old" to SavedBoard(3, start, moveCount = 4)))
        val viewModel = testViewModel(BoardImpl(3), saves, mordie = Mordie(FakeMordieSearchFactory()))

        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("old") }

        assertNull(viewModel.startingBoard())
    }
}
