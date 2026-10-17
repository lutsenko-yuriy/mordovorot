package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import board_model.Solvability
import storage.SaveRepository
import viewmodel.CurrentSaveName
import viewmodel.GameShiftKeys
import viewmodel.MordieHooks
import viewmodel.SaveCatalog
import viewmodel.UiRequestChannel
import viewmodel.catchingNonControl

/** Loads a save by name; an unsolvable one goes to [ResolveUnsolvableSaveUseCase] (GH-74). */
internal class LoadGameUseCase(
    private val board: BoardModel,
    private val saves: SaveRepository,
    private val mordieHooks: MordieHooks,
    private val analytics: AnalyticsService,
    private val ui: UiRequestChannel,
    private val catalog: SaveCatalog,
    private val resolveUnsolvable: ResolveUnsolvableSaveUseCase,
    private val currentSaveName: CurrentSaveName,
    private val gameShiftKeys: GameShiftKeys,
) {

    /** [trigger] is `"command"` for a mid-game load (toolbar Load / F6), or `"startup_prompt"`
     *  when called from `restoreOnStartup` - see `load_command_used` in docs/ANALYTICS_EVENTS.md.
     *  Returns whether the board was actually restored, so `restoreOnStartup` can report an
     *  accurate `startup_restore_decision` instead of assuming success (audit on PR #14: a
     *  corrupted/mismatched save offered at startup was being recorded as "restored"). */
    suspend fun loadGame(name: String, trigger: String = "command"): Boolean {
        // The whole body is guarded, not just saves.load - catalog.availableSavesMessage() (itself
        // saves.listSaves()) and board.restoreState can also throw, and the startup restore
        // flow calls this before any View's own try/catch exists, so loadGame must not throw
        // regardless of which step fails (audit on PR #13).
        return catchingNonControl(
            onError = { e ->
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "error"))
                ui.showMessage(e.message ?: "Could not load '$name'.")
                false
            },
        ) {
            val saved = saves.load(name)
            if (saved == null) {
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "not_found"))
                ui.showMessage("No save named '$name'. ${catalog.availableSavesMessage()}")
                return false
            }
            if (!Solvability.isSolvable(saved.state)) {
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "unsolvable"))
                return resolveUnsolvable.resolve(name, saved, trigger)
            }
            // No size-mismatch rejection (GH-44 WU5) - board.restoreState resizes the board to
            // match the save instead, so every successful saves.load() now restores.
            board.restoreState(saved.state, saved.moveCount)
            mordieHooks.onLoaded(saved.mordie)
            currentSaveName.name = name
            gameShiftKeys.reset()
            analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "success"))
            ui.showMessage("Loaded '$name'.")
            true
        }
    }
}
