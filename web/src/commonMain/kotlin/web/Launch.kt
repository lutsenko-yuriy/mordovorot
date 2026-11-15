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
import viewmodel.buildGameViewModel

/** The page's board: the web client alone offers [BoardSize.VERY_LARGE] (GH-207). */
fun webBoard(): BoardImpl = BoardImpl(maxSide = BoardSize.VERY_LARGE.last)

/** The page's wiring (GH-110), shared by the page's `main` and the scenario tests: tracks
 *  `app_launched` (with [appVersion] and [host], GH-111, and the tile labels kept in
 *  [preferences], GH-139), then starts a [WebGame] on [scope].
 *  Every event gets `platform: web`, and all but `app_launched` get `input_method` from
 *  [pressedWith] (GH-171). A share link in [address] starts its board (GH-243). On a [touch] screen the keys and Controls are out of reach (GH-287).
 *  [updates] checks for a new version of the page (GH-262). */
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
    address: PageAddress,
    clipboard: Clipboard,
    updates: AppUpdates,
): WebGame {
    val platform = PlatformAnalyticsService(analytics, "web")
    val tileLabels = SessionTileLabels(TileLabelsPreference(preferences))
    trackAppLaunched(platform, layout, appVersion, host, tileLabels.effectivePreference.wireName)
    val pressed = InputMethodAnalyticsService(platform) { pressedWith.inputMethod }
    val viewModel = buildGameViewModel(
        board, saves, pressed,
        mordie = mordie,
        currentSaveName = CurrentSaveName(),
        gameShiftSources = GameShiftSources(),
        unsavedProgress = UnsavedProgress(),
        solutionScreen = SolutionScreen(),
        tileLabels = tileLabels,
        ui = UiRequestChannel(),
    )
    val keyboardLayout = SessionKeyboardLayout(KeyboardLayoutPreference(preferences))
    return WebGame(viewModel, pressed, renderer, scope, pressedWith, keyboardLayout, !touch, address, clipboard, updates, appVersion, ShareTogglePreference(preferences, "share_labels"), ShareTogglePreference(preferences, "share_progress")).also { it.start() }
}

fun trackAppLaunched(analytics: AnalyticsService, layout: Layout, appVersion: String, host: Host, tileLabels: String) {
    analytics.track(
        "app_launched",
        mapOf(
            "layout" to layout.name.lowercase(),
            "app_version" to appVersion,
            "host" to host.name.lowercase(),
            "tile_labels" to tileLabels,
        ),
    )
}
