package presenter

import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardImpl
import board_model.BoardModel
import kotlinx.coroutines.CancellationException
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
    startupRestoreDone: Boolean = false,
) : BasePresenter(view, board, saves, analytics, startupRestoreDone), ConsolePresenter {

    override suspend fun play() {
        offerStartupRestore()
        while (!board.isCorrect()) {
            try {
                view.displayBoard(board.boardArray, board.SQUARE_SIDE)
                view.processCommand()
            } catch (e: EndOfInputException) {
                return
            } catch (e: ExitRequestedException) {
                return
            } catch (e: SessionControlException) {
                throw e // e.g. ModeSwitchRequestedException - must reach GameSession, not the catch-all below
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showMessage(e.message ?: "Error") // not System.err - stays in sync with the board output; ask()-based (GH-42 WU2)
            }
        }
    }
}
