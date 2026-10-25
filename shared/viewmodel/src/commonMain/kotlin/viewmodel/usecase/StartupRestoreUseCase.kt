package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import viewmodel.MordieHooks
import viewmodel.SaveCatalog
import viewmodel.SessionTileLabels
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
    private val tileLabels: SessionTileLabels,
    /** Seeded `true` by `GameSession` after a mode switch, so the prompt doesn't re-show. */
    startupRestoreDone: Boolean,
    /** `true` when `--size=N` was given at launch (GH-44) - the flag *is* the answer, so
     *  [restoreOnStartup] skips both the restore prompt and the size prompt entirely and starts
     *  fresh at the size the board was already constructed with, regardless of what saves
     *  exist. */
    private val sizeChosenAtLaunch: Boolean,
) {

    /** Guards [restoreOnStartup] against running twice - including across a mode switch. */
    private var startupRestoreDone = startupRestoreDone

    /** The startup sequence (GH-44 grew this beyond just the restore prompt): offers to restore
     *  a previous save (0/1/2+ saves, unchanged from GH-6), then - only if nothing was actually
     *  restored - offers a size prompt for the fresh game about to start. Skips both steps
     *  entirely when [sizeChosenAtLaunch] is set: `--size=N` *is* the answer, so the board just
     *  keeps the size it was already constructed with, regardless of what saves exist. Mordie
     *  starts on whatever board play begins with, unless it is already bound (a mode switch).
     *  Returns whether Exit was chosen for an unsolvable save (GH-41) - then neither the size
     *  prompt nor Mordie runs, and the caller ends the session. */
    suspend fun restoreOnStartup(): Boolean {
        val exitChosen = restoreOrPickSize()
        if (!exitChosen) mordieHooks.onStartup()
        return exitChosen
    }

    /** Returns whether Exit was chosen for an unsolvable save. */
    private suspend fun restoreOrPickSize(): Boolean {
        if (startupRestoreDone) return false
        startupRestoreDone = true
        if (sizeChosenAtLaunch) {
            // The labels were set before the board's size was known, e.g. Letters on 6x6 (GH-160).
            tileLabels.resetToPreference(board.squareSide)
            return false
        }
        // Any failure here just starts a fresh game instead of crashing at boot.
        return catchingNonCancellation(onError = { false }) {
            val saveInfos = catalog.infos()
            val restored = if (saveInfos.isEmpty()) {
                false
            } else {
                analytics.track("startup_restore_prompt_shown", mapOf("save_file_count" to saveInfos.size))

                val nameToRestore = if (saveInfos.size == 1) {
                    saveInfos[0].name.takeIf { ui.ask(UiRequest.ConfirmRestore(saveInfos[0])) }
                } else {
                    // The TUI answers with a listed name; anything else counts as declined (GH-79).
                    val chosen = ui.ask(UiRequest.ChooseSaveToRestore(saveInfos))
                    saveInfos.firstOrNull { it.name == chosen }?.name
                }

                // loadGame can still fail here (a save deleted or corrupted since listSaves()
                // ran just above) - decision reflects what actually happened, not just what was
                // picked.
                fun trackDecision(decision: String) =
                    analytics.track("startup_restore_decision", mapOf("decision" to decision, "save_file_count" to saveInfos.size))
                val result = nameToRestore?.let { loadGame.loadGame(it, trigger = "startup_prompt") } ?: LoadResult.NOT_LOADED
                if (result == LoadResult.EXIT_CHOSEN) {
                    // Exit from the unsolvable-save choice (GH-74).
                    trackDecision("quit")
                    return@catchingNonCancellation true
                }
                trackDecision(if (result == LoadResult.LOADED) "restored" else "new_game")
                result == LoadResult.LOADED
            }

            if (!restored) {
                ui.ask(UiRequest.ChooseBoardSize(board.squareSide))?.let { newGame.newGame(it, trigger = "startup_prompt") }
            }
            false
        }
    }
}
