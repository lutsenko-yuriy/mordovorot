package storage

/** Mordie's part of a save (GH-83): the game's [original] arrangement and Mordie's [moves] -
 *  its solution's length if [found], else the largest length ruled out. [solution] is that
 *  solution's moves, when known, and [solutionScreen] marks a save made on the solution screen
 *  (GH-126). Hand-written equals/hashCode because [original] is an [IntArray]. */
class SavedMordie(
    val original: IntArray,
    val moves: Int,
    val found: Boolean,
    val solution: List<SavedMove>? = null,
    val solutionScreen: Boolean = false,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedMordie) return false
        return original.contentEquals(other.original) && moves == other.moves && found == other.found &&
            solution == other.solution && solutionScreen == other.solutionScreen
    }

    override fun hashCode(): Int {
        var result = original.contentHashCode()
        result = 31 * result + moves
        result = 31 * result + found.hashCode()
        result = 31 * result + solution.hashCode()
        result = 31 * result + solutionScreen.hashCode()
        return result
    }

    override fun toString() = "SavedMordie(original=${original.contentToString()}, moves=$moves, found=$found, " +
        "solution=$solution, solutionScreen=$solutionScreen)"
}
