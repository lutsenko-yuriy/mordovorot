package storage

/** A deserialized save file. Hand-written equals/hashCode because [state] is an [IntArray].
 *  [mordie] is `null` for a save with no Mordie data (pre-GH-83, or GH-74 Repair). */
data class SavedBoard(
    val squareSide: Int,
    val state: IntArray,
    val moveCount: Int = 0,
    val mordie: SavedMordie? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedBoard) return false
        return squareSide == other.squareSide && state.contentEquals(other.state) &&
            moveCount == other.moveCount && mordie == other.mordie
    }

    override fun hashCode(): Int {
        var result = squareSide
        result = 31 * result + state.contentHashCode()
        result = 31 * result + moveCount
        result = 31 * result + mordie.hashCode()
        return result
    }
}
