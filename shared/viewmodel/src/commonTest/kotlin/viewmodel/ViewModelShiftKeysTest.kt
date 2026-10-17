package viewmodel

import kotlinx.coroutines.test.runTest
import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import storage.SavedBoard
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-101: `board_solved.shift_keys` - which keys made this game's shifts, keyboard mode only.
 *  Reset by a new game, a load or a repair, not by a save. */
class ViewModelShiftKeysTest {

    /** 3x3, solved by shifting row 0 left twice (or right once). */
    private fun nearlySolved() = BoardImpl(3).apply { restoreState(intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8)) }

    private fun shiftKeys(analytics: RecordingAnalyticsService) =
        analytics.events.single { it.name == "board_solved" }.properties["shift_keys"]

    @Test
    fun `shifts made with the arrow keys report arrows`() {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(nearlySolved(), analytics = analytics)

        viewModel.shiftRight(0, ShiftKey.ARROW)

        assertEquals("arrows", shiftKeys(analytics))
    }

    @Test
    fun `shifts made with HJKL report hjkl`() {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(nearlySolved(), analytics = analytics)

        viewModel.shiftRight(0, ShiftKey.HJKL)

        assertEquals("hjkl", shiftKeys(analytics))
    }

    @Test
    fun `both kinds of key report mixed`() {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(nearlySolved(), analytics = analytics)

        viewModel.shiftLeft(0, ShiftKey.ARROW)
        viewModel.shiftLeft(0, ShiftKey.HJKL)

        assertEquals("mixed", shiftKeys(analytics))
    }

    @Test
    fun `a solving shift without a key - mouse mode - sends no shift_keys`() {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(nearlySolved(), analytics = analytics)

        viewModel.shiftLeft(0, ShiftKey.HJKL)
        viewModel.shiftLeft(0)

        assertFalse("shift_keys" in analytics.events.single { it.name == "board_solved" }.properties)
    }

    @Test
    fun `mouse clicks earlier in the game aren't counted - only the keys are`() {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(nearlySolved(), analytics = analytics)

        viewModel.shiftLeft(0)
        viewModel.shiftLeft(0, ShiftKey.ARROW)

        assertEquals("arrows", shiftKeys(analytics))
    }

    @Test
    fun `a new game resets the tally`() {
        val analytics = RecordingAnalyticsService()
        val board = BoardImpl(3)
        val viewModel = testViewModel(board, analytics = analytics)

        viewModel.shiftUp(0, ShiftKey.ARROW)
        viewModel.newGame(3, trigger = "toolbar")
        board.restoreState(intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8))
        viewModel.shiftRight(0, ShiftKey.HJKL)

        assertEquals("hjkl", shiftKeys(analytics))
    }

    @Test
    fun `a load and a repair reset the tally, a save doesn't`() = runTest {
        val saves = FakeSaveRepository(
            mutableMapOf(
                "near" to SavedBoard(3, intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8), 0),
                "odd" to SavedBoard(3, intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8), 0),
            ),
        )
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(3), saves, analytics)
        val ui = FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR))

        ui.drive(viewModel) {
            viewModel.shiftUp(1, ShiftKey.ARROW)
            viewModel.loadGame("odd") // repaired: 1 2 0 ... - the arrow shift above no longer counts
            viewModel.shiftUp(1, ShiftKey.ARROW)
            viewModel.loadGame("near") // the arrow shift above no longer counts either
            viewModel.shiftLeft(0, ShiftKey.HJKL)
            viewModel.saveGame("kept") // a save keeps the tally
            viewModel.shiftLeft(0, ShiftKey.HJKL)
        }

        assertEquals("hjkl", shiftKeys(analytics))
    }
}
