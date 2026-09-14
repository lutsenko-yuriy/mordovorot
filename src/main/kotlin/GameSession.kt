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

/**
 * The session loop extracted out of [main] for GH-30: owns the one [BoardModel] and one
 * [SaveRepository] that survive every mode switch, builds a fresh `View` + presenter +
 * [InputMethodAnalyticsService] around them for the current [InputMode] on every iteration, runs
 * its `play()`, and - on [ModeSwitchRequestedException] - rebuilds for the requested mode instead
 * of ending the session. `play()` returning normally (an ordinary exit) ends [run] the same way.
 *
 * [buildView] is the one seam a test needs to drive this loop without any real console/terminal
 * I/O - it defaults to the production wiring ([defaultView]), which mirrors what `main` used to
 * do directly before GH-30.
 */
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

    /** `false` only for the very first `View` this session builds - every rebuild after a mode
     *  switch passes `true`, so [presenter.BasePresenter.offerStartupRestore] never re-shows the
     *  startup restore prompt on a mode switch (GH-30). */
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

/** The production `View` wiring - one `View`/presenter/`TuiInput` combination per [InputMode],
 *  same as `main` built directly before GH-30 extracted this loop out into [GameSession]. */
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
