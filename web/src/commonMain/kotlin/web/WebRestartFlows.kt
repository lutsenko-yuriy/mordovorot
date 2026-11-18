package web

import analytics.AnalyticsService
import viewmodel.GameViewModel
import viewmodel.RestartButton

/** [WebGame]'s Restart (GH-349): `[ Restart ]` or R puts the game back to its start. A won board restarts at once; with moves made the
 *  save question comes first, and on Save the restart goes on only once the game is saved. A Cancel, Escape or failed save leaves the
 *  game as it was, focus back on the opener. */
internal class WebRestartFlows(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val modal: WebModal,
) {

    /** [openedFrom] is `button` or `shortcut`; [opener] gets focus back when nothing restarts. Does nothing unless the button is live. */
    fun onRestart(openedFrom: String, opener: Focus = Focus.RestartButton) {
        if (!game.idle || game.tutorial.running || viewModel.restartButton() != RestartButton.LIVE) return
        game.runFlow(opener) {
            val choice = if (viewModel.isSolved()) NOT_ASKED else ask(openedFrom) ?: return@runFlow
            viewModel.restartGame(openedFrom, choice)
            // The keys work on the board; on touch there is none, and Restart is dimmed now.
            game.closing = game.onBoard ?: Focus.NewButton
        }
    }

    /** The save question; the `save_choice` it ends in, or `null` when nothing restarts. */
    private suspend fun ask(openedFrom: String): String? {
        analytics.track("screen_restart_save_dialog", mapOf("opened_from" to openedFrom, "move_count" to viewModel.restartMoves()))
        return when (modal.ask<ReplaySaveChoice>(WebDialog.RestartSave) { it == ReplaySaveChoice.SAVE }) {
            ReplaySaveChoice.CANCEL -> {
                game.cancelled("restart_save")
                null
            }
            ReplaySaveChoice.SAVE -> if (game.saveFlow("restart")) SAVED else null
            ReplaySaveChoice.DONT_SAVE -> DONT_SAVE
        }
    }

    /** The save question's answer. */
    fun onAnswered(choice: ReplaySaveChoice) = modal.answer<WebDialog.RestartSave, ReplaySaveChoice>(choice)

    private companion object {
        const val NOT_ASKED = "not_asked"
        const val DONT_SAVE = "dont_save"
        const val SAVED = "saved"
    }
}
