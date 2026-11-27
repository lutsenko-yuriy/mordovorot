package web.scenario

import web.AppInstall
import web.InstallSignals

/** A page whose browser can't install the game, as Firefox on a computer: the default, so a scenario that isn't about the Install entry (GH-360) sees the footer as it was. */
internal val FIREFOX_DESKTOP = InstallSignals("Mozilla/5.0 (X11; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0", appleTouch = false, promptCapable = false)

/** Chrome: it has `onbeforeinstallprompt`. */
internal val CHROME_DESKTOP = InstallSignals(
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
    appleTouch = false, promptCapable = true,
)

internal val SAFARI_IPHONE = InstallSignals(
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Mobile/15E148 Safari/604.1",
    appleTouch = true, promptCapable = false,
)

internal val SAFARI_MAC = InstallSignals(
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.1 Safari/605.1.15",
    appleTouch = false, promptCapable = false,
)

internal val CHROME_IPHONE = InstallSignals(
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/130.0.6723.90 Mobile/15E148 Safari/604.1",
    appleTouch = true, promptCapable = false,
)

internal val FIREFOX_IPHONE = InstallSignals(
    "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/131.0 Mobile/15E148 Safari/605.1.15",
    appleTouch = true, promptCapable = false,
)

/** The browser's install support, scripted: no [offered] prompt until [offer] (or from the start), and a prompt that stays open until [answer].
 *  [listening] says whether the page started to listen, and so took the browser's offer over: with the kill switch off, it doesn't. */
internal class FakeAppInstall(
    override val signals: InstallSignals = FIREFOX_DESKTOP,
    override var standalone: Boolean = false,
    offered: Boolean = false,
) : AppInstall {
    var offered = offered
        private set
    var listening = false
        private set
    var prompts = 0
        private set
    private var onPrompt: (() -> Unit)? = null
    private var onInstalled: (() -> Unit)? = null
    private var open: ((Boolean?) -> Unit)? = null

    /** The browser's prompt is open now. */
    val promptOpen get() = open != null

    override fun start(onPrompt: () -> Unit, onInstalled: () -> Unit) {
        listening = true
        this.onPrompt = onPrompt
        this.onInstalled = onInstalled
        if (offered) onPrompt()
    }

    override fun prompt(done: (Boolean?) -> Unit) {
        prompts++
        offered = false
        open = done
    }

    /** The browser fires `beforeinstallprompt`. */
    fun offer() {
        offered = true
        onPrompt?.invoke()
    }

    /** The player answers the open prompt: `true` accepts, `false` dismisses, `null` is a prompt that couldn't show. */
    fun answer(accepted: Boolean?) {
        val done = checkNotNull(open) { "no prompt is open" }
        open = null
        done(accepted)
    }

    /** The browser fires `appinstalled`: from the prompt, or from its menu. */
    fun installedByBrowser() {
        onInstalled?.invoke()
    }
}
