import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import board_model.BoardModel
import mordie.Mordie
import storage.SaveRepository
import view.View
import view.tui.KeyboardInput
import view.tui.MouseInput
import view.tui.ScreenRenderer
import view.tui.Terminal
import view.tui.TuiInput
import view.tui.TuiView
import viewmodel.ModeSwitcher
import viewmodel.ModeSwitcherImpl
import viewmodel.MordieHooks
import viewmodel.SaveCatalog
import viewmodel.UiRequestChannel
import viewmodel.ViewModel
import viewmodel.ViewModelImpl
import viewmodel.usecase.ExitGameUseCase
import viewmodel.usecase.LoadGameUseCase
import viewmodel.usecase.NewGameUseCase
import viewmodel.usecase.ResolveUnsolvableSaveUseCase
import viewmodel.usecase.SaveGameUseCase

// The composition root (GH-78): the only production code that constructs collaborators. `main`
// builds the session-lifetime objects; everything below is rebuilt per mode. Test builders call
// these same functions with fakes, so tests and production can't wire things differently.

/** A [GameSession] whose every rebuild shares [board], [saves] and [mordie]. */
internal fun buildGameSession(
    initialMode: InputMode,
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    sizeChosenAtLaunch: Boolean,
    mordie: Mordie,
    terminalFactory: () -> Terminal,
): GameSession = GameSession(initialMode, mordie) { mode, startupRestoreDone ->
    defaultView(mode, board, saves, analytics, startupRestoreDone, sizeChosenAtLaunch, mordie, terminalFactory)
}

/** The production `View` for one [mode]. [analytics] is undecorated: the viewModel and View get
 *  it decorated with `input_method`, the [ModeSwitcherImpl] doesn't (see [ModeSwitcher]). */
internal fun defaultView(
    mode: InputMode,
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    startupRestoreDone: Boolean,
    sizeChosenAtLaunch: Boolean,
    mordie: Mordie,
    terminalFactory: () -> Terminal,
): View {
    val decoratedAnalytics = InputMethodAnalyticsService(analytics, mode.name.lowercase())
    val viewModel = buildViewModel(board, saves, decoratedAnalytics, startupRestoreDone, sizeChosenAtLaunch, mordie)
    return buildTuiView(
        terminalFactory(),
        decoratedAnalytics,
        if (mode == InputMode.KEYBOARD) KeyboardInput() else MouseInput(),
        ModeSwitcherImpl(currentMode = mode, analytics = analytics),
        viewModel,
    )
}

/** [MordieHooks] is built here, per rebuild, so it tracks through this mode's [analytics]. The
 *  one [UiRequestChannel] is shared by the viewModel and every use case. */
internal fun buildViewModel(
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    startupRestoreDone: Boolean,
    sizeChosenAtLaunch: Boolean,
    mordie: Mordie,
): ViewModelImpl {
    val mordieHooks = MordieHooks(mordie, board, analytics)
    val ui = UiRequestChannel()
    val catalog = SaveCatalog(saves)
    val newGame = NewGameUseCase(board, mordieHooks, analytics)
    val saveGame = SaveGameUseCase(board, saves, mordieHooks, analytics, ui)
    val exitGame = ExitGameUseCase(saves, saveGame, analytics, ui)
    val resolveUnsolvable = ResolveUnsolvableSaveUseCase(board, saves, mordieHooks, analytics, ui, newGame, exitGame)
    return ViewModelImpl(
        board,
        analytics,
        startupRestoreDone,
        sizeChosenAtLaunch,
        mordieHooks,
        ui,
        catalog,
        newGame,
        saveGame,
        LoadGameUseCase(board, saves, mordieHooks, analytics, ui, catalog, resolveUnsolvable),
        exitGame,
    )
}

internal fun buildTuiView(
    terminal: Terminal,
    analytics: AnalyticsService,
    input: TuiInput,
    modeSwitcher: ModeSwitcher,
    viewModel: ViewModel,
): TuiView = TuiView(terminal, analytics, input, modeSwitcher, viewModel, ScreenRenderer())
