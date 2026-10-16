import analytics.AnalyticsService
import board_model.BoardModel
import mordie.Mordie
import mordie.MordieSearchFactory
import storage.SaveRepository
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeTerminal
import testing.RecordingAnalyticsService
import view.View
import view.tui.Terminal

/** The one place tests build a [GameSession] (GH-78), with test defaults. Without
 *  [buildView] it wires the production View over [terminalFactory]. */
fun testGameSession(
    initialMode: InputMode,
    board: BoardModel = FakeBoardModel(),
    saves: SaveRepository = FakeSaveRepository(),
    analytics: AnalyticsService = RecordingAnalyticsService(),
    sizeChosenAtLaunch: Boolean = false,
    terminalFactory: () -> Terminal = { FakeTerminal() },
    mordie: Mordie = Mordie(MordieSearchFactory.NONE),
    buildView: (
        mode: InputMode,
        board: BoardModel,
        saves: SaveRepository,
        analytics: AnalyticsService,
        startupRestoreDone: Boolean,
        sizeChosenAtLaunch: Boolean,
        mordie: Mordie,
    ) -> View = { mode, b, s, a, restoreDone, sizeAtLaunch, m ->
        defaultView(mode, b, s, a, restoreDone, sizeAtLaunch, m, terminalFactory)
    },
): GameSession = GameSession(initialMode, board, saves, analytics, sizeChosenAtLaunch, terminalFactory, mordie, buildView)
