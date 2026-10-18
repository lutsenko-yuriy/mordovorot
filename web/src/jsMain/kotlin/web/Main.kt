package web

import analytics.NoopAnalyticsService
import board_model.BoardImpl
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import mordie.BackgroundMordieSearch
import mordie.Mordie
import web.storage.BrowserSaveRepository
import web.storage.LocalStorageStore

/** The page's composition root (GH-110). */
fun main() {
    val root = document.getElementById("app") ?: return
    val renderer = DomRenderer(root)
    val game = startWebGame(
        BoardImpl(),
        BrowserSaveRepository(LocalStorageStore()),
        NoopAnalyticsService(),
        Mordie(BackgroundMordieSearch.Factory()),
        renderer,
        MainScope(),
        layoutOf(window.innerWidth, window.innerHeight),
    )
    renderer.game = game
    // The browser's own "Leave site?" prompt, only while some progress isn't in a save.
    window.addEventListener("beforeunload", { event ->
        if (game.hasUnsavedProgress()) {
            event.preventDefault()
            // Older browsers show the prompt only when this is set as well.
            event.asDynamic().returnValue = ""
        }
    })
}
