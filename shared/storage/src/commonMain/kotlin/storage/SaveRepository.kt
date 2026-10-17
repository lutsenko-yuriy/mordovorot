package storage

/**
 * Persistence contract for named board saves.
 *
 * [save] and [load] throw [IllegalArgumentException] for an unsafe/blank name or (on
 * [save]) a state that isn't a valid `squareSide`-sized permutation; [load] additionally
 * throws an implementation-specific format exception for a corrupted file. Filesystem
 * errors propagate as the platform's I/O exception.
 */
interface SaveRepository {

    /** Save names, sorted; empty when nothing has been saved yet. */
    fun listSaves(): List<String>

    fun exists(name: String): Boolean

    /** [moveCount] is the board's move count (GH-62); must be non-negative. [mordie] is `null`
     *  to save no Mordie data (GH-83); its original must be a valid arrangement too. */
    fun save(name: String, state: IntArray, squareSide: Int, moveCount: Int = 0, mordie: SavedMordie? = null)

    /** Whether [name] would be accepted by [save]/[load]/[exists] rather than throwing
     *  [IllegalArgumentException] - lets a caller validate up front instead of relying on
     *  catching the exception, e.g. to tell "the name was never valid" apart from "the save
     *  itself failed" (audit round 3 on GH-12/PR #15). */
    fun isValidName(name: String): Boolean

    /** `null` when there's no save named [name]. */
    fun load(name: String): SavedBoard?
}
