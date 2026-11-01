package mordie

/** Where a solution came from (GH-222): the fast solver or the exact search. */
enum class SolutionSource(val analyticsName: String) {
    FAST_SOLVER("fast_solver"),
    EXACT_SEARCH("exact_search"),
}

/** A snapshot of Mordie's search. [found] means a solution exists (GH-222): [moves] is then the
 *  length of the best one so far, otherwise the largest length ruled out. [ruledOut] is the
 *  largest length ruled out either way, [provenShortest] says whether the best solution is the
 *  minimum, and [source] which solver gave the first one. [elapsedMs] stops counting at the
 *  first solution. [solution] is the best solution's moves once [found] (GH-126).
 *  The defaults keep the pre-GH-222 meaning: a found solution was the exact minimum. */
data class SearchProgress(
    val moves: Int,
    val found: Boolean,
    val elapsedMs: Long,
    val solution: List<SolutionMove>? = null,
    val ruledOut: Int = if (found) moves - 1 else moves,
    val provenShortest: Boolean = found,
    val source: SolutionSource? = if (found) SolutionSource.EXACT_SEARCH else null,
)

/** One running search for the shortest solution of a game's original arrangement. */
interface MordieSearch {
    /** Safe to call from any thread. */
    fun progress(): SearchProgress

    /** Stops the search; [progress] keeps its last value. */
    fun cancel()
}

/** Starts a [MordieSearch] - the seam tests replace so no real search runs. */
fun interface MordieSearchFactory {
    /** [ruledOut] is a saved N to resume above, or -1 for a fresh search; [best] is the saved
     *  solution to start from, if any (GH-224). */
    fun start(original: IntArray, ruledOut: Int, best: List<SolutionMove>?): MordieSearch

    companion object {
        /** Never searches: progress stays at the given [ruledOut], never found. */
        val NONE = MordieSearchFactory { _, ruledOut, _ ->
            object : MordieSearch {
                private val progress = SearchProgress(moves = ruledOut, found = false, elapsedMs = 0)
                override fun progress() = progress
                override fun cancel() = Unit
            }
        }
    }
}

/** Starts a search with no kept solution. */
fun MordieSearchFactory.start(original: IntArray, ruledOut: Int): MordieSearch = start(original, ruledOut, null)
