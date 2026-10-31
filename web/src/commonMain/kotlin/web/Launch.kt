package web

import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import analytics.PlatformAnalyticsService
import board_model.BoardImpl
import board_model.BoardModel
import board_model.BoardSize
import kotlinx.coroutines.CoroutineScope
import mordie.Mordie
import storage.PreferenceRepository
import storage.SaveRepository
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.KeyboardLayoutPreference
import viewmodel.SessionKeyboardLayout
import viewmodel.SessionTileLabels
import viewmodel.TileLabelsPreference
import viewmodel.SolutionScreen
import viewmodel.UiRequestChannel
import viewmodel.UnsavedProgress
import viewmodel.UnsolvableSaveExit
import viewmodel.buildGameViewModel

/** The page's board: the web client alone offers [BoardSize.VERY_LARGE] (GH-207). */
fun webBoard(): BoardImpl = BoardImpl(maxSide = BoardSize.VERY_LARGE.last)

/** The page's wiring (GH-110), shared by the page's `main` and the scenario tests: tracks
 *  `app_launched` (with [appVersion] and [host], GH-111, and the tile labels kept in
 *  [preferences], GH-139), then starts a [WebGame] on [scope].
 *  Every event gets `platform: web`, and all but `app_launched` get `input_method` from
 *  [pressedWith] (GH-171). Keyboard mode is out of reach on a [touch] screen, whatever is
 *  stored. */
fun startWebGame(
    board: BoardModel,
    saves: SaveRepository,
    preferences: PreferenceRepository,
    analytics: AnalyticsService,
    mordie: Mordie,
    renderer: WebRenderer,
    scope: CoroutineScope,
    layout: Layout,
    appVersion: String,
    host: Host,
    touch: Boolean,
    pressedWith: PressedWith,
): WebGame {
    val platform = PlatformAnalyticsService(analytics, "web")
    val tileLabels = SessionTileLabels(TileLabelsPreference(preferences))
    val modePreference = PlayModePreference(preferences)
    val controlsShown = !touch
    val startMode = if (controlsShown) modePreference.load() ?: PlayMode.DEFAULT else PlayMode.DEFAULT
    trackAppLaunched(platform, layout, appVersion, host, tileLabels.effectivePreference.wireName, startMode)
    val pressed = InputMethodAnalyticsService(platform) { pressedWith.inputMethod }
    val viewModel = buildGameViewModel(
        board, saves, pressed,
        startupRestoreDone = false,
        sizeChosenAtLaunch = false,
        mordie = mordie,
        currentSaveName = CurrentSaveName(),
        gameShiftSources = GameShiftSources(),
        unsavedProgress = UnsavedProgress(),
        solutionScreen = SolutionScreen(),
        tileLabels = tileLabels,
        ui = UiRequestChannel(),
        // The page has no Exit, so it never offers one.
        unsolvableExit = UnsolvableSaveExit.NONE,
    )
    val keyboardLayout = SessionKeyboardLayout(KeyboardLayoutPreference(preferences))
    return WebGame(viewModel, pressed, renderer, scope, pressedWith, keyboardLayout, modePreference, startMode, controlsShown).also { it.start() }
}

fun trackAppLaunched(analytics: AnalyticsService, layout: Layout, appVersion: String, host: Host, tileLabels: String, mode: PlayMode) {
    analytics.track(
        "app_launched",
        mapOf(
            "mode" to mode.wireName,
            "layout" to layout.name.lowercase(),
            "app_version" to appVersion,
            "host" to host.name.lowercase(),
            "tile_labels" to tileLabels,
        ),
    )
}
