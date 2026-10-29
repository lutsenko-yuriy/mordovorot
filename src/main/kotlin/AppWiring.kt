import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import analytics.PlatformAnalyticsService
import board_model.BoardModel
import mordie.Mordie
import storage.SaveRepository
import view.View
import view.tui.KeyboardInput
import view.tui.KeyboardSelection
import view.tui.MouseInput
import view.tui.ScreenRenderer
import view.tui.Terminal
import view.tui.TuiInput
import view.tui.TuiScreen
import view.tui.ToolbarRouter
import view.tui.TuiView
import view.tui.UiRequestRouter
import view.tui.dialog.ControlsDialogController
import view.tui.dialog.ExitDialogController
import view.tui.dialog.GiveUpDialogController
import view.tui.dialog.LabelsDialogController
import view.tui.dialog.LoadDialogController
import view.tui.dialog.RestoreDialogController
import view.tui.dialog.SaveDialogController
import view.tui.dialog.SizeDialogController
import view.tui.dialog.UnsolvableDialogController
import viewmodel.CurrentSaveName
import viewmodel.ExitRequestedException
import viewmodel.GameShiftSources
import viewmodel.ModeSwitcher
import viewmodel.ModeSwitcherImpl
import viewmodel.SessionTileLabels
import viewmodel.SolutionScreen
import viewmodel.UiRequestChannel
import viewmodel.UnsavedProgress
import viewmodel.UnsolvableSaveExit
import viewmodel.ViewModel
import viewmodel.ViewModelImpl
import viewmodel.buildGameViewModel
import viewmodel.usecase.ExitGameUseCase

// The composition root (GH-78): the only production code that constructs collaborators. `main`
// builds the session-lifetime objects; everything below is rebuilt per mode. Test builders call
// these same functions with fakes, so tests and production can't wire things differently.

/** The terminal's outermost analytics (GH-110): every event, the launch and mode-switch ones
 *  included, carries `platform: terminal`. */
internal fun terminalAnalytics(delegate: AnalyticsService): AnalyticsService =
    PlatformAnalyticsService(delegate, "terminal")

/** A [GameSession] whose every rebuild shares [board], [saves], [mordie], [tileLabels] (GH-139),
 *  one [CurrentSaveName] (GH-99), one [GameShiftSources] and one [KeyboardSelection] (GH-170), one
 *  [UnsavedProgress] (GH-110), which the terminal never reads, and one [SolutionScreen] (GH-126). */
internal fun buildGameSession(
    initialMode: InputMode,
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    sizeChosenAtLaunch: Boolean,
    mordie: Mordie,
    tileLabels: SessionTileLabels,
    terminalFactory: () -> Terminal,
): GameSession {
    val currentSaveName = CurrentSaveName()
    val gameShiftSources = GameShiftSources()
    val unsavedProgress = UnsavedProgress()
    val solutionScreen = SolutionScreen()
    val keyboardSelection = KeyboardSelection()
    return GameSession(initialMode, mordie) { mode, startupRestoreDone ->
        defaultView(
            mode, board, saves, analytics, startupRestoreDone, sizeChosenAtLaunch, mordie, currentSaveName, gameShiftSources,
            unsavedProgress, solutionScreen, tileLabels, keyboardSelection, terminalFactory,
        )
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
    gameShiftSources: GameShiftSources,
    unsavedProgress: UnsavedProgress,
    solutionScreen: SolutionScreen,
    tileLabels: SessionTileLabels,
    keyboardSelection: KeyboardSelection,
    terminalFactory: () -> Terminal,
): View {
    val decoratedAnalytics = InputMethodAnalyticsService(analytics, mode.name.lowercase())
    val viewModel = buildViewModel(
        board, saves, decoratedAnalytics, startupRestoreDone, sizeChosenAtLaunch, mordie, currentSaveName, gameShiftSources,
        unsavedProgress, solutionScreen, tileLabels,
    )
    return buildTuiView(
        terminalFactory(),
        decoratedAnalytics,
        if (mode == InputMode.KEYBOARD) KeyboardInput(keyboardSelection) else MouseInput(),
        ModeSwitcherImpl(currentMode = mode, analytics = analytics),
        viewModel,
    )
}

/** The terminal viewModel: [buildGameViewModel] plus Exit (GH-41), which raises its requests on
 *  the same [UiRequestChannel]. Exit for an unsolvable save quits at once at startup, where
 *  there's no game to save, and runs the Exit flow mid-game. */
internal fun buildViewModel(
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    startupRestoreDone: Boolean,
    sizeChosenAtLaunch: Boolean,
    mordie: Mordie,
    currentSaveName: CurrentSaveName,
    gameShiftSources: GameShiftSources,
    unsavedProgress: UnsavedProgress,
    solutionScreen: SolutionScreen,
    tileLabels: SessionTileLabels,
): ViewModelImpl {
    val ui = UiRequestChannel()
    val exitGame = ExitGameUseCase(saves, analytics, ui)
    val unsolvableExit = UnsolvableSaveExit { viewModel, atStartup ->
        if (atStartup) throw ExitRequestedException()
        exitGame.exitGame(viewModel::saveGame)
    }
    val game = buildGameViewModel(
        board, saves, analytics, startupRestoreDone, sizeChosenAtLaunch, mordie, currentSaveName, gameShiftSources,
        unsavedProgress, solutionScreen, tileLabels, ui, unsolvableExit,
    )
    return ViewModelImpl(game, exitGame)
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
    val labelsDialog = LabelsDialogController(screen, viewModel)
    val requests = UiRequestRouter(
        screen,
        saveDialog,
        exitDialog,
        RestoreDialogController(loadDialog),
        sizeDialog,
        UnsolvableDialogController(screen, analytics),
        labelsDialog,
    )
    val giveUpDialog = GiveUpDialogController(screen, analytics, viewModel)
    val controlsDialog = ControlsDialogController(screen, viewModel, modeSwitcher)
    val toolbar = ToolbarRouter(saveDialog, loadDialog, exitDialog, sizeDialog, giveUpDialog, labelsDialog, controlsDialog, viewModel)
    return TuiView(terminal, analytics, input, viewModel, screen, requests, toolbar)
}
