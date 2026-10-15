package viewmodel

import storage.SaveFileFormatException
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
    fun `listSaves delegates to the save repository, sorted by name`() {
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, IntArray(16) { it }),
                "bar" to SavedBoard(5, IntArray(25) { it }),
            ),
        )
        val viewModel = ViewModelImpl(FakeBoardModel(), saves)

        assertEquals(listOf(SaveInfo("bar", 5), SaveInfo("foo", 4)), viewModel.listSaves())
    }

    @Test
    fun `listSaves reports a null squareSide for a save whose size can't be read, rather than dropping it`() {
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, IntArray(16) { it })))
        saves.loadException = SaveFileFormatException("foo", "corrupted")
        val viewModel = ViewModelImpl(FakeBoardModel(), saves)

        assertEquals(listOf(SaveInfo("foo", null)), viewModel.listSaves())
    }

    /** GH-74: 2 1 0 ... is one swap from solved - odd, so unsolvable on 3x3. */
    @Test
    fun `listSaves marks an odd-parity 3x3 save unsolvable and never a 4x4 one`() {
        val oneSwap3 = intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8)
        val oneSwap4 = IntArray(16) { it }.also { it[0] = 2; it[2] = 0 }
        val saves = FakeSaveRepository(
            mutableMapOf(
                "odd" to SavedBoard(3, oneSwap3),
                "even" to SavedBoard(4, oneSwap4),
                "fine" to SavedBoard(3, IntArray(9) { it }),
            ),
        )
        val viewModel = ViewModelImpl(FakeBoardModel(), saves)

        assertEquals(
            listOf(SaveInfo("even", 4), SaveInfo("fine", 3), SaveInfo("odd", 3, solvable = false)),
            viewModel.listSaves(),
        )
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
        val viewModel = ViewModelImpl(FakeBoardModel(initialSquareSide = 4))

        assertEquals(4, viewModel.squareSide())
    }
}
