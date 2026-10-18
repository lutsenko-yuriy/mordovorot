package web

import analytics.AnalyticsService
import analytics.InputMethodAnalyticsService
import analytics.PlatformAnalyticsService
import board_model.BoardModel
import kotlinx.coroutines.CoroutineScope
import mordie.Mordie
import storage.SaveRepository
import viewmodel.CurrentSaveName
import viewmodel.GameShiftSources
import viewmodel.UiRequestChannel
import viewmodel.UnsavedProgress
import viewmodel.UnsolvableSaveExit
import viewmodel.buildGameViewModel

/** The page's wiring (GH-110), shared by the page's `main` and the scenario tests: tracks
 *  `app_launched`, then starts a [WebGame] on [scope]. Every event gets `platform: web`, and all
 *  but `app_launched` get `input_method: rich_ui`. */
fun startWebGame(
    board: BoardModel,
    saves: SaveRepository,
    analytics: AnalyticsService,
    mordie: Mordie,
    renderer: WebRenderer,
    scope: CoroutineScope,
    layout: Layout,
): WebGame {
    val platform = PlatformAnalyticsService(analytics, "web")
    trackAppLaunched(platform, layout)
    val richUi = InputMethodAnalyticsService(platform, "rich_ui")
    val viewModel = buildGameViewModel(
        board, saves, richUi,
        startupRestoreDone = false,
        sizeChosenAtLaunch = false,
        mordie = mordie,
        currentSaveName = CurrentSaveName(),
        gameShiftSources = GameShiftSources(),
        unsavedProgress = UnsavedProgress(),
        ui = UiRequestChannel(),
        // The page has no Exit, so it never offers one.
        unsolvableExit = UnsolvableSaveExit.NONE,
    )
    return WebGame(viewModel, richUi, renderer, scope).also { it.start() }
}

fun trackAppLaunched(analytics: AnalyticsService, layout: Layout) {
    analytics.track("app_launched", mapOf("mode" to "rich_ui", "layout" to layout.name.lowercase()))
}
