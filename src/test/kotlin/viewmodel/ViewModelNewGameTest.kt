package viewmodel

import testing.FakeBoardModel
import testing.RecordingAnalyticsService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Covers [ViewModelImpl.newGame] (GH-44): delegates to [board_model.BoardModel.newGame],
 *  then tracks `new_game_size_selected` - unless the board rejects the size, in which case
 *  nothing is tracked and the throw propagates unchanged (mirrors `shiftLeft`'s contract). */
class ViewModelNewGameTest {

    @Test
    fun `newGame delegates to board_newGame with the given size`() {
        val board = FakeBoardModel()
        val viewModel = ViewModelImpl(board, analytics = RecordingAnalyticsService())

        viewModel.newGame(5, trigger = "command")

        assertEquals(listOf("newGame(5)"), board.calls)
    }

    @Test
    fun `newGame tracks new_game_size_selected with the size and trigger`() {
        val board = FakeBoardModel()
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, analytics = analytics)

        viewModel.newGame(3, trigger = "toolbar")

        assertEquals(
            listOf(RecordingAnalyticsService.Event("new_game_size_selected", mapOf("size" to 3, "trigger" to "toolbar"))),
            analytics.events,
        )
    }

    @Test
    fun `trigger defaults to command`() {
        val board = FakeBoardModel()
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, analytics = analytics)

        viewModel.newGame(3)

        assertEquals(
            listOf(RecordingAnalyticsService.Event("new_game_size_selected", mapOf("size" to 3, "trigger" to "command"))),
            analytics.events,
        )
    }

    @Test
    fun `an out-of-range size throws and tracks nothing`() {
        val board = FakeBoardModel().apply { newGameException = IllegalArgumentException("Board size must be between 3 and 5, got 9") }
        val analytics = RecordingAnalyticsService()
        val viewModel = ViewModelImpl(board, analytics = analytics)

        assertFailsWith<IllegalArgumentException> { viewModel.newGame(9) }

        assertEquals(emptyList(), analytics.events)
    }
}
