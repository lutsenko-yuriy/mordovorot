package storage

/** A deserialized save file. Hand-written equals/hashCode because [state] is an [IntArray]. */
data class SavedBoard(val squareSide: Int, val state: IntArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedBoard) return false
        return squareSide == other.squareSide && state.contentEquals(other.state)
    }

    override fun hashCode(): Int {
        var result = squareSide
        result = 31 * result + state.contentHashCode()
        return result
    }
}
