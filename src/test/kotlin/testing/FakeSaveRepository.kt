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

    override fun listSaves(): List<String> = saves.keys.sorted()

    override fun exists(name: String): Boolean = saves.containsKey(name)

    override fun save(name: String, state: IntArray, squareSide: Int) {
        saveCalls.add(Triple(name, state.copyOf(), squareSide))
        saves[name] = SavedBoard(squareSide, state.copyOf())
    }

    override fun load(name: String): SavedBoard? = saves[name]
}
