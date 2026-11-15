package web.tutorial

import kotlin.math.abs
import viewmodel.Direction
import web.drag.Axis
import web.drag.DragShift
import web.drag.direction

/** What a shift means to the tutorial (GH-288). */
enum class Verdict {
    /** Part of the asked shift: more tiles are still asked. */
    COUNTS,

    /** The last tiles of the asked shift: the step is over. */
    DONE,

    /** Not what is asked: the board stays as it was. */
    BLOCKED,

    /** Not a shift (a drag released at 0 tiles), or the guided steps are over. */
    IGNORED,
}

/** Where the player is in the tutorial's four guided steps, and what each shift they make does to it (GH-288). */
class TutorialProgress {

    var step = 1
        private set

    /** Blocked shifts on [step]; [totalBlocked] is over the whole tutorial. */
    var blockedInStep = 0
        private set
    var totalBlocked = 0
        private set

    /** The step [check] last returned [Verdict.DONE] for, and the shifts blocked on it: [step] has moved on by then. */
    var completedStep = 0
        private set
    var completedBlocked = 0
        private set

    private var remaining = asked?.tiles ?: 0
    private var chosen: Direction? = null

    /** The shift [step] asks for, with the tiles still to go; `null` once the guided steps are over. */
    val asked: AskedShift? get() = TutorialScript.shifts.getOrNull(step - 1)

    val tilesLeft get() = remaining

    /** Whether [step] has had part of its shift made. */
    val begun get() = asked?.let { remaining < it.tiles } == true

    /** Takes a shift of [line] on [axis] by [rawSteps] tiles (positive is right or down), as the viewModel would.
     *  A drag's count is read the short way round, so 3 right is 1 left. */
    fun check(axis: Axis, line: Int, rawSteps: Int): Verdict {
        val asked = asked
        val steps = shortWay(rawSteps)
        if (asked == null || steps == 0) return Verdict.IGNORED
        val direction = DragShift(axis, line, steps).direction
        val tiles = abs(steps)
        val wanted = chosen ?: asked.arrow.direction
        val turnsEitherWay = asked.halfTurn && !begun
        val counts = axis == asked.axis && line == asked.line && tiles <= remaining && (direction == wanted || turnsEitherWay)
        if (!counts) return blocked()
        chosen = direction
        remaining -= tiles
        if (remaining > 0) return Verdict.COUNTS
        advance()
        return Verdict.DONE
    }

    private fun blocked(): Verdict {
        blockedInStep++
        totalBlocked++
        return Verdict.BLOCKED
    }

    private fun shortWay(steps: Int): Int {
        val turned = (steps % TutorialScript.SIDE + TutorialScript.SIDE) % TutorialScript.SIDE
        return if (turned > TutorialScript.SIDE / 2) turned - TutorialScript.SIDE else turned
    }

    private fun advance() {
        completedStep = step
        completedBlocked = blockedInStep
        step++
        blockedInStep = 0
        chosen = null
        remaining = asked?.tiles ?: 0
    }
}
