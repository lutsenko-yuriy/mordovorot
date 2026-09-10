package testing

import storage.SaveRepository
import storage.SavedBoard

/**
 * A [SaveRepository] test double backed by an in-memory map instead of the filesystem.
 * Used to exercise [presenter.PresenterImpl]'s save/load delegation without touching disk.
 */
class FakeSaveRepository(
    private val saves: MutableMap<String, SavedBoard> = mutableMapOf(),
) : SaveRepository {

    /** Every [save] call, in order, for asserting what was persisted and with what arguments. */
    val saveCalls = mutableListOf<Triple<String, IntArray, Int>>()

    /** When set, [save] throws this instead of persisting - simulates an unsafe name or a
     *  filesystem failure (disk full, read-only directory). */
    var saveException: Throwable? = null

    /** When set, [load] throws this instead of returning - simulates an unsafe name, a
     *  corrupted save file ([storage.SaveFileFormatException]), or a filesystem failure. */
    var loadException: Throwable? = null

    override fun listSaves(): List<String> = saves.keys.sorted()

    override fun exists(name: String): Boolean = saves.containsKey(name)

    override fun save(name: String, state: IntArray, squareSide: Int) {
        saveException?.let { throw it }
        saveCalls.add(Triple(name, state.copyOf(), squareSide))
        saves[name] = SavedBoard(squareSide, state.copyOf())
    }

    override fun load(name: String): SavedBoard? {
        loadException?.let { throw it }
        return saves[name]
    }
}
