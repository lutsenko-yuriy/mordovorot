package mordie

/**
 * Mordie's two solvers behind one steppable object (GH-222): the [ThreeCycleSolver] answers on
 * the first [step], then the [IdaStarSolver] searches for the minimum. It keeps the best
 * solution so far - the exact search replaces the fast answer when it finds a shorter one - and
 * says whether that answer is [provenShortest].
 *
 * [ruledOut] (a saved N) only resumes the exact search: the fast solver always starts over.
 */
class BestSolutionSolver(original: IntArray, ruledOut: Int = -1) {

    private val fast = ThreeCycleSolver(original)
    private val exact = IdaStarSolver(original, ruledOut)

    /** The best solution's length, once any solver has one. */
    var bestLength: Int? = null
        private set

    var solution: List<SolutionMove>? = null
        private set

    /** Which solver gave the first solution; stays put when the exact search later improves it. */
    var source: SolutionSource? = null
        private set

    /** True when the exact search has finished, or has ruled out every length below the best. */
    var provenShortest = false
        private set

    /** The largest solution length proven impossible so far. */
    val ruledOut: Int get() = exact.ruledOut

    /** Spends up to [maxNodes] exact-search nodes (the first call also runs the fast solver); true once proven. */
    fun step(maxNodes: Int): Boolean {
        if (provenShortest) return true
        if (exact.solutionLength != null) {
            takeExact()
        } else {
            if (bestLength == null) takeFast()
            exact.step(maxNodes)
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
}
