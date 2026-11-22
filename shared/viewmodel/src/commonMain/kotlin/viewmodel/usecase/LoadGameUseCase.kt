package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import board_model.Solvability
import storage.SaveFileFormatException
import storage.SaveFromNewerVersionException
import storage.SaveRepository
import texts.texts
import viewmodel.AutosaveSlot
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.GameStart
import viewmodel.MordieHooks
import viewmodel.PlayerResult
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
    private val gameStart: GameStart,
) {

    /** [trigger] is `"command"` for a mid-game load (toolbar Load / 3), or `"startup_prompt"`
     *  when called from `restoreOnStartup` - see `load_command_used` in docs/ANALYTICS_EVENTS.md.
     *  Returns whether the board was actually restored, so `restoreOnStartup` can report an
     *  accurate `startup_restore_decision` instead of assuming success (audit on PR #14: a
     *  corrupted/mismatched save offered at startup was being recorded as "restored"). */
    suspend fun loadGame(name: String, trigger: String = "command"): LoadResult {
        // The whole body is guarded, not just saves.load - catalog.availableSavesMessage() (itself
        // saves.listSaves()) and board.restoreState can also throw, and the startup restore
        // flow calls this before any View's own try/catch exists, so loadGame must not throw
        // regardless of which step fails (audit on PR #13).
        return catchingNonCancellation(
            onError = { e ->
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "error"))
                // Storage's reasons are developer diagnostics: the player gets one friendly line per case (GH-274).
                ui.showMessage(
                    when (e) {
                        is SaveFromNewerVersionException -> texts().status.saveFromNewerVersion
                        is SaveFileFormatException -> texts().status.saveDamaged(name)
                        else -> texts().status.loadFailed(name)
                    },
                )
                LoadResult.NOT_LOADED
            },
        ) {
            val saved = saves.load(name)
            if (saved == null) {
                analytics.track("load_command_used", mapOf("trigger" to trigger, "result" to "not_found"))
                ui.showMessage(texts().status.noSaveNamed(name, catalog.availableSavesMessage()))
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
            gameStart.record(board, mordieHooks.original())
            currentSaveName.name = name
            gameShiftSources.reset()
            unsavedProgress.unsaved = false
            // An unknown name (a later build's scheme, a hand edit) loads with the preference (GH-139).
            val fromSave = TileLabels.fromWireName(saved.labels)
            val labelsDiffer = fromSave != null && fromSave != tileLabels.effectivePreference
            tileLabels.applySave(fromSave, board.squareSide)
            analytics.track(
                "load_command_used",
                mapOf("trigger" to trigger, "result" to "success", "labels_differ_from_preference" to labelsDiffer, "autosave" to AutosaveSlot.isAutosave(name)),
            )
            if (saved.mordie?.solutionScreen == true) solution.enterLoaded(trigger, saved.mordie?.playerResult?.let { PlayerResult(it.gaveUp, it.moves) })
            if (fromSave != null && !fromSave.fits(board.squareSide)) chooseFittingLabels(fromSave)
            ui.showMessage(texts().status.loaded(name), loadedSave = name)
            LoadResult.LOADED
        }
    }

    /** Only a hand-edited save has Letters, Cyrillic, Greek or pre-reform Russian letters on a board too big for them (GH-160, GH-313, GH-322, GH-354); numbers show until the player picks.
     *  The message names the scheme that didn't fit: the two letter schemes are the only ones that can. */
    private suspend fun chooseFittingLabels(scheme: TileLabels) {
        val side = board.squareSide
        tileLabelsUseCase.dialogOpened("letters_save")
        val message = with(texts().status) {
            when (scheme) {
                TileLabels.CYRILLIC_LETTERS -> cyrillicLettersDontFit(side)
                TileLabels.GREEK_LETTERS -> greekLettersDontFit(side)
                TileLabels.PREREFORM_LETTERS -> prereformLettersDontFit(side)
                else -> lettersDontFit(side)
            }
        }
        ui.ask(UiRequest.ChooseFittingLabels(message))?.let(tileLabelsUseCase::change)
    }
}
