package testing

import GameSession
import InputMode
import analytics.AnalyticsService
import board_model.BoardModel
import buildGameSession
import mordie.Mordie
import mordie.MordieSearchFactory
import storage.PreferenceRepository
import storage.SaveRepository
import view.View
import view.tui.Terminal
import viewmodel.KeyboardLayoutPreference
import viewmodel.SessionKeyboardLayout
import viewmodel.SessionTileLabels
import viewmodel.TileLabelsPreference

/** The one place tests build a production-wired [GameSession] (GH-78), with fake defaults. */
fun testGameSession(
    initialMode: InputMode,
    board: BoardModel = FakeBoardModel(),
    saves: SaveRepository = FakeSaveRepository(),
    analytics: AnalyticsService = RecordingAnalyticsService(),
    sizeChosenAtLaunch: Boolean = false,
    terminalFactory: () -> Terminal = { FakeTerminal() },
    mordie: Mordie = Mordie(MordieSearchFactory.NONE),
    preferences: PreferenceRepository = FakePreferenceRepository(),
): GameSession = buildGameSession(
    initialMode, board, saves, analytics, sizeChosenAtLaunch, mordie, SessionTileLabels(TileLabelsPreference(preferences)),
    SessionKeyboardLayout(KeyboardLayoutPreference(preferences)), terminalFactory,
)

/** A [GameSession] over a scripted [buildView], for tests of the session loop itself. */
fun testGameSession(
    initialMode: InputMode,
    mordie: Mordie = Mordie(MordieSearchFactory.NONE),
    buildView: (mode: InputMode, startupRestoreDone: Boolean) -> View,
): GameSession = GameSession(initialMode, mordie, buildView)
