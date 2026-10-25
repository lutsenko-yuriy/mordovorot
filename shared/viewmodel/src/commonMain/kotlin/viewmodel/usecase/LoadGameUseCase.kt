package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import board_model.Solvability
import storage.SaveRepository
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.MordieHooks
import viewmodel.SaveCatalog
import viewmodel.SessionTileLabels
import viewmodel.TileLabels
import viewmodel.UiRequest
import viewmodel.UiRequestChannel
import viewmodel.UnsavedProgress
import viewmodel.catchingNonCancellation

/** Loads a save by name; an unsolvable one goes to [ResolveUnsolvableSaveUseCase] (GH-74), and a
 *  solution-screen one reopens the screen (GH-126). */
internal class LoadGameUseCase(
    private val board: BoardModel,
    private val saves: SaveRepository,
    private val mordieHooks: MordieHooks,
    private val analytics: AnalyticsService,
    private val ui: UiRequestChannel,
    private val catalog: SaveCatalog,
    private val resolveUnsolvable: ResolveUnsolvableSaveUseCase,
    private val currentSaveName: CurrentSaveName,
    private val gameShiftSources: GameShiftSources,
    private val unsavedProgress: UnsavedProgress,
    private val solution: SolutionUseCase,
    private val tileLabels: SessionTileLabels,
    private val tileLabelsUseCase: TileLabelsUseCase,
) {

    /** [trigger] is `"command"` for a mid-game load (toolbar Load / F6), or `"startup_prompt"`
     *  when called from `restoreOnStartup` - see `load_command_used` in docs/ANALYTICS_EVENTS.md.
     *  Returns whether the board was actually restored, so `restoreOnStartup` can report an
     *  accurate `startup_restore_decision` instead of assuming success (audit on PR #14: a
     *  corrupted/mismatched save offered at startup was being recorded as "restored") - or that
     *  Exit was chosen for an unsolvable save (GH-41). */
    suspend fun loadGame(name: String, trigger: String = "command"): LoadResult {
        // The whole body is guarded, not just saves.load - catalog.availableSavesMessage() (itself
        // saves.listSaves()) and board.restoreState can also throw, and the startup restore
        // flow calls this before any View's own try/catch exists, so loadGame must not throw
        // regardless of which step fails (audit on PR #13).
        return catchingNonCancellation(
            onError = { e ->
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "error"))
                ui.showMessage(e.message ?: "Could not load '$name'.")
                LoadResult.NOT_LOADED
            },
        ) {
            val saved = saves.load(name)
            if (saved == null) {
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "not_found"))
                ui.showMessage("No save named '$name'. ${catalog.availableSavesMessage()}")
                return LoadResult.NOT_LOADED
            }
            if (!Solvability.isSolvable(saved.state)) {
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "unsolvable"))
                return resolveUnsolvable.resolve(name, saved, trigger)
            }
            // No size-mismatch rejection (GH-44 WU5) - board.restoreState resizes the board to
            // match the save instead, so every successful saves.load() now restores.
            board.restoreState(saved.state, saved.moveCount)
            solution.leave()
            mordieHooks.onLoaded(saved.mordie)
            currentSaveName.name = name
            gameShiftSources.reset()
            unsavedProgress.unsaved = false
            // An unknown name (a later build's scheme, a hand edit) loads with the preference (GH-139).
            val fromSave = TileLabels.fromWireName(saved.labels)
            val labelsDiffer = fromSave != null && fromSave != tileLabels.effectivePreference
            tileLabels.applySave(fromSave, board.squareSide)
            analytics.track(
                "load_command_used",
                mapOf("trigger" to trigger, "result" to "success", "labels_differ_from_preference" to labelsDiffer),
            )
            if (saved.mordie?.solutionScreen == true) solution.enterLoaded(trigger)
            if (fromSave != null && !fromSave.fits(board.squareSide)) chooseFittingLabels()
            ui.showMessage("Loaded '$name'.")
            LoadResult.LOADED
        }
    }

    /** Only a hand-edited save has Letters on 6x6 (GH-160); numbers show until the player picks.
     *  The message names Letters: up to 6x6 it is the only scheme that can fail to fit. */
    private suspend fun chooseFittingLabels() {
        val side = board.squareSide
        tileLabelsUseCase.dialogOpened("letters_save")
        val message = "Letters don't fit a ${side}x$side board (26 letters, ${side * side} tiles) - pick other labels."
        ui.ask(UiRequest.ChooseFittingLabels(message))?.let(tileLabelsUseCase::change)
    }
}
