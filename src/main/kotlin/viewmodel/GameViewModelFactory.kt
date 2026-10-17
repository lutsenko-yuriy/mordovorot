package viewmodel

import analytics.AnalyticsService
import board_model.BoardModel
import mordie.Mordie
import storage.SaveRepository
import viewmodel.usecase.LoadGameUseCase
import viewmodel.usecase.NewGameUseCase
import viewmodel.usecase.ResolveUnsolvableSaveUseCase
import viewmodel.usecase.SaveGameUseCase
import viewmodel.usecase.StartupRestoreUseCase

/** Builds a [GameViewModelImpl] and its use cases (GH-41) - the only way a platform gets one.
 *  [MordieHooks] is built here, per call, so it tracks through this [analytics]. [ui] is shared
 *  by the viewModel and every use case, and with any platform flow (the terminal's Exit) that
 *  raises requests to the same View. */
fun buildGameViewModel(
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    startupRestoreDone: Boolean,
    sizeChosenAtLaunch: Boolean,
    mordie: Mordie,
    currentSaveName: CurrentSaveName,
    gameShiftKeys: GameShiftKeys,
    ui: UiRequestChannel,
    unsolvableExit: UnsolvableSaveExit,
): GameViewModelImpl {
    val mordieHooks = MordieHooks(mordie, board, analytics)
    val catalog = SaveCatalog(saves)
    val newGame = NewGameUseCase(board, mordieHooks, analytics, currentSaveName, gameShiftKeys)
    val saveGame = SaveGameUseCase(board, saves, mordieHooks, analytics, ui, currentSaveName)
    val resolveUnsolvable = ResolveUnsolvableSaveUseCase(board, saves, mordieHooks, analytics, ui, newGame, currentSaveName, gameShiftKeys)
    val loadGame = LoadGameUseCase(board, saves, mordieHooks, analytics, ui, catalog, resolveUnsolvable, currentSaveName, gameShiftKeys)
    val startupRestore = StartupRestoreUseCase(
        board, catalog, analytics, ui, loadGame, newGame, mordieHooks, startupRestoreDone, sizeChosenAtLaunch,
    )
    return GameViewModelImpl(
        board, analytics, mordieHooks, ui, catalog, newGame, saveGame, loadGame, startupRestore, currentSaveName,
        gameShiftKeys, unsolvableExit,
    )
}
