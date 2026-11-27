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
import viewmodel.LanguagePreference
import viewmodel.SessionLanguage
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
 *  [updates] checks for a new version of the page (GH-262). [install] offers the game's installation through the footer's entry when [installOn] (GH-360). The first-launch [tutorial] runs when it is on (GH-288).
 *  The session speaks the stored language, else the first of [browserLanguages] that is ours, when [languages] is on (GH-275). */
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
    install: AppInstall,
    installOn: Boolean,
    tutorial: Boolean,
    browserLanguages: List<String>,
    languages: Boolean,
): WebGame {
    val platform = PlatformAnalyticsService(analytics, "web")
    val tileLabels = SessionTileLabels(TileLabelsPreference(preferences))
    val language = SessionLanguage(LanguagePreference(preferences), browserLanguages, enabled = languages)
    trackAppLaunched(platform, layout, appVersion, host, tileLabels.effectivePreference.wireName, language.current.code, if (install.standalone) "standalone" else "browser")
    val pressed = InputMethodAnalyticsService(platform) { pressedWith.inputMethod }
    val viewModel = buildGameViewModel(
        board, saves, pressed,
        mordie = mordie,
        currentSaveName = CurrentSaveName(),
        gameShiftSources = GameShiftSources(),
        unsavedProgress = UnsavedProgress(),
        solutionScreen = SolutionScreen(),
        tileLabels = tileLabels,
        language = language,
        ui = UiRequestChannel(),
    )
    val keyboardLayout = SessionKeyboardLayout(KeyboardLayoutPreference(preferences))
    return WebGame(viewModel, pressed, renderer, scope, pressedWith, keyboardLayout, !touch, address, clipboard, updates, install, installOn, appVersion, ShareTogglePreference(preferences, "share_labels"), ShareTogglePreference(preferences, "share_progress"), TutorialPreference(preferences), tutorial).also { it.start() }
}

fun trackAppLaunched(analytics: AnalyticsService, layout: Layout, appVersion: String, host: Host, tileLabels: String, language: String, displayMode: String) {
    analytics.track(
        "app_launched",
        mapOf(
            "layout" to layout.name.lowercase(),
            "app_version" to appVersion,
            "host" to host.name.lowercase(),
            "tile_labels" to tileLabels,
            "language" to language,
            "display_mode" to displayMode,
        ),
    )
}
