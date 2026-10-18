package testing

import analytics.AnalyticsService
import board_model.BoardModel
import buildViewModel
import mordie.Mordie
import mordie.MordieSearchFactory
import storage.SaveRepository
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.SolutionScreen
import viewmodel.UnsavedProgress
import viewmodel.ViewModelImpl

/** The one place tests build a [ViewModelImpl] (GH-78): the production wiring with fake
 *  defaults. */
internal fun testViewModel(
    board: BoardModel = FakeBoardModel(),
    saves: SaveRepository = FakeSaveRepository(),
    analytics: AnalyticsService = RecordingAnalyticsService(),
    startupRestoreDone: Boolean = false,
    sizeChosenAtLaunch: Boolean = false,
    mordie: Mordie = Mordie(MordieSearchFactory.NONE),
    currentSaveName: CurrentSaveName = CurrentSaveName(),
    gameShiftSources: GameShiftSources = GameShiftSources(),
    unsavedProgress: UnsavedProgress = UnsavedProgress(),
    solutionScreen: SolutionScreen = SolutionScreen(),
): ViewModelImpl =
    buildViewModel(board, saves, analytics, startupRestoreDone, sizeChosenAtLaunch, mordie, currentSaveName, gameShiftSources,
        unsavedProgress, solutionScreen,
    )
