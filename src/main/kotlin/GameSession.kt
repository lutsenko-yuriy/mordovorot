import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import analytics.NoopAnalyticsService
import board_model.BoardImpl
import board_model.BoardModel
import storage.FileSaveRepository
import storage.SaveRepository
import view.View
import view.ViewImpl
import view.tui.AnsiTerminal
import view.tui.KeyboardInput
import view.tui.Terminal
import view.tui.TuiView
import viewmodel.ModeSwitchRequestedException
import viewmodel.ModeSwitcherImpl
import viewmodel.catchingNonControl
import viewmodel.ViewModelImpl

/** The session loop extracted out of [main] for GH-30: owns the [BoardModel]/[SaveRepository]
 *  that survive every mode switch, rebuilds `View`+viewModel+analytics per [InputMode] on
 *  [ModeSwitchRequestedException], and stops when `play()` returns normally. [buildView] is the
 *  test seam; it defaults to [defaultView]'s real wiring. */
class GameSession(
    initialMode: InputMode,
    private val board: BoardModel = BoardImpl(),
    private val saves: SaveRepository = FileSaveRepository(),
    private val analytics: AnalyticsService = NoopAnalyticsService(),
    /** `true` when `--size=N` was given at launch (GH-44) - threaded into every rebuilt
     *  [ViewModelImpl] the same way `startupRestoreDone` already is, so its startup size/restore
     *  prompts stay skipped across a mode switch too. */
    private val sizeChosenAtLaunch: Boolean = false,
    private val terminalFactory: () -> Terminal = { AnsiTerminal() },
    private val buildView: (
        mode: InputMode,
        board: BoardModel,
        saves: SaveRepository,
        analytics: AnalyticsService,
        startupRestoreDone: Boolean,
        sizeChosenAtLaunch: Boolean,
    ) -> View = { mode, b, s, a, restoreDone, sizeAtLaunch ->
        defaultView(mode, b, s, a, restoreDone, sizeAtLaunch, terminalFactory)
    },
) {

    private var mode: InputMode = initialMode

    /** `true` after the first `View` build, so a mode switch never re-shows the startup restore
     *  prompt. */
    private var startupRestoreDone = false

    suspend fun run() {
        var isFirstSession = true
        while (true) {
            // Other control-flow exceptions must reach main (PR #37, PR #43).
            try {
                catchingNonControl(
                    onError = { e ->
                        // Failed rebuild (e.g. no `stty`): fall back to console to keep the game,
                        // unless this is startup or console itself failed.
                        if (isFirstSession || mode == InputMode.CONSOLE) throw e
                        System.err.println(
                            "Could not switch to ${mode.name.lowercase()} mode (${e.message ?: e::class.simpleName}) " +
                                "- falling back to console."
                        )
                        mode = InputMode.CONSOLE
                        startupRestoreDone = true
                    },
                ) {
                    // Undecorated analytics - defaultView decorates per mode. buildView is guarded too,
                    // so a rebuild failure falls back like a play() failure (PR #37).
                    val view = buildView(mode, board, saves, analytics, startupRestoreDone, sizeChosenAtLaunch)
                    view.play()
                    return
                }
            } catch (e: ModeSwitchRequestedException) {
                mode = e.target
                startupRestoreDone = true
            }
            isFirstSession = false
        }
    }
}

/** The production `View` wiring, one per [InputMode] - what `main` built directly before GH-30.
 *  [analytics] is the undecorated service; decorated here per-mode for the viewModel/View's own
 *  tracked events. Internal, not private, so a test can verify the CONSOLE branch actually wires
 *  a real `ModeSwitcher` rather than silently defaulting to a no-op (audit finding on PR #37). */
internal fun defaultView(
    mode: InputMode,
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    startupRestoreDone: Boolean,
    sizeChosenAtLaunch: Boolean,
    terminalFactory: () -> Terminal,
): View {
    val decoratedAnalytics = InputMethodAnalyticsService(analytics, mode.name.lowercase())
    // Built once per rebuild and handed to the View - the viewModel no longer needs a View to
    // exist first (GH-42 WU3: ViewModelImpl holds no View reference at all).
    val viewModel = ViewModelImpl(board, saves, decoratedAnalytics, startupRestoreDone, sizeChosenAtLaunch)
    val modeSwitcherFactory = { v: View -> ModeSwitcherImpl(view = v, currentMode = mode, analytics = analytics) }
    return when (mode) {
        InputMode.CONSOLE ->
            ViewImpl.create(modeSwitcherFactory = modeSwitcherFactory, viewModel = viewModel, analytics = decoratedAnalytics)
        InputMode.MOUSE ->
            TuiView.create(
                terminalFactory(),
                analytics = decoratedAnalytics,
                modeSwitcherFactory = modeSwitcherFactory,
                viewModel = viewModel,
            )
        InputMode.KEYBOARD ->
            TuiView.create(
                terminalFactory(),
                analytics = decoratedAnalytics,
                input = KeyboardInput(),
                modeSwitcherFactory = modeSwitcherFactory,
                viewModel = viewModel,
            )
    }
}
