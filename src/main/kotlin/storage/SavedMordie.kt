package storage

/** Mordie's part of a save (GH-83): the game's [original] arrangement and Mordie's [moves] -
 *  its solution's length if [found], else the largest length ruled out. Hand-written
 *  equals/hashCode because [original] is an [IntArray]. */
class SavedMordie(val original: IntArray, val moves: Int, val found: Boolean) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedMordie) return false
        return original.contentEquals(other.original) && moves == other.moves && found == other.found
    }

    override fun hashCode(): Int {
        var result = original.contentHashCode()
        result = 31 * result + moves
        result = 31 * result + found.hashCode()
        return result
    }

    override fun toString() = "SavedMordie(original=${original.contentToString()}, moves=$moves, found=$found)"
}
