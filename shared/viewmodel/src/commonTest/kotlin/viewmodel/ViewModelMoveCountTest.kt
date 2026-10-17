package viewmodel

import kotlinx.coroutines.test.runTest
import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-62: the move count's path through the viewModel - query, save/load, and `board_solved`. */
class ViewModelMoveCountTest {

    /** 3x3, one `shiftLeft(0)` from solved. */
    private fun oneShiftFromSolved() = BoardImpl(3).apply { restoreState(intArrayOf(2, 0, 1, 3, 4, 5, 6, 7, 8)) }

    @Test
    fun `moveCount delegates to the board`() {
        val board = FakeBoardModel().apply { moveCount = 4 }

        assertEquals(4, testViewModel(board).moveCount())
    }

    @Test
    fun `saveGame persists the move count`() = runTest {
        val saves = FakeSaveRepository()
        val board = FakeBoardModel().apply { moveCount = 6 }
        val viewModel = testViewModel(board, saves)

        FakeViewModelUi().drive(viewModel) { viewModel.saveGame("foo") }

        assertEquals(listOf(6), saves.savedMoveCounts)
    }

    @Test
    fun `loadGame restores the saved move count`() = runTest {
        val saves = FakeSaveRepository()
        saves.save("foo", intArrayOf(8, 7, 6, 5, 4, 3, 2, 1, 0), 3, moveCount = 9)
        val board = BoardImpl(3)
        val viewModel = testViewModel(board, saves)

        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("foo") }

        assertEquals(9, viewModel.moveCount())
    }

    @Test
    fun `board_solved fires once when a shift solves the board, with the move count and size`() {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(oneShiftFromSolved(), FakeSaveRepository(), analytics)

        viewModel.shiftUp(2)
        viewModel.shiftDown(2)
        viewModel.shiftLeft(0)

        assertEquals(
            listOf(RecordingAnalyticsService.Event("board_solved", mapOf("move_count" to 2, "board_size" to 3))),
            analytics.events,
        )
    }

    @Test
    fun `board_solved does not fire for a shift that unsolves an already-solved board`() {
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3).apply { restoreState(IntArray(9) { it }) }
        val viewModel = testViewModel(board, FakeSaveRepository(), analytics)

        viewModel.shiftLeft(0)

        assertEquals(emptyList(), analytics.events)
    }

    @Test
    fun `board_solved does not fire when loading a solved save`() = runTest {
        val saves = FakeSaveRepository()
        saves.save("done", IntArray(9) { it }, 3, moveCount = 4)
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(oneShiftFromSolved(), saves, analytics)

        FakeViewModelUi().drive(viewModel) { viewModel.loadGame("done") }

        assertEquals(listOf("load_command_used"), analytics.events.map { it.name })
    }

    @Test
    fun `a rejected shift fires nothing and still throws`() {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(oneShiftFromSolved(), FakeSaveRepository(), analytics)

        assertFailsWith<IllegalArgumentException> { viewModel.shiftLeft(9) }

        assertEquals(emptyList(), analytics.events)
    }
}
