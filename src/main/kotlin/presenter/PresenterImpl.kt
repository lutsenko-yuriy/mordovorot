package presenter

import analytics.AnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardImpl
import board_model.BoardModel
import storage.FileSaveRepository
import storage.SaveRepository
import view.EndOfInputException
import view.View

class PresenterImpl(
    var view: View,
    var board: BoardModel = BoardImpl(),
    private val saves: SaveRepository = FileSaveRepository(),
    private val analytics: AnalyticsService = NoopAnalyticsService(),
) : Presenter {

    override fun shiftLeft(row: Int) = board.shiftLeft(row)

    override fun shiftRight(row: Int) = board.shiftRight(row)

    override fun shiftUp(col: Int) = board.shiftUp(col)

    override fun shiftDown(col: Int) = board.shiftDown(col)

    override fun resetGame() = board.resetGame()

    override fun saveGame(name: String) {
        // saves.exists/save can throw (bad name, IOException) - caught here rather than left to
        // play()'s generic handler, so a failed save is still tracked and gets its own message
        // instead of silently missing from save_command_used (audit finding on PR #13).
        try {
            val existed = saves.exists(name)
            saves.save(name, board.boardArray, board.SQUARE_SIDE)
            analytics.track("save_command_used", mapOf("result" to "success", "overwrote_existing" to existed))
            view.showMessage("Saved as '$name'.")
        } catch (e: Exception) {
            analytics.track("save_command_used", mapOf("result" to "error"))
            view.showMessage(e.message ?: "Could not save as '$name'.")
        }
    }

    override fun loadGame(name: String) {
        // The whole body is guarded, not just saves.load - availableSavesMessage() (itself
        // saves.listSaves()) and board.restoreState can also throw, and WU4's startup restore
        // flow calls this before play()'s try/catch exists, so loadGame must not throw
        // regardless of which step fails (audit on PR #13).
        try {
            val saved = saves.load(name)
            if (saved == null) {
                analytics.track("load_command_used", mapOf("trigger" to "command", "result" to "not_found"))
                view.showMessage("No save named '$name'. ${availableSavesMessage()}")
                return
            }
            if (saved.squareSide != board.SQUARE_SIDE) {
                analytics.track("load_command_used", mapOf("trigger" to "command", "result" to "size_mismatch"))
                view.showMessage(
                    "Save '$name' is a ${saved.squareSide}x${saved.squareSide} board and can't be loaded onto " +
                        "this ${board.SQUARE_SIDE}x${board.SQUARE_SIDE} board."
                )
                return
            }
            board.restoreState(saved.state)
            analytics.track("load_command_used", mapOf("trigger" to "command", "result" to "success"))
            view.showMessage("Loaded '$name'.")
        } catch (e: Exception) {
            analytics.track("load_command_used", mapOf("trigger" to "command", "result" to "error"))
            view.showMessage(e.message ?: "Could not load '$name'.")
        }
    }

    private fun availableSavesMessage(): String {
        // Guarded on its own - a failure here (e.g. an unreadable saves/ directory) shouldn't
        // change the load's actual result (it was still "not found"), just degrade the message.
        val available = try {
            saves.listSaves()
        } catch (e: Exception) {
            return "Could not list available saves."
        }
        return if (available.isEmpty()) "No saves available." else "Available saves: ${available.joinToString(", ")}"
    }

    override fun play() {
        while (!board.isCorrect()) {
            try {
                view.displayBoard(board.boardArray, board.SQUARE_SIDE)
                view.processCommand()
            } catch (e: EndOfInputException) {
                return
            } catch (e: Exception) {
                view.showMessage(e.message ?: "Error") // not System.err - stays in sync with the board output
            }
        }
    }
}
