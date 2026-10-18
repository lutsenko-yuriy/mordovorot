package web

import kotlin.math.pow

/** Messages shown one at a time, like a chat (GH-110): the old one rises out and fades, and only
 *  once it has gone does the next rise in from below and fade in. A phase under way always
 *  finishes; then the latest message given to [show] follows. Times are in milliseconds; the
 *  durations can change between calls, and 0 shows each message at once. */
class MessageFeed<T : Any>(var outMs: Double, var inMs: Double) {

    /** [message] [rise]s from its place: negative is up, by a share of the way out; positive is
     *  below, by a share of the way in. */
    data class Frame<T>(val message: T?, val rise: Double, val alpha: Double)

    private enum class Phase { OUT, IN }

    /** On screen, or rising in. */
    private var shown: T? = null
    private var target: T? = null
    private var phase: Phase? = null
    private var startedAt = 0.0

    fun show(message: T?, now: Double) {
        advance(now)
        if (message == target) return
        target = message
        if (phase == null) next(now)
    }

    fun moving(now: Double): Boolean {
        advance(now)
        return phase != null
    }

    fun at(now: Double): Frame<T> {
        advance(now)
        val phase = phase ?: return Frame(shown, 0.0, if (shown == null) 0.0 else 1.0)
        // A frame's timestamp can come just before the phase began.
        val progress = ((now - startedAt) / duration(phase)).coerceIn(0.0, 1.0)
        return when (phase) {
            Phase.OUT -> Frame(shown, -progress.pow(2), 1 - progress)
            Phase.IN -> Frame(shown, (1 - progress).pow(3), progress)
        }
    }

    private fun advance(now: Double) {
        while (true) {
            val phase = phase ?: return
            val endsAt = startedAt + duration(phase)
            if (now < endsAt) return
            if (phase == Phase.OUT) shown = null
            this.phase = null
            next(endsAt)
        }
    }

    private fun next(at: Double) {
        when {
            shown != null && shown != target -> start(Phase.OUT, at)
            shown == null && target != null -> {
                shown = target
                start(Phase.IN, at)
            }
        }
    }

    private fun start(phase: Phase, at: Double) {
        this.phase = phase
        startedAt = at
    }

    private fun duration(phase: Phase) = when (phase) {
        Phase.OUT -> outMs
        Phase.IN -> inMs
    }
}
