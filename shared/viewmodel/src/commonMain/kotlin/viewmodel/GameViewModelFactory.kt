package viewmodel

import analytics.AnalyticsService
import board_model.BoardModel
import mordie.Mordie
import storage.SaveRepository
import viewmodel.usecase.AutosaveUseCase
import viewmodel.usecase.ControlsDialogUseCase
import viewmodel.usecase.DeleteSaveUseCase
import viewmodel.usecase.LanguageUseCase
import viewmodel.usecase.LoadGameUseCase
import viewmodel.usecase.NewGameUseCase
import viewmodel.usecase.ResolveUnsolvableSaveUseCase
import viewmodel.usecase.RestartGameUseCase
import viewmodel.usecase.SaveGameUseCase
import viewmodel.usecase.SolutionUseCase
import viewmodel.usecase.StartupRestoreUseCase
import viewmodel.usecase.TileLabelsUseCase

/** Builds a [GameViewModelImpl] and its use cases (GH-41) - the only way a platform gets one.
 *  [MordieHooks] is built here, per call, so it tracks through this [analytics]. [gameStart] (GH-349) is the session's. [ui] is shared
 *  by the viewModel and every use case, and with the View that answers its requests. [solutionScreen] (GH-126), [tileLabels] (GH-139) and [language] (GH-275) are the
 *  session's. */
fun buildGameViewModel(
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    mordie: Mordie,
    currentSaveName: CurrentSaveName,
    gameShiftSources: GameShiftSources,
    unsavedProgress: UnsavedProgress,
    solutionScreen: SolutionScreen,
    tileLabels: SessionTileLabels,
    language: SessionLanguage,
    ui: UiRequestChannel,
    gameStart: GameStart = GameStart(),
): GameViewModelImpl {
    val mordieHooks = MordieHooks(mordie, board, analytics, solutionScreen)
    val solution = SolutionUseCase(board, mordieHooks, analytics, solutionScreen, unsavedProgress)
    val catalog = SaveCatalog(saves)
    val newGame = NewGameUseCase(board, mordieHooks, analytics, currentSaveName, gameShiftSources, unsavedProgress, solution, tileLabels, gameStart)
    val saveGame = SaveGameUseCase(board, saves, mordieHooks, analytics, ui, currentSaveName, unsavedProgress, tileLabels)
    val autosave = AutosaveUseCase(board, saves, mordieHooks, analytics, unsavedProgress, tileLabels, solution)
    val resolveUnsolvable = ResolveUnsolvableSaveUseCase(
        board, saves, mordieHooks, analytics, ui, newGame, currentSaveName, gameShiftSources, unsavedProgress, solution, tileLabels, gameStart,
    )
    val tileLabelsUseCase = TileLabelsUseCase(board, tileLabels, analytics, solution)
    val loadGame = LoadGameUseCase(
        board, saves, mordieHooks, analytics, ui, catalog, resolveUnsolvable, currentSaveName, gameShiftSources, unsavedProgress,
        solution, tileLabels, tileLabelsUseCase, gameStart,
    )
    val deleteSave = DeleteSaveUseCase(saves, catalog, analytics, currentSaveName)
    val startupRestore = StartupRestoreUseCase(
        board, catalog, analytics, ui, loadGame, newGame, mordieHooks, gameStart,
    )
    val restartGame = RestartGameUseCase(board, gameStart, mordieHooks, analytics, solution, gameShiftSources, unsavedProgress)
    return GameViewModelImpl(
        board, analytics, mordieHooks, ui, catalog, newGame, saveGame, autosave, loadGame, deleteSave, startupRestore, solution, restartGame, gameStart, tileLabelsUseCase,
        ControlsDialogUseCase(board, analytics, solution), currentSaveName, gameShiftSources, unsavedProgress, tileLabels,
        language, LanguageUseCase(board, language, analytics, solution),
    )
}
