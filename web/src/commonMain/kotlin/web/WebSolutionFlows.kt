package web

import analytics.AnalyticsService
import viewmodel.GameViewModel
import viewmodel.GiveUpButton
import viewmodel.SolutionStep
import web.a11y.Announcements

/** [WebGame]'s Give Up and Mordie's solution screen (GH-126): the confirmation and the replay. */
internal class WebSolutionFlows(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val modal: WebModal,
) {

    /** `[ Give Up ]`: the confirmation, then Mordie's solution screen on Yes. */
    fun onGiveUp(opener: Focus = Focus.GiveUpRow) {
        if (viewModel.giveUpButton() != GiveUpButton.GIVE_UP) return
        game.runFlow(opener) {
            analytics.track("screen_give_up_dialog", mapOf("board_size" to viewModel.squareSide(), "move_count" to viewModel.moveCount()))
            if (modal.ask(WebDialog.GiveUp)) {
                viewModel.giveUp()
                game.updates.check(CheckTrigger.GAME_END)
                game.saying = solutionScreen()
                // The solution screen has no Give Up.
                game.closing = game.onBoard ?: Focus.ReplayNext
            } else game.cancelled("give_up")
        }
    }

    /** The give-up confirmation's answer: Yes or No. */
    fun onGiveUpAnswered(yes: Boolean) = modal.answer<WebDialog.GiveUp, Boolean>(yes)

    /** `[ Mordie's solution ]` after a win: the screen, with no confirmation. */
    fun onMordiesSolution() {
        if (!game.idle) return
        game.status = null
        viewModel.showSolution()
        game.saying = solutionScreen()
        game.render(focus = game.onBoard)
    }

    fun onReplayNext() = replay { viewModel.replayNext() }

    fun onReplayPrevious() = replay { viewModel.replayPrevious() }

    fun onReplayRestart() = replay(restart = true) {
        viewModel.replayRestart()
        null
    }

    /** Steps the replay and renders, sliding the shift [action] made and saying the step it's on,
     *  as a [restart] always does. */
    private fun replay(restart: Boolean = false, action: () -> SolutionStep?) {
        if (!replayable()) return
        // A step waits for a running label wave (GH-343).
        game.moves.run(::replayable) { step(restart, action) }
    }

    private fun replayable() = game.idle && viewModel.solution() != null

    private fun step(restart: Boolean, action: () -> SolutionStep?) {
        game.status = null
        val shifted = action()
        if (shifted != null || restart) game.saying = viewModel.solution()?.let { Announcements.step(it.moves, it.current) }
        game.render(shifted?.let(Slides::ofStep))
    }

    private fun solutionScreen() = viewModel.solution()?.let { Announcements.solution(it.moves.size) }
}
