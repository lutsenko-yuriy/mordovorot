package testing

import analytics.AnalyticsService
import board_model.BoardModel
import mordie.Mordie
import mordie.MordieSearchFactory
import storage.SaveRepository
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.GameViewModelImpl
import viewmodel.UiRequestChannel
import viewmodel.UnsolvableSaveExit
import viewmodel.buildGameViewModel

/** The shared module's [testViewModel] (GH-41): the same name and parameters as the terminal
 *  app's, so tests moved here compile unchanged, building a [GameViewModelImpl] with no Exit. */
internal fun testViewModel(
    board: BoardModel = FakeBoardModel(),
    saves: SaveRepository = FakeSaveRepository(),
    analytics: AnalyticsService = RecordingAnalyticsService(),
    startupRestoreDone: Boolean = false,
    sizeChosenAtLaunch: Boolean = false,
    mordie: Mordie = Mordie(MordieSearchFactory.NONE),
    currentSaveName: CurrentSaveName = CurrentSaveName(),
    gameShiftSources: GameShiftSources = GameShiftSources(),
): GameViewModelImpl = buildGameViewModel(
    board, saves, analytics, startupRestoreDone, sizeChosenAtLaunch, mordie, currentSaveName, gameShiftSources,
    UiRequestChannel(), UnsolvableSaveExit.NONE,
)
