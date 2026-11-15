package mordie

/**
 * Mordie's two solvers behind one steppable object (GH-222): the [ThreeCycleSolver] answers on
 * the first [step], then the [IdaStarSolver] searches for the minimum. It keeps the best
 * solution so far - the exact search replaces the fast answer when it finds a shorter one - and
 * says whether that answer is [provenShortest].
 *
 * [ruledOut] (a saved N) resumes the exact search. A kept [best] (GH-224) is the answer from the
 * start, so the fast solver is skipped; without one it always starts over.
 *
 * The exact search runs only up to [EXACT_SEARCH_MAX_SIDE] (GH-260): it never finishes on bigger
 * boards and only heats the device. There the fast answer, or a kept one, is final: [done].
 *
 * On those boards a [PatternDatabase] is built in slices alongside the exact search (GH-282), which
 * switches to its bound once it is ready: a short board is proven at once, and a long one faster
 * than the plain bound would.
 */
class BestSolutionSolver(original: IntArray, ruledOut: Int = -1, best: List<SolutionMove>? = null) {

    private val fast = ThreeCycleSolver(original)
    private val exact = IdaStarSolver(original, ruledOut)
    private val exactRuns = MoveMetricHeuristic.sideOf(original) <= EXACT_SEARCH_MAX_SIDE
    private val patterns = if (exactRuns) PatternDatabase(MoveMetricHeuristic.sideOf(original)) else null

    /** The best solution's length, once any solver has one. */
    var bestLength: Int? = best?.size
        private set

    var solution: List<SolutionMove>? = best
        private set

    /** Which solver gave the first solution; stays put when the exact search later improves it. */
    var source: SolutionSource? = null
        private set

    /** True when the exact search has finished, or has ruled out every length below the best. */
    var provenShortest = false
        private set

    /** True when no further [step] can change the answer: it is proven, or the exact search doesn't run here. */
    val done: Boolean get() = provenShortest || (!exactRuns && bestLength != null)

    /** The largest solution length proven impossible so far. */
    val ruledOut: Int get() = exact.ruledOut

    /** Spends up to [maxNodes] exact-search nodes (the first call also runs the fast solver); true once proven. */
    fun step(maxNodes: Int): Boolean {
        if (provenShortest) return true
        if (exact.solutionLength != null) {
            takeExact()
        } else {
            if (bestLength == null) takeFast()
            if (exactRuns) {
                buildPatterns(maxNodes)
                exact.step(maxNodes)
            }
            if (exact.solutionLength != null) takeExact()
        }
        if (bestLength?.let { exact.ruledOut >= it - 1 } == true) provenShortest = true
        return provenShortest
    }

    fun progress(elapsedMs: Long) = SearchProgress(
        moves = bestLength ?: exact.ruledOut,
        found = bestLength != null,
        elapsedMs = elapsedMs,
        solution = solution,
        ruledOut = exact.ruledOut,
        provenShortest = provenShortest,
        source = source,
    )

    /** Builds a slice of the table, about as long as [maxNodes] nodes; hands it to the search when it is whole. */
    private fun buildPatterns(maxNodes: Int) {
        val table = patterns?.takeIf { !it.done } ?: return
        table.step(maxOf(1, maxNodes / NODES_PER_STATE))
        if (table.done) exact.usePatterns(table)
    }

    private fun takeFast() {
        fast.step(Int.MAX_VALUE)
        keep(fast.solutionLength!!, SolutionSource.FAST_SOLVER) { fast.solution() }
    }

    private fun takeExact() {
        keep(exact.solutionLength!!, SolutionSource.EXACT_SEARCH) { exact.solution() }
        provenShortest = true
    }

    private fun keep(length: Int, from: SolutionSource, moves: () -> List<SolutionMove>) {
        if (bestLength != null && length >= bestLength!!) return
        if (source == null) source = from
        bestLength = length
        solution = moves()
    }

    companion object {
        /** The largest board side the exact search runs on, 4x4 (GH-260). */
        const val EXACT_SEARCH_MAX_SIDE = 4

        /** Building one table state costs about this many search nodes (measured with scripts compiled and not). */
        private const val NODES_PER_STATE = 5
    }
}
