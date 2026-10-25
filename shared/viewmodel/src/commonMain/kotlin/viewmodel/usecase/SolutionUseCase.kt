package viewmodel.usecase

import analytics.AnalyticsService
import board_model.BoardModel
import viewmodel.GiveUpButton
import viewmodel.MordieHooks
import viewmodel.MordieText
import viewmodel.SolutionReplay
import viewmodel.SolutionScreen
import viewmodel.SolutionState
import viewmodel.SolutionStep
import viewmodel.SolutionSteps
import viewmodel.UnsavedProgress

/** Give Up and the solution screen (GH-126): when the button shows, entering the screen on the
 *  original arrangement, and the replay with its analytics. */
internal class SolutionUseCase(
    private val board: BoardModel,
    private val mordieHooks: MordieHooks,
    private val analytics: AnalyticsService,
    private val solutionScreen: SolutionScreen,
    private val unsavedProgress: UnsavedProgress,
) {

    val onScreen: Boolean get() = solutionScreen.replay != null

    fun button(): GiveUpButton {
        val hasPath = mordieHooks.solution() != null
        return when {
            onScreen || mordieHooks.sitsOut() -> GiveUpButton.HIDDEN
            board.isCorrect() ->
                if (hasPath && mordieHooks.foundBeforeWin()) GiveUpButton.MORDIES_SOLUTION else GiveUpButton.HIDDEN
            hasPath && mordieHooks.revealed() -> GiveUpButton.GIVE_UP
            else -> GiveUpButton.DIMMED
        }
    }

    fun giveUp() {
        if (button() != GiveUpButton.GIVE_UP) return
        analytics.track(
            "game_given_up",
            mapOf("board_size" to board.squareSide, "move_count" to board.moveCount, "solution_length" to mordieHooks.solution()!!.size) +
                mordieHooks.sinceRevealProps(),
        )
        enter("give_up")
    }

    fun showAfterWin() {
        if (button() == GiveUpButton.MORDIES_SOLUTION) enter("after_win")
    }

    /** After a load of a solution-screen save, [trigger] being the load's. */
    fun enterLoaded(trigger: String) = enter(if (trigger == "startup_prompt") "startup_prompt" else "load")

    fun leave() {
        solutionScreen.replay = null
    }

    /** Puts the original arrangement back on the board, which discards the player's moves. */
    private fun enter(openedFrom: String) {
        val original = mordieHooks.original() ?: return
        val moves = mordieHooks.solution() ?: return
        board.restoreState(original, 0)
        val replay = SolutionReplay(original, board.squareSide, SolutionSteps.of(moves, board.squareSide))
        solutionScreen.replay = replay
        unsavedProgress.unsaved = false
        analytics.track("screen_solution", mapOf("opened_from" to openedFrom) + replayProps(replay))
    }

    fun state(): SolutionState? = solutionScreen.replay?.let {
        SolutionState(SolutionSteps.format(it.steps), it.step, it.atStart, it.atEnd)
    }

    /** Mordie's line on the screen; `null` off it. */
    fun line(): String? = solutionScreen.replay?.let { MordieText.solution(it.steps.size) }

    /** The replay's tiles; `null` off the screen. */
    fun tiles(): IntArray? = solutionScreen.replay?.tiles

    /** Tracks `solution_replay_started` and `solution_replay_finished` as the replay's runs
     *  begin and end. */
    fun next(): SolutionStep? {
        val replay = solutionScreen.replay ?: return null
        val shift = replay.next() ?: return null
        if (replay.startedRun) analytics.track("solution_replay_started", mapOf("restart" to (replay.runs > 1)) + replayProps(replay))
        if (replay.finishedRun) analytics.track("solution_replay_finished", replayProps(replay) + ("stepped_back" to replay.steppedBack))
        return shift
    }

    fun previous(): SolutionStep? = solutionScreen.replay?.previous()

    fun restart() {
        solutionScreen.replay?.restart()
    }

    private fun replayProps(replay: SolutionReplay) =
        mapOf("board_size" to board.squareSide, "solution_length" to replay.steps.size)
}
