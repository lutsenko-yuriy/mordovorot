package web

import analytics.NoopAnalyticsService
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
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
    // Outside #app, which DomRenderer rebuilds: it fills this footer in, and redraws it for the New version available notice.
    document.body?.append(element("footer", "footer"))
    val renderer = DomRenderer(root, document.getElementById("announcer")?.let(::DomAnnouncer))
    val pressedWith = PressedWith()
    val game = startWebGame(
        webBoard(),
        BrowserSaveRepository(LocalStorageStore()),
        BrowserPreferenceRepository(LocalStorageStore()),
        NoopAnalyticsService(),
        Mordie(WorkerMordieSearch.Factory(::DomSearchPort)),
        renderer,
        MainScope(),
        layoutOf(window.innerWidth, window.innerHeight),
        BuildInfo.VERSION,
        hostOf(window.location.hostname),
        touch = window.matchMedia("(pointer: coarse)").matches,
        pressedWith,
        DomPageAddress,
        DomClipboard,
        DomAppUpdates(window),
    )
    renderer.game = game
    listenForKeys(root)
    listenForEscape(root) { game }
    listenForPlayKeys(root, pressedWith) { game }
    // A share link pasted into this open tab.
    window.addEventListener("hashchange", { game.share.onLinkChanged() })
    // Coming back to this tab (or to a page restored from the back-forward cache) may find a new version.
    document.addEventListener("visibilitychange", { if (document.asDynamic().visibilityState == "visible") game.updates.check(CheckTrigger.TAB_RETURN) })
    window.addEventListener("pageshow", { event -> if (event.asDynamic().persisted == true) game.updates.check(CheckTrigger.TAB_RETURN) })
    // The browser's own "Leave site?" prompt, only while some progress isn't in a save.
    window.addEventListener("beforeunload", { event ->
        if (game.hasUnsavedProgress()) {
            event.preventDefault()
            // Older browsers show the prompt only when this is set, and not empty, as well.
            event.asDynamic().returnValue = true
        }
    })
}
