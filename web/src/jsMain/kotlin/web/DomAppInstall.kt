package web

import org.w3c.dom.Window

/** [AppInstall] over `window` (GH-360). Created first thing in `main`, so a `beforeinstallprompt` that comes before the game is up is kept: with
 *  [enabled] off (the kill switch) it listens to nothing, and never calls `preventDefault()`, so the browser's own offer (Chrome's mini-infobar
 *  on Android, the address bar's icon) is left alone. */
internal class DomAppInstall(private val window: Window, enabled: Boolean) : AppInstall {

    /** The `beforeinstallprompt` event, kept until its single use. */
    private var offer: dynamic = null
    private var installedEarly = false
    private var onPrompt: (() -> Unit)? = null
    private var onInstalled: (() -> Unit)? = null

    init {
        if (enabled) {
            window.addEventListener("beforeinstallprompt", { event ->
                // The page's own entry replaces the browser's offer: Chrome's mini-infobar on Android.
                event.preventDefault()
                offer = event
                onPrompt?.invoke()
            })
            window.addEventListener("appinstalled", {
                installedEarly = true
                onInstalled?.invoke()
            })
        }
    }

    override val signals = InstallSignals(
        userAgent = window.navigator.userAgent,
        appleTouch = isAppleTouch(window.navigator),
        promptCapable = js("'onbeforeinstallprompt' in window") == true,
    )

    override val standalone: Boolean
        get() = window.matchMedia("(display-mode: standalone)").matches || window.navigator.asDynamic().standalone == true

    override fun start(onPrompt: () -> Unit, onInstalled: () -> Unit) {
        this.onPrompt = onPrompt
        this.onInstalled = onInstalled
        if (offer != null) onPrompt()
        if (installedEarly) onInstalled()
    }

    override fun prompt(done: (Boolean?) -> Unit) {
        val event = offer
        // Single use: the browser makes another offer when it wants one.
        offer = null
        if (event == null) return done(null)
        try {
            val shown = event.prompt()
            // A rejected prompt() (no user activation left, a stale event) never settles userChoice: done(null) frees the game.
            if (shown != null && shown != undefined) shown.then({ _: dynamic -> }, { _: dynamic -> done(null) })
            event.userChoice.then({ choice: dynamic -> done(choice.outcome == "accepted") }, { _: dynamic -> done(null) })
        } catch (e: Throwable) {
            done(null)
        }
    }
}
