package web

import kotlin.math.ceil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** GH-343: a move pressed while the board's label wave plays waits for it, so a shift doesn't cut the wave. Only the latest press is kept.
 *  A toolbar action or a drag drops it. Without a wave (none started, or reduced motion) nothing waits. */
internal class WebMoves(private val scope: CoroutineScope, private val renderer: WebRenderer) {

    private var held: Job? = null

    /** Makes [move] now, or when the wave ends if it is [allowed] then; whether it waits. */
    fun run(allowed: () -> Boolean, move: () -> Unit): Boolean {
        drop()
        var left: Double = renderer.waveRemainingMs() ?: run {
            move()
            return false
        }
        held = scope.launch {
            while (true) {
                delay(ceil(left).toLong())
                left = renderer.waveRemainingMs() ?: break
            }
            held = null
            if (allowed()) move()
        }
        return true
    }

    /** Forgets the held move. */
    fun drop() {
        held?.cancel()
        held = null
    }
}
