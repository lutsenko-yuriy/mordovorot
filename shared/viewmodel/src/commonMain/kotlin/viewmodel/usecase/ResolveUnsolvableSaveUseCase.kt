package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import board_model.Solvability
import storage.SaveRepository
import storage.SavedBoard
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.MordieHooks
import viewmodel.SaveInfo
import viewmodel.UiRequest
import viewmodel.UiRequestChannel
import viewmodel.UnsavedProgress
import viewmodel.UnsolvableSaveChoice
import viewmodel.displayWithSize

/** What happens to a save no sequence of shifts can solve (GH-74): repair, new game or exit. */
internal class ResolveUnsolvableSaveUseCase(
    private val board: BoardModel,
    private val saves: SaveRepository,
    private val mordieHooks: MordieHooks,
    private val analytics: AnalyticsService,
    private val ui: UiRequestChannel,
    private val newGame: NewGameUseCase,
    private val currentSaveName: CurrentSaveName,
    private val gameShiftSources: GameShiftSources,
    private val unsavedProgress: UnsavedProgress,
) {

    /** The repair / new game / exit choice for an unsolvable save (GH-74). At startup, New game
     *  returns [LoadResult.NOT_LOADED] so `restoreOnStartup`'s own size prompt runs. Exit only
     *  reports [LoadResult.EXIT_CHOSEN] (GH-41): the platform runs it once the load returns. */
    suspend fun resolve(name: String, saved: SavedBoard, trigger: String): LoadResult {
        val atStartup = trigger == "startup_prompt"
        val info = SaveInfo(name, saved.squareSide, solvable = false)
        ui.showMessage("'${info.displayWithSize()}' can't be solved - no sequence of shifts reaches the solved board.")
        val choice = ui.ask(UiRequest.ResolveUnsolvableSave(info, atStartup))
        fun track(extra: Map<String, Any> = emptyMap()) = analytics.track(
            "unsolvable_save_resolved",
            mapOf("choice" to choice.name.lowercase(), "trigger" to trigger, "board_size" to saved.squareSide) + extra,
        )
        when (choice) {
            UnsolvableSaveChoice.REPAIR -> {
                val fixed = Solvability.repaired(saved.state)
                // Restored first, so a board that can't take the save (a hand-edited size) never
                // rewrites the file. A failed overwrite still loads - the file keeps its mark.
                board.restoreState(fixed, saved.moveCount)
                mordieHooks.onRepaired()
                currentSaveName.name = name
                gameShiftSources.reset()
                unsavedProgress.unsaved = false
                val error = try {
                    saves.save(name, fixed, saved.squareSide, saved.moveCount)
                    null
                } catch (e: Exception) {
                    e
                }
                track(mapOf("repair_saved" to (error == null)))
                ui.showMessage(
                    if (error == null) "Repaired '$name' (two tiles swapped) and loaded it."
                    else "Repaired and loaded '$name', but could not update the save file: ${error.message ?: error::class.simpleName}"
                )
                return LoadResult.LOADED
            }
            UnsolvableSaveChoice.NEW_GAME -> {
                track()
                if (!atStartup) {
                    val size = ui.ask(UiRequest.ChooseBoardSize(board.squareSide, origin = "unsolvable_save"))
                    newGame.newGame(size ?: board.squareSide, trigger = "unsolvable_save")
                }
                return LoadResult.NOT_LOADED
            }
            UnsolvableSaveChoice.EXIT -> {
                track()
                return LoadResult.EXIT_CHOSEN
            }
        }
    }
}
