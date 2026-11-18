package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import storage.SaveRepository
import viewmodel.AutosaveResult
import viewmodel.AutosaveSlot
import viewmodel.MordieHooks
import viewmodel.SessionTileLabels
import viewmodel.UnsavedProgress

/** Writes the game to its [AutosaveSlot] when the page is left (GH-338): what Save writes, but silently, with the current save name untouched.
 *  Not `suspend`: it runs inside `beforeunload`, which cannot wait. */
internal class AutosaveUseCase(
    private val board: BoardModel,
    private val saves: SaveRepository,
    private val mordieHooks: MordieHooks,
    private val analytics: AnalyticsService,
    private val unsavedProgress: UnsavedProgress,
    private val tileLabels: SessionTileLabels,
    private val solution: SolutionUseCase,
) {

    /** [trigger] is what left the game: `page_left`, `update`, `shared_link` or `tutorial_replay`. */
    fun autosave(trigger: String): AutosaveResult {
        // A solved board or the solution screen is no game to come back to; the tutorial's game is not the player's.
        if (!unsavedProgress.unsaved || board.isCorrect() || solution.onScreen || mordieHooks.tutorialGame) return AutosaveResult.SKIPPED
        val result = try {
            saves.save(
                AutosaveSlot.nameFor(tileLabels.current, board.squareSide),
                board.boardArray, board.squareSide, board.moveCount, mordieHooks.snapshot(), tileLabels.current.wireName,
            )
            unsavedProgress.unsaved = false
            AutosaveResult.SAVED
        } catch (e: Exception) {
            AutosaveResult.FAILED
        }
        analytics.track(
            "autosave_written",
            mapOf(
                "result" to if (result == AutosaveResult.SAVED) "success" else "error",
                "board_size" to board.squareSide,
                "tile_labels" to tileLabels.current.wireName,
                "moves" to board.moveCount,
                "trigger" to trigger,
            ),
        )
        return result
    }
}
