package testing

import analytics.AnalyticsService
import board_model.BoardModel
import mordie.Mordie
import mordie.MordieSearchFactory
import storage.SaveRepository
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.GameViewModelImpl
import viewmodel.LanguagePreference
import viewmodel.SessionLanguage
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
    /** English unless a test says otherwise: building sets the global language every time (GH-275). */
    language: SessionLanguage = SessionLanguage(LanguagePreference(FakePreferenceRepository()), emptyList()),
): GameViewModelImpl = buildGameViewModel(
    board, saves, analytics, mordie, currentSaveName, gameShiftSources,
    unsavedProgress, solutionScreen, tileLabels, language, UiRequestChannel(),
)
