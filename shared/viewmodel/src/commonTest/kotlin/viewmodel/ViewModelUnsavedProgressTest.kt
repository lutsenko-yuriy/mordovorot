package viewmodel

import board_model.BoardImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import storage.SavedBoard
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.testViewModel

/** GH-110: `hasUnsavedProgress`, behind the web's leave-site prompt - set by a shift, cleared by
 *  a new game, a successful save or load, and a repair. */
class ViewModelUnsavedProgressTest {

    private val saves = FakeSaveRepository(
        mutableMapOf(
            "near" to SavedBoard(3, intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8), 0),
            "odd" to SavedBoard(3, intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8), 0),
        ),
    )
    private val viewModel = testViewModel(BoardImpl(3), saves)

    private suspend fun drive(ui: FakeViewModelUi = FakeViewModelUi(), block: suspend () -> Unit) = ui.drive(viewModel, block)

    @Test
    fun `a fresh game has no unsaved progress, and a shift sets it`() {
        assertFalse(viewModel.hasUnsavedProgress())

        viewModel.shiftLeft(0)

        assertTrue(viewModel.hasUnsavedProgress())
    }

    @Test
    fun `a multi-step shift sets it, zero steps doesn't`() {
        viewModel.shiftRow(0, 0, PointerShift.DRAG)
        assertFalse(viewModel.hasUnsavedProgress())

        viewModel.shiftColumn(1, 2, PointerShift.DRAG)
        assertTrue(viewModel.hasUnsavedProgress())
    }

    @Test
    fun `a new game clears it`() {
        viewModel.shiftUp(0)

        viewModel.newGame(4, trigger = "toolbar")

        assertFalse(viewModel.hasUnsavedProgress())
    }

    @Test
    fun `a successful save clears it, a failed one keeps it`() = runTest {
        viewModel.shiftDown(0)
        saves.saveException = IllegalStateException("disk full")
        drive { viewModel.saveGame("x") }
        assertTrue(viewModel.hasUnsavedProgress())

        saves.saveException = null
        drive { viewModel.saveGame("x") }
        assertFalse(viewModel.hasUnsavedProgress())
    }

    @Test
    fun `a successful load clears it, a failed or missing one keeps it`() = runTest {
        viewModel.shiftRight(1)
        drive { viewModel.loadGame("missing") }
        assertTrue(viewModel.hasUnsavedProgress())

        saves.loadException = IllegalStateException("corrupted")
        drive { viewModel.loadGame("near") }
        assertTrue(viewModel.hasUnsavedProgress())

        saves.loadException = null
        drive { viewModel.loadGame("near") }
        assertFalse(viewModel.hasUnsavedProgress())
    }

    @Test
    fun `Repair and load clears it, even when the repaired save can't be written back`() = runTest {
        viewModel.shiftLeft(2)
        saves.saveException = IllegalStateException("read-only")

        drive(FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR))) {
            viewModel.loadGame("odd")
        }

        assertFalse(viewModel.hasUnsavedProgress())
    }

    @Test
    fun `New game for an unsolvable save clears it`() = runTest {
        viewModel.shiftLeft(2)

        drive(
            FakeViewModelUi(
                resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.NEW_GAME),
                chooseBoardSizeResponses = mutableListOf(4),
            ),
        ) { viewModel.loadGame("odd") }

        assertFalse(viewModel.hasUnsavedProgress())
    }

    @Test
    fun `a session's later View sees the same flag`() {
        val progress = UnsavedProgress()
        testViewModel(BoardImpl(3), unsavedProgress = progress).shiftLeft(0)

        assertTrue(testViewModel(BoardImpl(3), unsavedProgress = progress).hasUnsavedProgress())
    }
}
