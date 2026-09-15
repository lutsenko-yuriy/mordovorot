package viewmodel

import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakeSaveRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers [ViewModelImpl]'s additive, read-only query methods (`listSaves`, `saveExists`,
 * `isSolved`) that the mouse-driven TUI needs and the console UI never had to ask for
 * (GH-3). All three degrade to a safe default instead of throwing, mirroring the
 * non-throwing discipline [ViewModelImpl.saveGame]/[ViewModelImpl.loadGame] already follow.
 */
class ViewModelQueriesTest {

    @Test
    fun `listSaves delegates to the save repository`() {
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, IntArray(16) { it }),
                "bar" to SavedBoard(4, IntArray(16) { it }),
            ),
        )
        val viewModel = ViewModelImpl(FakeBoardModel(), saves)

        assertEquals(listOf("bar", "foo"), viewModel.listSaves())
    }

    @Test
    fun `listSaves degrades to an empty list on a repository failure`() {
        val saves = FakeSaveRepository()
        saves.listSavesException = RuntimeException("unreadable saves dir")
        val viewModel = ViewModelImpl(FakeBoardModel(), saves)

        assertEquals(emptyList(), viewModel.listSaves())
    }

    @Test
    fun `saveExists delegates to the save repository`() {
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, IntArray(16) { it })))
        val viewModel = ViewModelImpl(FakeBoardModel(), saves)

        assertTrue(viewModel.saveExists("foo"))
        assertFalse(viewModel.saveExists("bar"))
    }

    @Test
    fun `saveExists degrades to false on a repository failure`() {
        val saves = FakeSaveRepository()
        saves.existsException = RuntimeException("a/b")
        val viewModel = ViewModelImpl(FakeBoardModel(), saves)

        assertFalse(viewModel.saveExists("a/b"))
    }

    @Test
    fun `isSolved delegates to board isCorrect`() {
        val board = FakeBoardModel()
        val viewModel = ViewModelImpl(board)

        board.correct = false
        assertFalse(viewModel.isSolved())

        board.correct = true
        assertTrue(viewModel.isSolved())
    }

    @Test
    fun `boardState delegates to the board's current tile array`() {
        val board = FakeBoardModel(boardArray = intArrayOf(3, 1, 0, 2))
        val viewModel = ViewModelImpl(board)

        assertEquals(listOf(3, 1, 0, 2), viewModel.boardState().toList())
    }

    @Test
    fun `boardState returns a defensive copy - mutating it does not affect the board`() {
        val board = FakeBoardModel(boardArray = intArrayOf(3, 1, 0, 2))
        val viewModel = ViewModelImpl(board)

        viewModel.boardState()[0] = 99

        assertEquals(listOf(3, 1, 0, 2), board.boardArray.toList())
    }

    @Test
    fun `squareSide delegates to the board's square side`() {
        val viewModel = ViewModelImpl(FakeBoardModel(SQUARE_SIDE = 4))

        assertEquals(4, viewModel.squareSide())
    }
}
