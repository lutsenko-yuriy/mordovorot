package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import board_model.Solvability
import storage.SaveRepository
import storage.SavedBoard
import viewmodel.CurrentSaveName
import viewmodel.GameShiftKeys
import viewmodel.ExitRequestedException
import viewmodel.MordieHooks
import viewmodel.SaveInfo
import viewmodel.UiRequest
import viewmodel.UiRequestChannel
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
    private val exitGame: ExitGameUseCase,
    private val currentSaveName: CurrentSaveName,
    private val gameShiftKeys: GameShiftKeys,
) {

    /** The repair / new game / exit choice for an unsolvable save (GH-74). Returns whether the
     *  (repaired) save was loaded. At startup, New game returns `false` so `restoreOnStartup`'s
     *  own size prompt runs, and Exit throws [ExitRequestedException] - there is no game to save. */
    suspend fun resolve(name: String, saved: SavedBoard, trigger: String): Boolean {
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
                gameShiftKeys.reset()
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
                return true
            }
            UnsolvableSaveChoice.NEW_GAME -> {
                track()
                if (!atStartup) {
                    val size = ui.ask(UiRequest.ChooseBoardSize(board.squareSide, origin = "unsolvable_save"))
                    newGame.newGame(size ?: board.squareSide, trigger = "unsolvable_save")
                }
                return false
            }
            UnsolvableSaveChoice.EXIT -> {
                track()
                if (atStartup) throw ExitRequestedException()
                exitGame.exitGame()
                return false
            }
        }
    }
}
