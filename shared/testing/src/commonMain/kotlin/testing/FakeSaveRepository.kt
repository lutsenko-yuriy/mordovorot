package testing

import storage.SaveRepository
import storage.SavedBoard
import storage.SavedMordie

/**
 * A [SaveRepository] test double backed by an in-memory map instead of the filesystem.
 * Used to exercise [viewmodel.ViewModelImpl]'s save/load delegation without touching disk.
 */
class FakeSaveRepository(
    private val saves: MutableMap<String, SavedBoard> = mutableMapOf(),
) : SaveRepository {

    /** Every [save] call, in order, for asserting what was persisted and with what arguments. */
    val saveCalls = mutableListOf<Triple<String, IntArray, Int>>()

    /** The moveCount passed to each [save] call, parallel to [saveCalls]. */
    val savedMoveCounts = mutableListOf<Int>()

    /** The Mordie data passed to each [save] call, parallel to [saveCalls]. */
    val savedMordies = mutableListOf<SavedMordie?>()

    /** The labels passed to each [save] call, parallel to [saveCalls]. */
    val savedLabels = mutableListOf<String?>()

    /** When set, [save] throws this instead of persisting - simulates an unsafe name or a
     *  filesystem failure (disk full, read-only directory). */
    var saveException: Throwable? = null

    /** When set, [load] throws this instead of returning - simulates an unsafe name, a
     *  corrupted save file ([storage.SaveFileFormatException]), or a filesystem failure. */
    var loadException: Throwable? = null

    /** When set, [exists] throws this instead of returning - simulates an unsafe name (e.g.
     *  a path separator typed mid-name into the mouse-driven Save dialog, GH-3) or a
     *  filesystem failure. */
    var existsException: Throwable? = null

    /** When set, [listSaves] throws this instead of returning - simulates an unreadable
     *  store, which a real repository's `listSaves` can raise. */
    var listSavesException: Throwable? = null

    /** Every name passed to [delete], in order (GH-251). */
    val deleteCalls = mutableListOf<String>()

    /** When set, [delete] throws this instead of deleting - simulates a storage failure. */
    var deleteException: Throwable? = null

    override fun listSaves(): List<String> {
        listSavesException?.let { throw it }
        return saves.keys.sorted()
    }

    override fun exists(name: String): Boolean {
        existsException?.let { throw it }
        return saves.containsKey(name)
    }

    override fun save(name: String, state: IntArray, squareSide: Int, moveCount: Int, mordie: SavedMordie?, labels: String?) {
        saveException?.let { throw it }
        saveCalls.add(Triple(name, state.copyOf(), squareSide))
        savedMoveCounts.add(moveCount)
        savedMordies.add(mordie)
        savedLabels.add(labels)
        saves[name] = SavedBoard(squareSide, state.copyOf(), moveCount, mordie, labels)
    }

    override fun load(name: String): SavedBoard? {
        loadException?.let { throw it }
        return saves[name]
    }

    override fun delete(name: String): Boolean {
        deleteCalls.add(name)
        deleteException?.let { throw it }
        return saves.remove(name) != null
    }

    /** Mirrors [storage.SaveNames]' safety rule, so tests exercising name validation
     *  don't need the real repository. */
    override fun isValidName(name: String): Boolean =
        name.isNotBlank() &&
            !name.contains('/') && !name.contains('\\') &&
            name.split('/', '\\').none { it == ".." }
}
