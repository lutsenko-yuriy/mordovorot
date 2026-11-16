package web

import analytics.AnalyticsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import viewmodel.GameViewModel
import web.drag.Axis
import web.tutorial.TutorialProgress
import web.tutorial.TutorialScript
import web.tutorial.TutorialText
import web.tutorial.Verdict

/** What the save question was answered with (GH-289). */
internal enum class ReplaySaveChoice { SAVE, DONT_SAVE, CANCEL }

/** The tutorial's flow (GH-288): starts on the fixed board when it is [enabled], not seen and there are no saves,
 *  checks every shift against the step's asked one, and ends at step 6 or on Skip. While [running] the leave-site prompt,
 *  the share link and most digits wait. It can be replayed from the Give Up row, after a question about saving (GH-289). */
internal class WebTutorial(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val modal: WebModal,
    private val preference: TutorialPreference,
    private val scope: CoroutineScope,
    private val computer: Boolean,
    private val enabled: Boolean,
) {

    private var progress: TutorialProgress? = null

    /** The axis of the line a blocked shift said to shift, until the next shift that counts or is blocked. */
    private var blockedAxis: Axis? = null

    /** What [take] said about the shift now being made. */
    private var verdict = Verdict.IGNORED
    private var pause: Job? = null

    /** `first_launch` or `replay`: what started the tutorial now running, as `tutorial_started.source`. */
    private var source = FIRST_LAUNCH

    val running get() = progress != null

    /** Step 5: the board is solved and Mordie's ending waits for the pause to end. */
    val holdsEnding get() = progress?.step == TutorialScript.SOLVED_STEP

    fun shouldStart() = enabled && !preference.seen() && viewModel.listSaves().isEmpty()

    /** `[ Tutorial ]` shows in the Give Up row, and 8 works: not while it runs, and not on Mordie's solution screen (GH-289). */
    val offered get() = enabled && !running && viewModel.solution() == null

    fun begin(source: String = FIRST_LAUNCH) {
        this.source = source
        pause?.cancel()
        viewModel.newGameFrom(TutorialScript.board, tutorial = true)
        progress = TutorialProgress()
        blockedAxis = null
        analytics.track("tutorial_started", mapOf("source" to source))
    }

    /** `[ Tutorial ]` or 8, [openedFrom] `button` or `shortcut` (GH-289): with unsaved progress the replay asks first, and on Save goes on
     *  only once the game is saved. A Cancel or a failed save leaves the game as it was, focus back on [opener]. */
    fun onReplay(openedFrom: String, opener: Focus = Focus.TutorialReplay) = game.runFlow(opener) {
        if (game.hasUnsavedProgress()) {
            analytics.track("screen_replay_save_dialog", mapOf("opened_from" to openedFrom, "move_count" to viewModel.moveCount()))
            when (modal.ask<ReplaySaveChoice>(WebDialog.ReplaySave) { it == ReplaySaveChoice.SAVE }) {
                ReplaySaveChoice.CANCEL -> {
                    game.cancelled("replay_save")
                    return@runFlow
                }
                ReplaySaveChoice.SAVE -> {
                    analytics.track("tutorial_replay_decision", mapOf("decision" to "save"))
                    if (!game.saveFlow("tutorial_replay")) return@runFlow
                }
                ReplaySaveChoice.DONT_SAVE -> analytics.track("tutorial_replay_decision", mapOf("decision" to "dont_save"))
            }
        }
        begin(REPLAY)
        game.closing = game.onBoard ?: Focus.PageStart
    }

    /** The save question's answer. */
    fun onReplayAnswered(choice: ReplaySaveChoice) = modal.answer<WebDialog.ReplaySave, ReplaySaveChoice>(choice)

    /** Takes the shift of [line] on [axis] by [steps] (positive is right or down) into the step's progress, and whether it goes ahead;
     *  one that doesn't is drawn as blocked. A shift that goes ahead is followed by [afterShift]. */
    fun take(axis: Axis, line: Int, steps: Int): Boolean {
        val progress = progress ?: return true
        verdict = progress.check(axis, line, steps)
        if (verdict == Verdict.BLOCKED) blockedAxis = progress.asked?.axis else if (verdict != Verdict.IGNORED) blockedAxis = null
        if (verdict == Verdict.COUNTS || verdict == Verdict.DONE) return true
        game.render()
        return false
    }

    /** After a shift [allows] let through: tracks a finished step, and at step 5 starts the pause before step 6. */
    fun afterShift() {
        val progress = progress ?: return
        if (verdict != Verdict.DONE) return
        analytics.track("tutorial_step_completed", mapOf("step" to progress.completedStep, "blocked_shifts" to progress.completedBlocked))
        if (progress.step == TutorialScript.SOLVED_STEP) pause = scope.launch {
            delay(PAUSE_MS)
            finish()
        }
    }

    /** `[ Skip ]`: the tutorial is over, and the startup goes on as at a launch without it. */
    fun onSkip() = game.runFlow(Focus.TutorialSkip) {
        val progress = progress ?: return@runFlow
        pause?.cancel()
        end("skipped", progress.step, progress)
        game.closing = game.onBoard ?: game.closing
        game.startup(afterSkip = true)
    }

    fun onControls() = game.controls.open("tutorial", Focus.TutorialControls)

    fun onLabels() = game.labels.open("tutorial", Focus.TutorialLabels)

    /** The panel for this frame, `null` off the guided steps. */
    fun guidance(): Guidance? {
        val progress = progress ?: return null
        val step = progress.step
        val asked = progress.asked
        val text = when {
            blockedAxis != null -> TutorialText.blocked(blockedAxis!!)
            step == 3 && progress.begun -> TutorialText.again(viewModel.moveCount())
            else -> TutorialText.step(step, computer, viewModel.tileLabels())
        }
        return Guidance(step, text, controls = computer, highlight = asked?.let { TutorialHighlight(it.axis, it.line, it.arrow.takeIf { computer }) })
    }

    /** Step 6: the panel goes, Mordie's ending shows, and a share link that waited is asked about. */
    private fun finish() {
        val progress = progress ?: return
        end("completed", LAST_STEP, progress)
        game.share.holdStartupLink()
        game.share.openWaiting()
        game.render()
    }

    private fun end(result: String, lastStep: Int, progress: TutorialProgress) {
        this.progress = null
        blockedAxis = null
        preference.markSeen()
        analytics.track("tutorial_ended", mapOf("source" to source, "result" to result, "last_step" to lastStep, "blocked_shifts" to progress.totalBlocked))
    }

    private companion object {
        const val FIRST_LAUNCH = "first_launch"
        const val REPLAY = "replay"
        const val PAUSE_MS = 3_000L
        const val LAST_STEP = 6
    }
}
