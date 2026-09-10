package storage

/** Persistence contract for named board saves. */
interface SaveRepository {

    /** Save names, sorted; empty when nothing has been saved yet. */
    fun listSaves(): List<String>

    fun exists(name: String): Boolean

    fun save(name: String, state: IntArray, squareSide: Int)

    /** `null` when there's no save named [name]. */
    fun load(name: String): SavedBoard?
}
