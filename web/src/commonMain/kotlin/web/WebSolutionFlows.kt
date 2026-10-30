package web

import analytics.AnalyticsService
import viewmodel.GameViewModel
import viewmodel.GiveUpButton
import viewmodel.SolutionStep

/** [WebGame]'s Give Up and Mordie's solution screen (GH-126): the confirmation and the replay. */
internal class WebSolutionFlows(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val modal: WebModal,
) {

    /** `[ Give Up ]`: the confirmation, then Mordie's solution screen on Yes. */
    fun onGiveUp() {
        if (viewModel.giveUpButton() != GiveUpButton.GIVE_UP) return
        game.runFlow(Focus.GiveUpRow) {
            analytics.track("screen_give_up_dialog", mapOf("board_size" to viewModel.squareSide(), "move_count" to viewModel.moveCount()))
            if (modal.ask(WebDialog.GiveUp)) {
                viewModel.giveUp()
                game.saying = solutionScreen()
                // The solution screen has no Give Up.
                game.closing = Focus.ReplayNext
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
        game.render()
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
        if (!game.idle || viewModel.solution() == null) return
        game.status = null
        val shifted = action()
        if (shifted != null || restart) game.saying = viewModel.solution()?.let { Announcements.step(it.moves, it.current) }
        game.render(shifted?.let(Slides::ofStep))
    }

    private fun solutionScreen() = viewModel.solution()?.let { Announcements.solution(it.moves.size) }
}
