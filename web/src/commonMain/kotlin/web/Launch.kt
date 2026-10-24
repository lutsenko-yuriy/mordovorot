package web

import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import analytics.PlatformAnalyticsService
import board_model.BoardModel
import kotlinx.coroutines.CoroutineScope
import mordie.Mordie
import storage.PreferenceRepository
import storage.SaveRepository
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.SessionTileLabels
import viewmodel.TileLabelsPreference
import viewmodel.SolutionScreen
import viewmodel.UiRequestChannel
import viewmodel.UnsavedProgress
import viewmodel.UnsolvableSaveExit
import viewmodel.buildGameViewModel

/** The page's wiring (GH-110), shared by the page's `main` and the scenario tests: tracks
 *  `app_launched` (with [appVersion] and [host], GH-111, and the tile labels kept in
 *  [preferences], GH-139), then starts a [WebGame] on [scope].
 *  Every event gets `platform: web`, and all but `app_launched` get `input_method: rich_ui`. */
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
): WebGame {
    val platform = PlatformAnalyticsService(analytics, "web")
    val tileLabels = SessionTileLabels(TileLabelsPreference(preferences))
    trackAppLaunched(platform, layout, appVersion, host, tileLabels.effectivePreference.wireName)
    val richUi = InputMethodAnalyticsService(platform, "rich_ui")
    val viewModel = buildGameViewModel(
        board, saves, richUi,
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
    return WebGame(viewModel, richUi, renderer, scope).also { it.start() }
}

fun trackAppLaunched(analytics: AnalyticsService, layout: Layout, appVersion: String, host: Host, tileLabels: String) {
    analytics.track(
        "app_launched",
        mapOf(
            "mode" to "rich_ui",
            "layout" to layout.name.lowercase(),
            "app_version" to appVersion,
            "host" to host.name.lowercase(),
            "tile_labels" to tileLabels,
        ),
    )
}
