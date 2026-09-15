package presenter

import testing.FakeBoardModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PresenterDelegationTest {

    @Test
    fun `shiftLeft delegates to the board`() {
        val board = FakeBoardModel()
        val presenter = PresenterImpl(board)

        presenter.shiftLeft(2)

        assertEquals(listOf("shiftLeft(2)"), board.calls)
    }

    @Test
    fun `shiftRight delegates to the board`() {
        val board = FakeBoardModel()
        val presenter = PresenterImpl(board)

        presenter.shiftRight(1)

        assertEquals(listOf("shiftRight(1)"), board.calls)
    }

    @Test
    fun `shiftUp delegates to the board`() {
        val board = FakeBoardModel()
        val presenter = PresenterImpl(board)

        presenter.shiftUp(3)

        assertEquals(listOf("shiftUp(3)"), board.calls)
    }

    @Test
    fun `shiftDown delegates to the board`() {
        val board = FakeBoardModel()
        val presenter = PresenterImpl(board)

        presenter.shiftDown(0)

        assertEquals(listOf("shiftDown(0)"), board.calls)
    }

    @Test
    fun `resetGame delegates to the board`() {
        val board = FakeBoardModel()
        val presenter = PresenterImpl(board)

        presenter.resetGame()

        assertEquals(listOf("resetGame"), board.calls)
    }

    @Test
    fun `exceptions from the board propagate unchanged`() {
        val board = FakeBoardModel()
        board.shiftLeftException = IllegalArgumentException("Incorrect row")
        val presenter = PresenterImpl(board)

        val exception = assertFailsWith<IllegalArgumentException> { presenter.shiftLeft(4) }

        assertEquals("Incorrect row", exception.message)
    }
}
