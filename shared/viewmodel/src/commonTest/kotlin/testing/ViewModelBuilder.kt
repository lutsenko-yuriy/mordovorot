package testing

import analytics.AnalyticsService
import board_model.BoardModel
import mordie.Mordie
import mordie.MordieSearchFactory
import storage.SaveRepository
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.GameViewModelImpl
import viewmodel.SessionTileLabels
import viewmodel.SolutionScreen
import viewmodel.TileLabelsPreference
import viewmodel.UiRequestChannel
import viewmodel.UnsavedProgress
import viewmodel.buildGameViewModel

/** Builds a [GameViewModelImpl] for tests (GH-41). */
internal fun testViewModel(
    board: BoardModel = FakeBoardModel(),
    saves: SaveRepository = FakeSaveRepository(),
    analytics: AnalyticsService = RecordingAnalyticsService(),
    mordie: Mordie = Mordie(MordieSearchFactory.NONE),
    currentSaveName: CurrentSaveName = CurrentSaveName(),
    gameShiftSources: GameShiftSources = GameShiftSources(),
    unsavedProgress: UnsavedProgress = UnsavedProgress(),
    solutionScreen: SolutionScreen = SolutionScreen(),
    tileLabels: SessionTileLabels = SessionTileLabels(TileLabelsPreference(FakePreferenceRepository())),
): GameViewModelImpl = buildGameViewModel(
    board, saves, analytics, mordie, currentSaveName, gameShiftSources,
    unsavedProgress, solutionScreen, tileLabels, UiRequestChannel(),
)
