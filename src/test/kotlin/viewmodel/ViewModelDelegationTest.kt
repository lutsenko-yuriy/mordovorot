package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import testing.FakeBoardModel
import testing.testViewModel

class ViewModelDelegationTest {

    @Test
    fun `shiftLeft delegates to the board`() {
        val board = FakeBoardModel()
        val viewModel = testViewModel(board)

        viewModel.shiftLeft(2)

        assertEquals(listOf("shiftLeft(2)"), board.calls)
    }

    @Test
    fun `shiftRight delegates to the board`() {
        val board = FakeBoardModel()
        val viewModel = testViewModel(board)

        viewModel.shiftRight(1)

        assertEquals(listOf("shiftRight(1)"), board.calls)
    }

    @Test
    fun `shiftUp delegates to the board`() {
        val board = FakeBoardModel()
        val viewModel = testViewModel(board)

        viewModel.shiftUp(3)

        assertEquals(listOf("shiftUp(3)"), board.calls)
    }

    @Test
    fun `shiftDown delegates to the board`() {
        val board = FakeBoardModel()
        val viewModel = testViewModel(board)

        viewModel.shiftDown(0)

        assertEquals(listOf("shiftDown(0)"), board.calls)
    }

    @Test
    fun `exceptions from the board propagate unchanged`() {
        val board = FakeBoardModel()
        board.shiftLeftException = IllegalArgumentException("Incorrect row")
        val viewModel = testViewModel(board)

        val exception = assertFailsWith<IllegalArgumentException> { viewModel.shiftLeft(4) }

        assertEquals("Incorrect row", exception.message)
    }
}
