package testing

import analytics.AnalyticsService
import board_model.BoardModel
import mordie.Mordie
import mordie.MordieSearchFactory
import storage.SaveRepository
import viewmodel.ViewModelImpl

/** The one place tests build a [ViewModelImpl] (GH-78): defaults are fakes, never real
 *  implementations, so production constructors can drop their own defaults. */
fun testViewModel(
    board: BoardModel = FakeBoardModel(),
    saves: SaveRepository = FakeSaveRepository(),
    analytics: AnalyticsService = RecordingAnalyticsService(),
    startupRestoreDone: Boolean = false,
    sizeChosenAtLaunch: Boolean = false,
    mordie: Mordie = Mordie(MordieSearchFactory.NONE),
): ViewModelImpl = ViewModelImpl(board, saves, analytics, startupRestoreDone, sizeChosenAtLaunch, mordie)
