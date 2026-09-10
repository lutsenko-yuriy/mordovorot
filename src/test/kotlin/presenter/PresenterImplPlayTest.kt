package presenter

import testing.FakeBoardModel
import testing.FakeView
import kotlin.test.Test
import kotlin.test.assertEquals

class PresenterImplPlayTest {

    @Test
    fun `play returns immediately when the board is already correct`() {
        val board = FakeBoardModel().apply { correct = true }
        val view = FakeView()
        val presenter = PresenterImpl(view, board)

        presenter.play()

        assertEquals(0, view.displayBoardCalls.size)
        assertEquals(0, view.processCommandCallCount)
    }

    @Test
    fun `play displays the board and processes one command before the board is solved`() {
        val board = FakeBoardModel()
        val view = FakeView(mutableListOf({ board.correct = true }))
        val presenter = PresenterImpl(view, board)

        presenter.play()

        assertEquals(1, view.displayBoardCalls.size)
        assertEquals(1, view.processCommandCallCount)
        assertEquals(board.SQUARE_SIDE, view.displayBoardCalls[0].second)
        assertEquals(board.boardArray.toList(), view.displayBoardCalls[0].first.toList())
    }

    @Test
    fun `play returns without looping when processCommand throws EOFException`() {
        val board = FakeBoardModel()
        val view = FakeView(mutableListOf({ throw java.io.EOFException() }))
        val presenter = PresenterImpl(view, board)

        presenter.play()

        assertEquals(1, view.displayBoardCalls.size)
        assertEquals(1, view.processCommandCallCount)
    }

    @Test
    fun `play swallows processCommand exceptions and keeps looping`() {
        val board = FakeBoardModel()
        val view = FakeView(
            mutableListOf(
                { throw RuntimeException("boom") },
                { board.correct = true },
            )
        )
        val presenter = PresenterImpl(view, board)

        presenter.play()

        assertEquals(2, view.displayBoardCalls.size)
        assertEquals(2, view.processCommandCallCount)
    }
}
