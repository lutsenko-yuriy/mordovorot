package viewmodel

import analytics.AnalyticsService
import board_model.BoardModel
import mordie.Mordie
import storage.SaveRepository
import viewmodel.usecase.LoadGameUseCase
import viewmodel.usecase.NewGameUseCase
import viewmodel.usecase.ResolveUnsolvableSaveUseCase
import viewmodel.usecase.SaveGameUseCase
import viewmodel.usecase.SolutionUseCase
import viewmodel.usecase.StartupRestoreUseCase
import viewmodel.usecase.TileLabelsUseCase

/** Builds a [GameViewModelImpl] and its use cases (GH-41) - the only way a platform gets one.
 *  [MordieHooks] is built here, per call, so it tracks through this [analytics]. [ui] is shared
 *  by the viewModel and every use case, and with any platform flow (the terminal's Exit) that
 *  raises requests to the same View. [solutionScreen] (GH-126) and [tileLabels] (GH-139) are the
 *  session's. */
fun buildGameViewModel(
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    startupRestoreDone: Boolean,
    sizeChosenAtLaunch: Boolean,
    mordie: Mordie,
    currentSaveName: CurrentSaveName,
    gameShiftSources: GameShiftSources,
    unsavedProgress: UnsavedProgress,
    solutionScreen: SolutionScreen,
    tileLabels: SessionTileLabels,
    ui: UiRequestChannel,
    unsolvableExit: UnsolvableSaveExit,
): GameViewModelImpl {
    val mordieHooks = MordieHooks(mordie, board, analytics, solutionScreen)
    val solution = SolutionUseCase(board, mordieHooks, analytics, solutionScreen, unsavedProgress)
    val catalog = SaveCatalog(saves)
    val newGame = NewGameUseCase(board, mordieHooks, analytics, currentSaveName, gameShiftSources, unsavedProgress, solution, tileLabels)
    val saveGame = SaveGameUseCase(board, saves, mordieHooks, analytics, ui, currentSaveName, unsavedProgress, tileLabels)
    val resolveUnsolvable = ResolveUnsolvableSaveUseCase(
        board, saves, mordieHooks, analytics, ui, newGame, currentSaveName, gameShiftSources, unsavedProgress, solution, tileLabels,
    )
    val loadGame = LoadGameUseCase(
        board, saves, mordieHooks, analytics, ui, catalog, resolveUnsolvable, currentSaveName, gameShiftSources, unsavedProgress,
        solution, tileLabels,
    )
    val startupRestore = StartupRestoreUseCase(
        board, catalog, analytics, ui, loadGame, newGame, mordieHooks, startupRestoreDone, sizeChosenAtLaunch,
    )
    val tileLabelsUseCase = TileLabelsUseCase(board, tileLabels, analytics, solution)
    return GameViewModelImpl(
        board, analytics, mordieHooks, ui, catalog, newGame, saveGame, loadGame, startupRestore, solution, tileLabelsUseCase,
        currentSaveName, gameShiftSources, unsavedProgress, tileLabels, unsolvableExit,
    )
}
