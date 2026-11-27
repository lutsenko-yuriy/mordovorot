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
    // Before anything else: the browser may offer the install prompt at any moment, and this keeps it (GH-360).
    val installOn = true
    val install = DomAppInstall(window, installOn)
    // First, while the root's font size is still the browser's default.
    applySystemTextSize(document, window)
    val root = document.getElementById("app") as? HTMLElement ?: return
    stopPinchZoom(document, window.navigator)
    // Outside #app, which DomRenderer rebuilds: it fills this footer in, and redraws it for the New version available notice.
    // The language picker (GH-275) stands before it, so Tab reaches it after the game's controls and before the credits.
    val picker = DomLanguagePicker.create()
    document.body?.append(picker, element("footer", "footer"))
    val renderer = DomRenderer(root, document.getElementById("announcer")?.let(::DomAnnouncer), picker)
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
        install,
        installOn,
        tutorial = true,
        browserLanguages = browserLanguages(),
        languages = true,
    )
    renderer.game = game
    listenForKeys(root)
    listenForEscape(root) { game }
    listenForPlayKeys(root, pressedWith) { game }
    // A share link pasted into this open tab.
    window.addEventListener("hashchange", { game.share.onLinkChanged() })
    // Coming back to this tab (or to a page restored from the back-forward cache) may find a new version.
    // Hiding it autosaves: a phone may never fire the events that follow.
    document.addEventListener("visibilitychange", {
        if (document.asDynamic().visibilityState == "visible") game.updates.check(CheckTrigger.TAB_RETURN) else game.onPageLeft()
    })
    window.addEventListener("pageshow", { event -> if (event.asDynamic().persisted == true) game.updates.check(CheckTrigger.TAB_RETURN) })
    // Leaving autosaves (GH-338), in beforeunload too: the browser decides on its own "Leave site?" prompt before pagehide fires.
    // The prompt shows only when the autosave failed, and a success makes the later events no-ops.
    window.addEventListener("beforeunload", { event ->
        if (game.onPageLeft()) {
            event.preventDefault()
            // Older browsers show the prompt only when this is set, and not empty, as well.
            event.asDynamic().returnValue = true
        }
    })
    window.addEventListener("pagehide", { game.onPageLeft() })
}

/** The browser's languages, the most wanted first; just its own language where the list is empty, and none where the browser has neither. */
private fun browserLanguages(): List<String> {
    val navigator = window.navigator.asDynamic()
    val list = (navigator.languages as? Array<*>)?.mapNotNull { it as? String }.orEmpty()
    return list.ifEmpty { listOfNotNull((navigator.language as? String)?.takeIf { it.isNotEmpty() }) }
}
