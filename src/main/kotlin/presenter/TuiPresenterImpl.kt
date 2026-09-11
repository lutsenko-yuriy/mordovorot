package presenter

import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardImpl
import board_model.BoardModel
import storage.FileSaveRepository
import storage.SaveRepository
import view.View

/** The TUI-only presenter (GH-3, split out for GH-23): owns the read-only query surface
 *  [view.tui.TuiView] needs to render/re-render outside the console's [ConsolePresenter.play]
 *  loop, plus the public entry point into the shared startup-restore flow. */
class TuiPresenterImpl(
    view: View,
    board: BoardModel = BoardImpl(),
    saves: SaveRepository = FileSaveRepository(),
    analytics: AnalyticsService = NoopAnalyticsService(),
) : BasePresenter(view, board, saves, analytics), TuiPresenter {

    override fun restoreOnStartup() = offerStartupRestore()

    override fun listSaves(): List<String> =
        try {
            saves.listSaves()
        } catch (e: Exception) {
            emptyList()
        }

    override fun saveExists(name: String): Boolean =
        try {
            saves.exists(name)
        } catch (e: Exception) {
            false
        }

    override fun isSolved(): Boolean = board.isCorrect()

    // A defensive copy - board.boardArray is the live, mutable backing array; handing it out
    // directly would let a caller (or a future one) mutate board state without going through
    // shiftLeft/Right/Up/Down (audit finding on PR #22).
    override fun boardState(): IntArray = board.boardArray.copyOf()

    override fun squareSide(): Int = board.SQUARE_SIDE
}
