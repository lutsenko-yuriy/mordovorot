package mordie

/** A snapshot of Mordie's search. [moves] is the largest length ruled out so far, or the
 *  solution's length once [found]; [elapsedMs] stops counting at the moment it was found.
 *  [solution] is the solution's moves once [found] (GH-126). */
data class SearchProgress(
    val moves: Int,
    val found: Boolean,
    val elapsedMs: Long,
    val solution: List<SolutionMove>? = null,
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
    /** [ruledOut] is a saved N to resume above, or -1 for a fresh search. */
    fun start(original: IntArray, ruledOut: Int): MordieSearch

    companion object {
        /** Never searches: progress stays at the given [ruledOut], never found. */
        val NONE = MordieSearchFactory { _, ruledOut ->
            object : MordieSearch {
                private val progress = SearchProgress(moves = ruledOut, found = false, elapsedMs = 0)
                override fun progress() = progress
                override fun cancel() = Unit
            }
        }
    }
}
