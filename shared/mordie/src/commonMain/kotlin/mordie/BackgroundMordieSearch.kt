package mordie

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.time.TimeSource

/**
 * The real [MordieSearch]: one coroutine on [Dispatchers.Default] that steps a
 * [BestSolutionSolver] for [WORK_MS], publishes a snapshot, then rests for [REST_MS] - about half
 * of one core, so the device isn't hogged (GH-41). The fast solver answers in the first slice,
 * and the search goes on until the answer is proven shortest (GH-222). The snapshot is the only
 * shared state; the game's event loop just reads it.
 */
class BackgroundMordieSearch(
    original: IntArray,
    ruledOut: Int,
    best: List<SolutionMove>? = null,
    /** Called with each published snapshot, from the search's thread: the Web Worker forwards them (GH-218). */
    private val onSnapshot: (SearchProgress) -> Unit = {},
) : MordieSearch {

    private val started = TimeSource.Monotonic.markNow()
    private val solver = BestSolutionSolver(original, ruledOut, best)

    /** The time of the first solution, which the elapsed time then stops at. */
    private var firstSolutionMs: Long? = null

    @Volatile
    private var snapshot = snapshotOf()

    private val job = CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        while (isActive && !solver.provenShortest) {
            val slice = TimeSource.Monotonic.markNow()
            while (isActive && slice.elapsedNow().inWholeMilliseconds < WORK_MS) {
                if (solver.step(NODES_PER_CHECK)) break
            }
            snapshot = snapshotOf()
            onSnapshot(snapshot)
            delay(REST_MS)
        }
    }

    /** Set by [cancel]: a slice still running at that moment may publish one more snapshot. */
    @Volatile
    private var frozen: SearchProgress? = null

    override fun progress(): SearchProgress = frozen ?: snapshot

    override fun cancel() {
        if (frozen == null) frozen = snapshot
        job.cancel()
    }

    internal suspend fun join() = job.join()

    private fun snapshotOf(): SearchProgress {
        val now = started.elapsedNow().inWholeMilliseconds
        if (firstSolutionMs == null && solver.bestLength != null) firstSolutionMs = now
        return solver.progress(firstSolutionMs ?: now)
    }

    /** The production [MordieSearchFactory]. */
    class Factory : MordieSearchFactory {
        override fun start(original: IntArray, ruledOut: Int, best: List<SolutionMove>?): MordieSearch =
            BackgroundMordieSearch(original, ruledOut, best)
    }

    private companion object {
        // Tuning knobs for the mobile port (GH-41): the share of one core is WORK / (WORK + REST).
        const val WORK_MS = 10L
        const val REST_MS = 10L
        const val NODES_PER_CHECK = 1_000
    }
}
