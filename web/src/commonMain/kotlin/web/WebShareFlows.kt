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
    private val shareLabels: ShareTogglePreference,
    private val shareProgress: ShareTogglePreference,
) {

    /** Whether the Share dialog's toggle is on: read once, kept for the session and written on each flip (GH-267). */
    private var includeLabels = shareLabels.load()

    /** The same for `Include my progress` (GH-272). It holds while there is no progress to send; the dialog shows it off then. */
    private var includeProgress = shareProgress.load()

    /** The latest link that arrived while a dialog or action was under way. */
    private var pending: String? = null

    /** The link in the address at startup; whether it started a board. */
    suspend fun openAtStartup(): Boolean {
        // The address holds the latest link, so one that waited as pending is the same (the tutorial's Skip, GH-288).
        pending = null
        return openLink(address.fragment())
    }

    /** Whether the address holds a share link, read or not. */
    fun hasStartupLink(): Boolean = ShareLink.read(address.fragment()) != null

    /** Whether the address holds a link that starts a board, not a rejected one. */
    fun hasPlayableStartupLink(): Boolean = ShareLink.read(address.fragment()) is LinkBoard.Playable

    /** The link left in the address while the tutorial ran now waits as any other. */
    fun holdStartupLink() {
        if (hasStartupLink()) pending = address.fragment()
    }

    /** The address changed (`hashchange`): opens a share link now, or once the dialog or action ends. */
    fun onLinkChanged() {
        val fragment = address.fragment()
        if (ShareLink.read(fragment) == null) return
        pending = fragment
        openWaiting()
    }

    /** Opens the link that waited, if nothing is under way now (a drag ended). */
    fun openWaiting() {
        if (pending != null && game.idle && !game.tutorial.running) game.runFlow(game.onBoard ?: Focus.PageStart) { openPending() }
    }

    /** Opens the links that waited, if any: one that arrives while another is asked about waits for that answer. */
    suspend fun openPending() {
        while (!game.tutorial.running) {
            val fragment = pending ?: return
            pending = null
            openLink(fragment)
        }
    }

    /** `[ Share ]`, or 7: the dialog with the link to the board Mordie started from, else to the board as it is. */
    fun open(opener: Focus = Focus.ShareButton) = game.runFlow(opener) {
        val side = viewModel.squareSide()
        val dialog = shareDialog(copied = false)
        analytics.track("screen_share_dialog", mapOf("board_size" to side, "start_known" to dialog.startKnown, "include_labels" to dialog.includeLabels, "include_progress" to dialog.includeProgress))
        modal.ask<Unit>(dialog)
        game.cancelled("share")
    }

    /** The dialog for the board as it is: the labels shown go in the link when the toggle is on (GH-267), so a
     *  6x6 board with a Letters preference carries Numbers. */
    private fun shareDialog(copied: Boolean): WebDialog.Share {
        val start = viewModel.startingBoard()
        val side = viewModel.squareSide()
        val labels = viewModel.tileLabels()
        val progress = viewModel.sharedProgress()?.takeIf(ShareLink::carries)
        val sent = progress.takeIf { includeProgress }
        val link = ShareLink.of(address.base(), side, start ?: viewModel.boardState(), labels.takeIf { includeLabels }, sent)
        // Too long for the largest code (a fork's long address): the link alone.
        val qr = try { QrCode.encode(link) } catch (e: IllegalArgumentException) { null }
        return WebDialog.Share(link, side, start != null, qr, copied, labels, includeLabels, progress, sent != null)
    }

    /** The toggle, flipped: the link and its code are rebuilt, and Copied! no longer holds, as the link copied is the
     *  other one. Focus stays on the toggle. */
    fun onLabelsToggled() {
        val open = modal.shown as? WebDialog.Share ?: return
        includeLabels = !includeLabels
        shareLabels.save(includeLabels)
        modal.update(shareDialog(copied = false))
        analytics.track(
            "share_labels_toggled",
            mapOf("enabled" to includeLabels, "labels" to open.labels.wireName, "board_size" to open.side),
        )
        game.render(focus = Focus.ShareLabels)
    }

    /** The progress toggle, flipped, as [onLabelsToggled]; nothing happens at 0 moves, when it is dimmed (GH-272). */
    fun onProgressToggled() {
        val open = modal.shown as? WebDialog.Share ?: return
        val progress = open.progress ?: return
        includeProgress = !includeProgress
        shareProgress.save(includeProgress)
        modal.update(shareDialog(copied = false))
        analytics.track(
            "share_progress_toggled",
            mapOf("enabled" to includeProgress, "progress" to progress.analyticsName, "board_size" to open.side),
        )
        game.render(focus = Focus.ShareProgress)
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
                    mapOf(
                        "board_size" to open.side, "start_known" to open.startKnown, "includes_labels" to open.includeLabels,
                        "includes_progress" to open.includeProgress,
                        "via" to if (fromKeyboard) "enter_key" else "button",
                    ),
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
                val sent = (link.progress as? LinkProgress.Sent)?.progress
                viewModel.newGameFrom(link.tiles, sent)
                val own = viewModel.tileLabels()
                val labels = when {
                    link.labels is LinkLabels.None -> "none"
                    carried == null -> "unreadable"
                    carried == own -> "same"
                    else -> "differs"
                }
                val progress = when (link.progress) {
                    LinkProgress.None -> "none"
                    LinkProgress.Unreadable -> "unreadable"
                    is LinkProgress.Sent -> link.progress.progress.analyticsName
                }
                analytics.track("shared_board_opened", mapOf("result" to "started", "board_size" to side, "labels" to labels, "progress" to progress))
                val started = "Started a shared ${side}x$side board."
                if (labels == "differs") {
                    // Not drawn on its own: the prompt's frame shows it, so a prompt that follows another doesn't blink.
                    game.noteStatus(started)
                    askLabels(carried!!, own, side)
                } else {
                    game.showStatus(started)
                }
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
