package storage

/** Mordie's part of a save (GH-83): the game's [original] arrangement and Mordie's [moves] -
 *  its solution's length if [found], else the largest length ruled out. [solution] is that
 *  solution's moves, when known, and [solutionScreen] marks a save made on the solution screen
 *  (GH-126). A found answer is [provenShortest] unless the save says otherwise (the text format's line 9, the packed format's status), and [ruledOut] is
 *  then the largest length the exact search had ruled out (GH-224). Hand-written equals/hashCode because [original] is an [IntArray]. */
class SavedMordie(
    val original: IntArray,
    val moves: Int,
    val found: Boolean,
    val solution: List<SavedMove>? = null,
    val solutionScreen: Boolean = false,
    val provenShortest: Boolean = true,
    val ruledOut: Int = 0,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedMordie) return false
        return original.contentEquals(other.original) && moves == other.moves && found == other.found &&
            solution == other.solution && solutionScreen == other.solutionScreen &&
            provenShortest == other.provenShortest && ruledOut == other.ruledOut
    }

    override fun hashCode(): Int {
        var result = original.contentHashCode()
        result = 31 * result + moves
        result = 31 * result + found.hashCode()
        result = 31 * result + solution.hashCode()
        result = 31 * result + solutionScreen.hashCode()
        result = 31 * result + provenShortest.hashCode()
        result = 31 * result + ruledOut
        return result
    }

    override fun toString() = "SavedMordie(original=${original.contentToString()}, moves=$moves, found=$found, " +
        "solution=$solution, solutionScreen=$solutionScreen, provenShortest=$provenShortest, ruledOut=$ruledOut)"
}
