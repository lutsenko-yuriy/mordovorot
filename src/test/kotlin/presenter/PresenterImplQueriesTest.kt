package presenter

import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers [PresenterImpl]'s additive, read-only query methods (`listSaves`, `saveExists`,
 * `isSolved`) that the mouse-driven TUI needs and the console UI never had to ask for
 * (GH-3). All three degrade to a safe default instead of throwing, mirroring the
 * non-throwing discipline [PresenterImpl.saveGame]/[PresenterImpl.loadGame] already follow.
 */
class PresenterImplQueriesTest {

    @Test
    fun `listSaves delegates to the save repository`() {
        val saves = FakeSaveRepository(
            mutableMapOf(
                "foo" to SavedBoard(4, IntArray(16) { it }),
                "bar" to SavedBoard(4, IntArray(16) { it }),
            ),
        )
        val presenter = PresenterImpl(FakeView(), FakeBoardModel(), saves)

        assertEquals(listOf("bar", "foo"), presenter.listSaves())
    }

    @Test
    fun `listSaves degrades to an empty list on a repository failure`() {
        val saves = FakeSaveRepository()
        saves.listSavesException = RuntimeException("unreadable saves dir")
        val presenter = PresenterImpl(FakeView(), FakeBoardModel(), saves)

        assertEquals(emptyList(), presenter.listSaves())
    }

    @Test
    fun `saveExists delegates to the save repository`() {
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, IntArray(16) { it })))
        val presenter = PresenterImpl(FakeView(), FakeBoardModel(), saves)

        assertTrue(presenter.saveExists("foo"))
        assertFalse(presenter.saveExists("bar"))
    }

    @Test
    fun `saveExists degrades to false on a repository failure`() {
        val saves = FakeSaveRepository()
        saves.existsException = RuntimeException("a/b")
        val presenter = PresenterImpl(FakeView(), FakeBoardModel(), saves)

        assertFalse(presenter.saveExists("a/b"))
    }

    @Test
    fun `isSolved delegates to board isCorrect`() {
        val board = FakeBoardModel()
        val presenter = PresenterImpl(FakeView(), board)

        board.correct = false
        assertFalse(presenter.isSolved())

        board.correct = true
        assertTrue(presenter.isSolved())
    }

    @Test
    fun `boardState delegates to the board's current tile array`() {
        val board = FakeBoardModel(boardArray = intArrayOf(3, 1, 0, 2))
        val presenter = PresenterImpl(FakeView(), board)

        assertEquals(listOf(3, 1, 0, 2), presenter.boardState().toList())
    }

    @Test
    fun `squareSide delegates to the board's square side`() {
        val presenter = PresenterImpl(FakeView(), FakeBoardModel(SQUARE_SIDE = 4))

        assertEquals(4, presenter.squareSide())
    }
}
