package storage

/**
 * Persistence contract for named board saves.
 *
 * [save] and [load] throw [IllegalArgumentException] for an unsafe/blank name or (on
 * [save]) a state that isn't a valid `squareSide`-sized permutation; [load] additionally
 * throws [SaveFileFormatException] for a corrupted file. Filesystem errors propagate as
 * [java.io.IOException].
 */
interface SaveRepository {

    /** Save names, sorted; empty when nothing has been saved yet. */
    fun listSaves(): List<String>

    fun exists(name: String): Boolean

    fun save(name: String, state: IntArray, squareSide: Int)

    /** `null` when there's no save named [name]. */
    fun load(name: String): SavedBoard?
}
