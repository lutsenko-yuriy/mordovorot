import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardImpl
import board_model.BoardModel
import presenter.ConsolePresenterImpl
import presenter.ModeSwitchRequestedException
import presenter.TuiPresenterImpl
import storage.FileSaveRepository
import storage.SaveRepository
import view.View
import view.ViewImpl
import view.tui.AnsiTerminal
import view.tui.KeyboardInput
import view.tui.Terminal
import view.tui.TuiView

/** The session loop extracted out of [main] for GH-30: owns the [BoardModel]/[SaveRepository]
 *  that survive every mode switch, rebuilds `View`+presenter+analytics per [InputMode] on
 *  [ModeSwitchRequestedException], and stops when `play()` returns normally. [buildView] is the
 *  test seam; it defaults to [defaultView]'s real wiring. */
class GameSession(
    initialMode: InputMode,
    private val board: BoardModel = BoardImpl(),
    private val saves: SaveRepository = FileSaveRepository(),
    private val analytics: AnalyticsService = NoopAnalyticsService(),
    private val terminalFactory: () -> Terminal = { AnsiTerminal() },
    private val buildView: (
        mode: InputMode,
        board: BoardModel,
        saves: SaveRepository,
        analytics: AnalyticsService,
        startupRestoreDone: Boolean,
    ) -> View = { mode, b, s, a, restoreDone -> defaultView(mode, b, s, a, restoreDone, terminalFactory) },
) {

    private var mode: InputMode = initialMode

    /** `true` after the first `View` build, so a mode switch never re-shows the startup restore
     *  prompt. */
    private var startupRestoreDone = false

    fun run() {
        while (true) {
            val decoratedAnalytics = InputMethodAnalyticsService(analytics, mode.name.lowercase())
            val view = buildView(mode, board, saves, decoratedAnalytics, startupRestoreDone)
            try {
                view.play()
                return
            } catch (e: ModeSwitchRequestedException) {
                mode = e.target
                startupRestoreDone = true
            }
        }
    }
}

/** The production `View` wiring, one per [InputMode] - what `main` built directly before GH-30. */
private fun defaultView(
    mode: InputMode,
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    startupRestoreDone: Boolean,
    terminalFactory: () -> Terminal,
): View =
    when (mode) {
        InputMode.CONSOLE ->
            ViewImpl.create { v -> ConsolePresenterImpl(v, board, saves, analytics, startupRestoreDone) }
        InputMode.MOUSE ->
            TuiView.create(terminalFactory(), analytics = analytics) { v ->
                TuiPresenterImpl(v, board, saves, analytics, startupRestoreDone)
            }
        InputMode.KEYBOARD ->
            TuiView.create(terminalFactory(), analytics = analytics, input = KeyboardInput()) { v ->
                TuiPresenterImpl(v, board, saves, analytics, startupRestoreDone)
            }
    }
