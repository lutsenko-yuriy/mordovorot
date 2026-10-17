package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import storage.SaveRepository
import viewmodel.CurrentSaveName
import viewmodel.MordieHooks
import viewmodel.UiRequestChannel
import viewmodel.UnsavedProgress
import viewmodel.catchingNonCancellation

/** Saves the current game under a name, Mordie's progress included (GH-83). */
internal class SaveGameUseCase(
    private val board: BoardModel,
    private val saves: SaveRepository,
    private val mordieHooks: MordieHooks,
    private val analytics: AnalyticsService,
    private val ui: UiRequestChannel,
    private val currentSaveName: CurrentSaveName,
    private val unsavedProgress: UnsavedProgress,
) {

    /** Returns whether the save succeeded; a failure is tracked and shown, never thrown. */
    suspend fun saveGame(name: String): Boolean {
        // The Save dialog is modal and prefilled from currentSaveName (GH-99), so comparing with
        // it tells whether the player kept the prefill.
        val nameSource = when (currentSaveName.name) {
            null -> "typed"
            name -> "prefill"
            else -> "edited"
        }
        // saves.exists/save can throw (bad name, IOException) - caught here rather than left to
        // play()'s generic handler, so a failed save is still tracked and gets its own message
        // instead of silently missing from save_command_used (audit finding on PR #13).
        return catchingNonCancellation(
            onError = { e ->
                analytics.track("save_command_used", mapOf("result" to "error", "name_source" to nameSource))
                // Always say the save failed - e.message alone can be just a path (PR #15).
                ui.showMessage("Could not save as '$name': ${e.message ?: e::class.simpleName}")
                false
            },
        ) {
            val existed = saves.exists(name)
            saves.save(name, board.boardArray, board.squareSide, board.moveCount, mordieHooks.snapshot())
            currentSaveName.name = name
            unsavedProgress.unsaved = false
            analytics.track(
                "save_command_used",
                mapOf("result" to "success", "overwrote_existing" to existed, "name_source" to nameSource),
            )
            ui.showMessage("Saved as '$name'.")
            true
        }
    }
}
