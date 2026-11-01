package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import viewmodel.MordieHooks
import viewmodel.SaveCatalog
import viewmodel.UiRequest
import viewmodel.UiRequestChannel
import viewmodel.catchingNonCancellation

/** What happens before play starts: restore a save or pick a size, then start Mordie. */
internal class StartupRestoreUseCase(
    private val board: BoardModel,
    private val catalog: SaveCatalog,
    private val analytics: AnalyticsService,
    private val ui: UiRequestChannel,
    private val loadGame: LoadGameUseCase,
    private val newGame: NewGameUseCase,
    private val mordieHooks: MordieHooks,
) {

    /** Guards [restoreOnStartup] against running twice. */
    private var startupRestoreDone = false

    /** The startup sequence (GH-44 grew this beyond just the restore prompt): offers to restore
     *  a previous save (0/1/2+ saves, unchanged from GH-6), then - only if nothing was actually
     *  restored - offers a size prompt for the fresh game about to start. Mordie starts on
     *  whatever board play begins with, unless it is already bound. */
    suspend fun restoreOnStartup() {
        restoreOrPickSize()
        mordieHooks.onStartup()
    }

    private suspend fun restoreOrPickSize() {
        if (startupRestoreDone) return
        startupRestoreDone = true
        // Any failure here just starts a fresh game instead of crashing at boot.
        catchingNonCancellation(onError = {}) {
            val saveInfos = catalog.infos()
            val restored = if (saveInfos.isEmpty()) {
                false
            } else {
                analytics.track("startup_restore_prompt_shown", mapOf("save_file_count" to saveInfos.size))

                val nameToRestore = if (saveInfos.size == 1) {
                    saveInfos[0].name.takeIf { ui.ask(UiRequest.ConfirmRestore(saveInfos[0])) }
                } else {
                    // The view answers with a listed name; anything else counts as declined (GH-79).
                    val chosen = ui.ask(UiRequest.ChooseSaveToRestore(saveInfos))
                    saveInfos.firstOrNull { it.name == chosen }?.name
                }

                // loadGame can still fail here (a save deleted or corrupted since listSaves()
                // ran just above) - decision reflects what actually happened, not just what was
                // picked.
                fun trackDecision(decision: String) =
                    analytics.track("startup_restore_decision", mapOf("decision" to decision, "save_file_count" to saveInfos.size))
                val result = nameToRestore?.let { loadGame.loadGame(it, trigger = "startup_prompt") } ?: LoadResult.NOT_LOADED
                trackDecision(if (result == LoadResult.LOADED) "restored" else "new_game")
                result == LoadResult.LOADED
            }

            if (!restored) {
                ui.ask(UiRequest.ChooseBoardSize(board.squareSide))?.let { newGame.newGame(it, trigger = "startup_prompt") }
            }
        }
    }
}
