package storage

/** A deserialized save file. Hand-written equals/hashCode because [state] is an [IntArray].
 *  [mordie] is `null` for a save with no Mordie data (pre-GH-83, or GH-74 Repair). [labels] is
 *  the tile labels' wire name (GH-139), `null` for an older save. */
data class SavedBoard(
    val squareSide: Int,
    val state: IntArray,
    val moveCount: Int = 0,
    val mordie: SavedMordie? = null,
    val labels: String? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SavedBoard) return false
        return squareSide == other.squareSide && state.contentEquals(other.state) &&
            moveCount == other.moveCount && mordie == other.mordie && labels == other.labels
    }

    override fun hashCode(): Int {
        var result = squareSide
        result = 31 * result + state.contentHashCode()
        result = 31 * result + moveCount
        result = 31 * result + mordie.hashCode()
        result = 31 * result + labels.hashCode()
        return result
    }
}
