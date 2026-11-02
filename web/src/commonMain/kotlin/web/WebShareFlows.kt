package web

import analytics.AnalyticsService
import board_model.BoardSize
import viewmodel.GameViewModel

/** Opening a share link (GH-243, ADR-0005), at startup and when the address changes. [modal] shows
 *  the notice and the confirmation. */
internal class WebShareFlows(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val modal: WebModal,
    private val address: PageAddress,
) {

    /** The latest link that arrived while a dialog or action was under way. */
    private var pending: String? = null

    /** The link in the address at startup; whether it started a board. */
    suspend fun openAtStartup(): Boolean = open(address.fragment())

    /** The address changed (`hashchange`): opens a share link now, or once the dialog or action ends. */
    fun onLinkChanged() {
        val fragment = address.fragment()
        if (ShareLink.read(fragment) == null) return
        pending = fragment
        openWaiting()
    }

    /** Opens the link that waited, if nothing is under way now (a drag ended). */
    fun openWaiting() {
        if (pending != null && game.idle) game.runFlow(if (game.controls.mode == PlayMode.KEYBOARD) Focus.Board else Focus.PageStart) { openPending() }
    }

    /** Opens the links that waited, if any: one that arrives while another is asked about waits for that answer. */
    suspend fun openPending() {
        while (true) {
            val fragment = pending ?: return
            pending = null
            open(fragment)
        }
    }

    /** The notice's OK. */
    fun onNoticeAnswered() = modal.answer<WebDialog.LinkRejected, Unit>(Unit)

    /** The confirmation's answer: Open, or Cancel. */
    fun onOpenAnswered(open: Boolean) = modal.answer<WebDialog.OpenShared, Boolean>(open)

    private suspend fun open(fragment: String): Boolean {
        val link = ShareLink.read(fragment) ?: return false
        // The link is spent whatever happens next, so a reload doesn't meet it again.
        address.clearFragment()
        when (link) {
            is LinkBoard.Rejected -> {
                analytics.track("shared_board_opened", mapOf("result" to "rejected", "reason" to link.reason))
                modal.ask<Unit>(WebDialog.LinkRejected(message(link)))
                return false
            }
            is LinkBoard.Playable -> {
                val side = link.side
                if (viewModel.hasUnsavedProgress() && !modal.ask<Boolean>(WebDialog.OpenShared(side))) return false
                viewModel.newGameFrom(link.tiles)
                analytics.track("shared_board_opened", mapOf("result" to "started", "board_size" to side))
                game.showStatus("Started a shared ${side}x$side board.")
                return true
            }
        }
    }

    private fun message(link: LinkBoard.Rejected) = when (link.reason) {
        "unsupported_size" ->
            "This link's board is ${link.side}x${link.side}; this site plays ${BoardSize.MIN}x${BoardSize.MIN} to ${BoardSize.VERY_LARGE.last}x${BoardSize.VERY_LARGE.last}."
        "unsolvable" -> "No sequence of shifts can solve this link's board."
        else -> "This link's board isn't a valid board."
    }
}
