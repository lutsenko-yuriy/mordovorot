package viewmodel

import testing.FakeBoardModel
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Covers [ViewModelImpl.offerPlayAgain] (GH-66) directly, without a play() loop - the console's
 *  solved-state flow: Congratulations message, `Start a new game?`, then the size prompt and a
 *  fresh game, or [ExitRequestedException] on decline. */
class ViewModelPlayAgainTest {

    private fun solvedBoard(moves: Int) = FakeBoardModel().apply {
        correct = true
        moveCount = moves
    }

    @Test
    fun `shows the congratulations message with the move count before asking`(): Unit = runBlocking {
        val ui = FakeViewModelUi(confirmPlayAgainResponses = mutableListOf(false))
        val viewModel = ViewModelImpl(solvedBoard(12))

        assertFailsWith<ExitRequestedException> { ui.drive(viewModel) { viewModel.offerPlayAgain() } }

        assertEquals(listOf("Congratulations, you solved this puzzle in 12 moves!"), ui.shownMessages)
        assertEquals(1, ui.confirmPlayAgainCallCount)
    }

    @Test
    fun `uses the singular for one move and the plural for zero`(): Unit = runBlocking {
        for ((moves, expected) in listOf(1 to "in 1 move!", 0 to "in 0 moves!")) {
            val ui = FakeViewModelUi(confirmPlayAgainResponses = mutableListOf(false))
            val viewModel = ViewModelImpl(solvedBoard(moves))

            assertFailsWith<ExitRequestedException> { ui.drive(viewModel) { viewModel.offerPlayAgain() } }

            assertEquals("Congratulations, you solved this puzzle $expected", ui.shownMessages.single())
        }
    }

    @Test
    fun `declining throws ExitRequestedException, tracks quit and never asks for a size`(): Unit = runBlocking {
        val board = solvedBoard(3)
        val analytics = RecordingAnalyticsService()
        val ui = FakeViewModelUi(confirmPlayAgainResponses = mutableListOf(false))
        val viewModel = ViewModelImpl(board, analytics = analytics)

        assertFailsWith<ExitRequestedException> { ui.drive(viewModel) { viewModel.offerPlayAgain() } }

        assertEquals(emptyList(), ui.chooseBoardSizeCalls)
        assertEquals(emptyList(), board.calls)
        assertEquals(listOf(Event("play_again_decision", mapOf("decision" to "quit"))), analytics.events)
    }

    @Test
    fun `accepting asks for a size and starts a new game at it`(): Unit = runBlocking {
        val board = solvedBoard(3)
        val analytics = RecordingAnalyticsService()
        val ui = FakeViewModelUi(
            confirmPlayAgainResponses = mutableListOf(true),
            chooseBoardSizeResponses = mutableListOf(3),
        )
        val viewModel = ViewModelImpl(board, analytics = analytics)

        ui.drive(viewModel) { viewModel.offerPlayAgain() }

        assertEquals(listOf(4), ui.chooseBoardSizeCalls)
        assertEquals(listOf("newGame(3)"), board.calls)
        assertEquals(
            listOf(
                Event("play_again_decision", mapOf("decision" to "new_game")),
                Event("new_game_size_selected", mapOf("size" to 3, "trigger" to "play_again")),
            ),
            analytics.events,
        )
    }

    @Test
    fun `accepting then pressing Enter at the size prompt starts a new game at the current size`(): Unit = runBlocking {
        val board = solvedBoard(3)
        val analytics = RecordingAnalyticsService()
        val ui = FakeViewModelUi(
            confirmPlayAgainResponses = mutableListOf(true),
            chooseBoardSizeResponses = mutableListOf(null),
        )
        val viewModel = ViewModelImpl(board, analytics = analytics)

        ui.drive(viewModel) { viewModel.offerPlayAgain() }

        assertEquals(listOf("newGame(4)"), board.calls)
        assertEquals(Event("new_game_size_selected", mapOf("size" to 4, "trigger" to "play_again")), analytics.events.last())
    }
}
