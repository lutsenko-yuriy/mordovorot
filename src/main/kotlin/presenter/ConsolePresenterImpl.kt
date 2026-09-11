package presenter

import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardImpl
import board_model.BoardModel
import storage.FileSaveRepository
import storage.SaveRepository
import view.EndOfInputException
import view.View

/** The console-only presenter (GH-23): owns [play]'s line-based command loop, the only
 *  console-specific concern that doesn't belong on the shared [BasePresenter] core. */
class ConsolePresenterImpl(
    view: View,
    board: BoardModel = BoardImpl(),
    saves: SaveRepository = FileSaveRepository(),
    analytics: AnalyticsService = NoopAnalyticsService(),
) : BasePresenter(view, board, saves, analytics), ConsolePresenter {

    override fun play() {
        offerStartupRestore()
        while (!board.isCorrect()) {
            try {
                view.displayBoard(board.boardArray, board.SQUARE_SIDE)
                view.processCommand()
            } catch (e: EndOfInputException) {
                return
            } catch (e: ExitRequestedException) {
                return
            } catch (e: Exception) {
                view.showMessage(e.message ?: "Error") // not System.err - stays in sync with the board output
            }
        }
    }
}
