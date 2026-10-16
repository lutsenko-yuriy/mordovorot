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
 * The real [MordieSearch]: one coroutine on [Dispatchers.Default] that steps an
 * [IdaStarSolver] for [WORK_MS], publishes a snapshot, then rests for [REST_MS] - about half of
 * one core, so the device isn't hogged (GH-41). The snapshot is the only shared state; the
 * game's event loop just reads it.
 */
class BackgroundMordieSearch(original: IntArray, ruledOut: Int) : MordieSearch {

    private val started = TimeSource.Monotonic.markNow()
    private val solver = IdaStarSolver(original, ruledOut)

    @Volatile
    private var snapshot = snapshotOf(solver)

    private val job = CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        while (isActive && solver.solutionLength == null) {
            val slice = TimeSource.Monotonic.markNow()
            while (isActive && slice.elapsedNow().inWholeMilliseconds < WORK_MS) {
                if (solver.step(NODES_PER_CHECK)) break
            }
            snapshot = snapshotOf(solver)
            delay(REST_MS)
        }
    }

    override fun progress(): SearchProgress = snapshot

    override fun cancel() = job.cancel()

    internal suspend fun join() = job.join()

    private fun snapshotOf(solver: IdaStarSolver): SearchProgress {
        val length = solver.solutionLength
        return SearchProgress(length ?: solver.ruledOut, found = length != null, started.elapsedNow().inWholeMilliseconds)
    }

    /** The production [MordieSearchFactory]. */
    class Factory : MordieSearchFactory {
        override fun start(original: IntArray, ruledOut: Int): MordieSearch = BackgroundMordieSearch(original, ruledOut)
    }

    private companion object {
        // Tuning knobs for the mobile port (GH-41): the share of one core is WORK / (WORK + REST).
        const val WORK_MS = 10L
        const val REST_MS = 10L
        const val NODES_PER_CHECK = 1_000
    }
}
