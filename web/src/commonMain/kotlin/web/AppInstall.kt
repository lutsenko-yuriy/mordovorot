package web

/** How the install entry installs the game (GH-360); [wire] is `method` in the install events. */
enum class InstallMethod(val wire: String) {
    /** Chrome, Edge and Android: a click opens the browser's own install prompt. */
    BROWSER_PROMPT("browser_prompt"),

    /** Safari: no install API, so a click opens the dialog with the steps. */
    SAFARI_STEPS("safari_steps"),
}

/** Which of Safari's two ways to install the steps dialog words; [wire] is `screen_install_dialog.device`. */
enum class InstallDevice(val wire: String) {
    /** Add to Home Screen, in the Share sheet or the ⋯ menu. */
    PHONE_OR_TABLET("phone_or_tablet"),

    /** Add to Dock, in the File menu. */
    MAC("mac"),
}

/** What the page knows about its browser, collected in the DOM layer so [safariDevice] stays a pure rule (GH-360). */
data class InstallSignals(
    val userAgent: String,
    /** An iPhone or iPad's WebKit: touch points and `-webkit-touch-callout`, which an iPad in desktop mode shows as well. */
    val appleTouch: Boolean,
    /** The browser has `onbeforeinstallprompt`: it is Chromium, and the prompt path serves it. */
    val promptCapable: Boolean,
)

/** The page's seam to the browser's install support (GH-360), like [AppUpdates] (GH-262). */
interface AppInstall {
    /** What the browser is. */
    val signals: InstallSignals

    /** The page runs in the installed app's own window: `display-mode: standalone`, or Safari's `navigator.standalone`. */
    val standalone: Boolean

    /** Starts listening. [onPrompt] is called each time the browser offers its install prompt, at once for one it offered before the page was ready,
     *  [onInstalled] on an `appinstalled` event, which also comes from an install made in the browser's menu. */
    fun start(onPrompt: () -> Unit, onInstalled: () -> Unit)

    /** Opens the browser's prompt, spending the offer. [done] is `true` when accepted, `false` when dismissed, `null` when the prompt couldn't show. */
    fun prompt(done: (Boolean?) -> Unit)
}

/** Browsers that wrap WebKit on iOS or look like Safari, and have no Add to Home Screen of their own the page could name: Chrome, Firefox, Edge, Opera,
 *  Google, Yandex and DuckDuckGo's. A Mac Safari user agent never carries them. */
private val OTHER_BROWSERS = listOf("CriOS", "FxiOS", "EdgiOS", "OPiOS", "OPT/", "GSA/", "YaBrowser", "DuckDuckGo", "Ddg/", "Chrome", "Chromium", "Edg/", "Firefox", "OPR/")

/** The Safari the steps dialog is for, `null` for any other browser (GH-360, decision 1).
 *  A browser with `onbeforeinstallprompt` is Chromium and gets the prompt instead. The platform comes from [InstallSignals.appleTouch]
 *  (an iPad in desktop mode says `Macintosh` in its user agent too); the user agent only excludes what features can't: other browsers
 *  on iOS, in-app views, which have no `Safari/` token, and on a Mac any Safari before 17, which has no Add to Dock. */
internal fun safariDevice(signals: InstallSignals): InstallDevice? {
    val agent = signals.userAgent
    if (signals.promptCapable || "Safari/" !in agent) return null
    if (OTHER_BROWSERS.any { it in agent }) return null
    return when {
        signals.appleTouch -> InstallDevice.PHONE_OR_TABLET
        "Macintosh" in agent && safariMajor(agent) >= MAC_ADD_TO_DOCK -> InstallDevice.MAC
        else -> null
    }
}

private const val MAC_ADD_TO_DOCK = 17

private fun safariMajor(agent: String): Int =
    Regex("Version/(\\d+)").find(agent)?.groupValues?.get(1)?.toIntOrNull() ?: 0

/** The entry the footer shows, `null` for none (GH-360): the browser's prompt is waiting, or this is a Safari that can add the game, and nothing
 *  rules it out: it is [enabled] (the kill switch), the page isn't the installed app's own window ([standalone]), the game isn't [installed]
 *  already, the tutorial is behind the player ([tutorialDone]) and none runs ([tutorialRunning], a replay too), and no new version is
 *  waiting ([updateReady]: its notice takes the entry's place). */
internal fun installEntry(
    promptWaiting: Boolean,
    safari: InstallDevice?,
    standalone: Boolean,
    installed: Boolean,
    tutorialDone: Boolean,
    tutorialRunning: Boolean,
    updateReady: Boolean,
    enabled: Boolean,
): InstallMethod? = when {
    !enabled || standalone || installed || !tutorialDone || tutorialRunning || updateReady -> null
    promptWaiting -> InstallMethod.BROWSER_PROMPT
    safari != null -> InstallMethod.SAFARI_STEPS
    else -> null
}
