package web

import analytics.AnalyticsService
import viewmodel.GameViewModel

/** The New version available notice and Update (GH-262): version checks, the footer they change, and the
 *  confirmation before an update would lose a game. [modal] shows the dialog. */
internal class WebUpdateFlows(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val modal: WebModal,
    private val updates: AppUpdates,
    private val appVersion: String,
) {

    /** A new version is cached and waiting. */
    var ready = false
        private set

    /** The page is about to reload into the new version: its leave-site prompt has been answered. */
    var applying = false
        private set

    /** What made the latest check. */
    private var trigger = CheckTrigger.LOAD

    /** What made the check that found the version now downloading, which `update_available` reports once it is
     *  ready: later checks, a tab switch during the download, say, don't relabel it. */
    private var foundBy = CheckTrigger.LOAD

    val footer: List<FooterPart> get() = footer(appVersion, ready)

    /** Registers the worker and makes the first check. */
    fun start() {
        updates.start({ foundBy = trigger }, ::onReady)
        check(CheckTrigger.LOAD)
    }

    fun check(because: CheckTrigger) {
        trigger = because
        updates.check()
    }

    /** `Update` in the footer: reloads at once, or after a confirmation when the game has unsaved progress. */
    fun onUpdate() = game.runFlow(Focus.UpdateLink) {
        if (!ready) return@runFlow
        if (!viewModel.hasUnsavedProgress()) return@runFlow apply(unsaved = false, confirmed = false)
        analytics.track("screen_update_dialog", mapOf("board_size" to viewModel.squareSide(), "move_count" to viewModel.moveCount()))
        if (modal.ask<Boolean>(WebDialog.Update)) apply(unsaved = true, confirmed = true) else game.cancelled("update")
    }

    /** The confirmation's answer: `true` is Update. */
    fun onUpdateAnswered(update: Boolean) = modal.answer<WebDialog.Update, Boolean>(update)

    private fun onReady() {
        if (ready) return
        ready = true
        analytics.track("update_available", mapOf("check_trigger" to foundBy.wire, "current_version" to appVersion))
        game.render()
    }

    private fun apply(unsaved: Boolean, confirmed: Boolean) {
        analytics.track("update_applied", mapOf("had_unsaved_progress" to unsaved, "confirmed" to confirmed))
        applying = true
        updates.apply()
    }
}
