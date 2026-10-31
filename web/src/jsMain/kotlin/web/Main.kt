package web

import analytics.NoopAnalyticsService
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import mordie.BackgroundMordieSearch
import mordie.Mordie
import org.w3c.dom.HTMLElement
import web.a11y.DomAnnouncer
import web.a11y.listenForKeys
import web.storage.BrowserPreferenceRepository
import web.storage.BrowserSaveRepository
import web.storage.LocalStorageStore

/** The page's composition root (GH-110). */
fun main() {
    // First, while the root's font size is still the browser's default.
    applySystemTextSize(document, window)
    val root = document.getElementById("app") as? HTMLElement ?: return
    stopPinchZoom(document, window.navigator)
    // Outside #app, which DomRenderer rebuilds: the footer never changes.
    document.body?.append(footerElement(footer(BuildInfo.VERSION)))
    val renderer = DomRenderer(root, document.getElementById("announcer")?.let(::DomAnnouncer))
    val pressedWith = PressedWith()
    val game = startWebGame(
        webBoard(),
        BrowserSaveRepository(LocalStorageStore()),
        BrowserPreferenceRepository(LocalStorageStore()),
        NoopAnalyticsService(),
        Mordie(BackgroundMordieSearch.Factory()),
        renderer,
        MainScope(),
        layoutOf(window.innerWidth, window.innerHeight),
        BuildInfo.VERSION,
        hostOf(window.location.hostname),
        touch = window.matchMedia("(pointer: coarse)").matches,
        pressedWith,
    )
    renderer.game = game
    listenForKeys(root)
    listenForEscape(root) { game }
    listenForPlayKeys(root, pressedWith) { game }
    // The browser's own "Leave site?" prompt, only while some progress isn't in a save.
    window.addEventListener("beforeunload", { event ->
        if (game.hasUnsavedProgress()) {
            event.preventDefault()
            // Older browsers show the prompt only when this is set, and not empty, as well.
            event.asDynamic().returnValue = true
        }
    })
}
