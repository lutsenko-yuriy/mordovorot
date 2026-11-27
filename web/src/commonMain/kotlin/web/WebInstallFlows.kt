package web

import analytics.AnalyticsService
import kotlinx.coroutines.CompletableDeferred

/** The Install entry in the footer (GH-360): when it shows, its click, the browser's install prompt, the Safari steps dialog and the install
 *  analytics. [tutorialDone], [tutorialRunning] and [updateReady] are the game's other flows, read when a frame is built. With [enabled] off
 *  (the kill switch) nothing listens to the browser and the entry never shows. */
internal class WebInstallFlows(
    private val game: WebGame,
    private val analytics: AnalyticsService,
    private val modal: WebModal,
    private val install: AppInstall,
    private val appVersion: String,
    private val enabled: Boolean,
    private val tutorialDone: () -> Boolean,
    private val tutorialRunning: () -> Boolean,
    private val updateReady: () -> Boolean,
) {

    private val safari = safariDevice(install.signals)

    /** The browser has offered its install prompt, and it isn't spent. */
    private var promptWaiting = false

    /** The game was installed in this page load, from the entry or the browser's menu: no entry for the rest of it. */
    private var installed = false

    /** The browser's prompt is open now. */
    private var prompting = false

    /** The entry's prompt was accepted in this page load. */
    private var accepted = false

    /** `install_entry_shown` was sent: at most once per page load. */
    private var shownSent = false

    /** The entry this frame shows, if any. */
    private val entry: InstallMethod?
        get() = installEntry(promptWaiting, safari, install.standalone, installed, tutorialDone(), tutorialRunning(), updateReady(), enabled)

    /** The footer line: the New version available notice, else the credit with the entry. */
    val footer: List<FooterPart> get() = footer(appVersion, updateReady(), entry)

    fun start() {
        if (enabled) install.start(::onPrompt, ::onInstalled)
    }

    /** The frame just drawn shows the entry: sent once per page load, the notice taking its place and the entry coming back not counting again. */
    fun noteShown() {
        val method = entry ?: return
        if (shownSent) return
        shownSent = true
        analytics.track("install_entry_shown", mapOf("method" to method.wire))
    }

    /** `Install the game` in the footer, by a click if [byPointer]. After it, focus returns to the entry; a mouse click on a computer puts it on the board
     *  instead, where the keys work (as the language picker does, GH-275), unless Escape closed the steps. */
    fun onInstall(byPointer: Boolean) {
        val method = entry ?: return
        game.runFlow(Focus.InstallLink) {
            analytics.track("install_entry_clicked", mapOf("method" to method.wire))
            val escaped = when (method) {
                InstallMethod.BROWSER_PROMPT -> prompt().let { false }
                InstallMethod.SAFARI_STEPS -> steps(checkNotNull(safari)).let { modal.closedVia == CancelVia.ESCAPE }
            }
            if (byPointer && !escaped) game.closing = game.onBoard ?: game.closing
        }
    }

    /** The Install dialog's Close. */
    fun onClosed() = modal.answer<WebDialog.Install, Unit>(Unit)

    private fun onPrompt() {
        promptWaiting = true
        game.render()
    }

    private fun onInstalled() {
        if (installed) return
        installed = true
        // `appinstalled` can come before the prompt's own answer does.
        analytics.track("app_installed", mapOf("via_entry" to (accepted || prompting)))
        game.render()
    }

    /** The browser's prompt: a spent offer, so the entry stays away until the browser makes another. A prompt that couldn't show reports nothing. */
    private suspend fun prompt() {
        val answer = CompletableDeferred<Boolean?>()
        prompting = true
        promptWaiting = false
        install.prompt { answer.complete(it) }
        val accepted = answer.await()
        prompting = false
        if (accepted == null) return
        this.accepted = accepted
        analytics.track("install_prompt_result", mapOf("outcome" to if (accepted) "accepted" else "dismissed"))
    }

    private suspend fun steps(device: InstallDevice) {
        analytics.track("screen_install_dialog", mapOf("device" to device.wire))
        modal.ask<Unit>(WebDialog.Install(device))
    }
}
