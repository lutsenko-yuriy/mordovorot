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
import view.tui.TuiScreen
import view.tui.ToolbarRouter
import view.tui.TuiView
import view.tui.UiRequestRouter
import view.tui.dialog.ExitDialogController
import view.tui.dialog.LoadDialogController
import view.tui.dialog.RestoreDialogController
import view.tui.dialog.SaveDialogController
import view.tui.dialog.SizeDialogController
import view.tui.dialog.UnsolvableDialogController
import viewmodel.CurrentSaveName
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
import viewmodel.usecase.StartupRestoreUseCase

// The composition root (GH-78): the only production code that constructs collaborators. `main`
// builds the session-lifetime objects; everything below is rebuilt per mode. Test builders call
// these same functions with fakes, so tests and production can't wire things differently.

/** A [GameSession] whose every rebuild shares [board], [saves], [mordie] and one
 *  [CurrentSaveName] (GH-99). */
internal fun buildGameSession(
    initialMode: InputMode,
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    sizeChosenAtLaunch: Boolean,
    mordie: Mordie,
    terminalFactory: () -> Terminal,
): GameSession {
    val currentSaveName = CurrentSaveName()
    return GameSession(initialMode, mordie) { mode, startupRestoreDone ->
        defaultView(mode, board, saves, analytics, startupRestoreDone, sizeChosenAtLaunch, mordie, currentSaveName, terminalFactory)
    }
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
    currentSaveName: CurrentSaveName,
    terminalFactory: () -> Terminal,
): View {
    val decoratedAnalytics = InputMethodAnalyticsService(analytics, mode.name.lowercase())
    val viewModel = buildViewModel(board, saves, decoratedAnalytics, startupRestoreDone, sizeChosenAtLaunch, mordie, currentSaveName)
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
    currentSaveName: CurrentSaveName,
): ViewModelImpl {
    val mordieHooks = MordieHooks(mordie, board, analytics)
    val ui = UiRequestChannel()
    val catalog = SaveCatalog(saves)
    val newGame = NewGameUseCase(board, mordieHooks, analytics, currentSaveName)
    val saveGame = SaveGameUseCase(board, saves, mordieHooks, analytics, ui, currentSaveName)
    val exitGame = ExitGameUseCase(saves, saveGame, analytics, ui)
    val resolveUnsolvable = ResolveUnsolvableSaveUseCase(board, saves, mordieHooks, analytics, ui, newGame, exitGame, currentSaveName)
    val loadGame = LoadGameUseCase(board, saves, mordieHooks, analytics, ui, catalog, resolveUnsolvable, currentSaveName)
    val startupRestore = StartupRestoreUseCase(
        board, catalog, analytics, ui, loadGame, newGame, mordieHooks, startupRestoreDone, sizeChosenAtLaunch,
    )
    return ViewModelImpl(
        board, analytics, mordieHooks, ui, catalog, newGame, saveGame, loadGame, exitGame, startupRestore, currentSaveName,
    )
}

internal fun buildTuiView(
    terminal: Terminal,
    analytics: AnalyticsService,
    input: TuiInput,
    modeSwitcher: ModeSwitcher,
    viewModel: ViewModel,
): TuiView {
    val screen = TuiScreen(terminal, input, ScreenRenderer(), viewModel, analytics)
    val saveDialog = SaveDialogController(screen, analytics, viewModel)
    val exitDialog = ExitDialogController(screen, analytics, viewModel)
    val loadDialog = LoadDialogController(screen, analytics, viewModel)
    val sizeDialog = SizeDialogController(screen, analytics, viewModel)
    val requests = UiRequestRouter(
        screen,
        saveDialog,
        exitDialog,
        RestoreDialogController(loadDialog),
        sizeDialog,
        UnsolvableDialogController(screen, analytics),
    )
    val toolbar = ToolbarRouter(saveDialog, loadDialog, exitDialog, sizeDialog, modeSwitcher, input)
    return TuiView(terminal, analytics, input, viewModel, screen, requests, toolbar)
}
