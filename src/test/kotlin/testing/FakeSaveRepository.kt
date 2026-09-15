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

    /** When set, [exists] throws this instead of returning - simulates an unsafe name (e.g.
     *  a path separator typed mid-name into the mouse-driven Save dialog, GH-3) or a
     *  filesystem failure. */
    var existsException: Throwable? = null

    /** When set, [listSaves] throws this instead of returning - simulates an unreadable
     *  saves directory (e.g. permissions), which [storage.FileSaveRepository.listSaves]
     *  can raise via [java.io.IOException]. */
    var listSavesException: Throwable? = null

    override fun listSaves(): List<String> {
        listSavesException?.let { throw it }
        return saves.keys.sorted()
    }

    override fun exists(name: String): Boolean {
        existsException?.let { throw it }
        return saves.containsKey(name)
    }

    override fun save(name: String, state: IntArray, squareSide: Int) {
        saveException?.let { throw it }
        saveCalls.add(Triple(name, state.copyOf(), squareSide))
        saves[name] = SavedBoard(squareSide, state.copyOf())
    }

    override fun load(name: String): SavedBoard? {
        loadException?.let { throw it }
        return saves[name]
    }

    /** Mirrors [storage.FileSaveRepository]'s real safety rule, so tests exercising name
     *  validation (e.g. [presenter.PresenterImpl.exitGame]'s re-prompt loop) don't need the
     *  real filesystem-backed repository. */
    override fun isValidName(name: String): Boolean =
        name.isNotBlank() &&
            !name.contains('/') && !name.contains('\\') &&
            name.split('/', '\\').none { it == ".." }
}
