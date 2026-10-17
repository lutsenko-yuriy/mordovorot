package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import storage.SaveRepository
import viewmodel.MordieHooks
import viewmodel.UiRequestChannel
import viewmodel.catchingNonControl

/** Saves the current game under a name, Mordie's progress included (GH-83). */
internal class SaveGameUseCase(
    private val board: BoardModel,
    private val saves: SaveRepository,
    private val mordieHooks: MordieHooks,
    private val analytics: AnalyticsService,
    private val ui: UiRequestChannel,
) {

    /** Returns whether the save succeeded; a failure is tracked and shown, never thrown. */
    suspend fun saveGame(name: String): Boolean {
        // saves.exists/save can throw (bad name, IOException) - caught here rather than left to
        // play()'s generic handler, so a failed save is still tracked and gets its own message
        // instead of silently missing from save_command_used (audit finding on PR #13).
        return catchingNonControl(
            onError = { e ->
                analytics.track("save_command_used", mapOf("result" to "error"))
                // Always say the save failed - e.message alone can be just a path (PR #15).
                ui.showMessage("Could not save as '$name': ${e.message ?: e::class.simpleName}")
                false
            },
        ) {
            val existed = saves.exists(name)
            saves.save(name, board.boardArray, board.squareSide, board.moveCount, mordieHooks.snapshot())
            analytics.track("save_command_used", mapOf("result" to "success", "overwrote_existing" to existed))
            ui.showMessage("Saved as '$name'.")
            true
        }
    }
}
