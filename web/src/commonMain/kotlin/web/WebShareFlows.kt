package web

import analytics.AnalyticsService
import board_model.BoardSize
import viewmodel.GameViewModel
import viewmodel.TileLabels
import web.qr.QrCode

/** Sharing a board (GH-243, ADR-0005): opening a share link, at startup and when the address changes, and the
 *  Share dialog with Copy link. [modal] shows the dialogs. */
internal class WebShareFlows(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val modal: WebModal,
    private val address: PageAddress,
    private val clipboard: Clipboard,
) {

    /** The latest link that arrived while a dialog or action was under way. */
    private var pending: String? = null

    /** The link in the address at startup; whether it started a board. */
    suspend fun openAtStartup(): Boolean = openLink(address.fragment())

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
            openLink(fragment)
        }
    }

    /** `[ Share ]`, or 7: the dialog with the link to the board Mordie started from, else to the board as it is. */
    fun open(opener: Focus = Focus.ShareButton) = game.runFlow(opener) {
        val start = viewModel.startingBoard()
        val side = viewModel.squareSide()
        val link = ShareLink.of(address.base(), side, start ?: viewModel.boardState())
        val props = mapOf("board_size" to side, "start_known" to (start != null))
        analytics.track("screen_share_dialog", props)
        // Too long for the largest code (a fork's long address): the link alone.
        val qr = try { QrCode.encode(link) } catch (e: IllegalArgumentException) { null }
        modal.ask<Unit>(WebDialog.Share(link, side, start != null, qr))
        game.cancelled("share")
    }

    /** Copy link, from the keyboard when [fromKeyboard] (a click with no pointer): the button reads
     *  Copied!; a refusal selects the link for the player to copy. */
    fun onCopyLink(fromKeyboard: Boolean) {
        val shown = modal.shown as? WebDialog.Share ?: return
        clipboard.copy(shown.link) { copied ->
            val open = modal.shown as? WebDialog.Share ?: return@copy
            if (copied) {
                modal.update(open.copy(copied = true))
                analytics.track(
                    "board_share_link_copied",
                    mapOf("board_size" to open.side, "start_known" to open.startKnown, "via" to if (fromKeyboard) "enter_key" else "button"),
                )
                game.showStatus("Link copied.")
            } else {
                game.showStatus("Couldn't copy the link - it's selected, so copy it yourself.")
                game.render(focus = Focus.ShareLinkField)
            }
        }
    }

    /** The Share dialog's Close. */
    fun onClosed() = modal.answer<WebDialog.Share, Unit>(Unit)

    /** The notice's OK. */
    fun onNoticeAnswered() = modal.answer<WebDialog.LinkRejected, Unit>(Unit)

    /** The confirmation's answer: Open, or Cancel. */
    fun onOpenAnswered(open: Boolean) = modal.answer<WebDialog.OpenShared, Boolean>(open)

    /** The labels prompt's answer: Yes, or No (GH-267). */
    fun onSharedLabelsAnswered(yes: Boolean) = modal.answer<WebDialog.SharedLabels, Boolean>(yes)

    private suspend fun openLink(fragment: String): Boolean {
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
                val carried = (link.labels as? LinkLabels.Scheme)?.labels
                // The labels prompt may follow, so the page doesn't blink between the two.
                if (viewModel.hasUnsavedProgress() && !modal.ask<Boolean>(WebDialog.OpenShared(side), leadsOn = { it && carried != null })) return false
                viewModel.newGameFrom(link.tiles)
                val own = viewModel.tileLabels()
                val labels = when {
                    link.labels is LinkLabels.None -> "none"
                    carried == null -> "unreadable"
                    carried == own -> "same"
                    else -> "differs"
                }
                analytics.track("shared_board_opened", mapOf("result" to "started", "board_size" to side, "labels" to labels))
                game.showStatus("Started a shared ${side}x$side board.")
                if (labels == "differs") askLabels(carried!!, own, side)
                return true
            }
        }
    }

    /** The sender's [sender] labels, previewed behind the prompt: Yes keeps them for this game, No drops them (GH-267). */
    private suspend fun askLabels(sender: TileLabels, own: TileLabels, side: Int) {
        analytics.track(
            "screen_shared_labels_dialog",
            mapOf("board_size" to side, "sender_labels" to sender.wireName, "receiver_labels" to own.wireName),
        )
        if (modal.ask<Boolean>(WebDialog.SharedLabels(sender), leadsOn = { it })) {
            viewModel.useSharedLabels(sender)
            analytics.track(
                "shared_labels_accepted",
                mapOf("sender_labels" to sender.wireName, "receiver_labels" to own.wireName, "board_size" to side),
            )
            game.render()
        } else {
            game.cancelled("shared_labels")
        }
    }

    private fun message(link: LinkBoard.Rejected) = when (link.reason) {
        "unsupported_size" ->
            "This link's board is ${link.side}x${link.side}; this site plays ${BoardSize.MIN}x${BoardSize.MIN} to ${BoardSize.VERY_LARGE.last}x${BoardSize.VERY_LARGE.last}."
        "unsolvable" -> "No sequence of shifts can solve this link's board."
        else -> "This link's board isn't a valid board."
    }
}
