package web.storage

import storage.SaveFileCodec
import storage.SaveNames
import storage.SaveRepository
import storage.SavedBoard
import storage.SavedMordie

/** The page's saves (GH-110): one [store] entry per save under `mordovorot.save.<name>`, in the
 *  terminal's text format, with the terminal's name rules. */
class BrowserSaveRepository(private val store: KeyValueStore) : SaveRepository {

    override fun listSaves(): List<String> =
        store.keys().filter { it.startsWith(PREFIX) }.map { it.removePrefix(PREFIX) }.filter(SaveNames::isSafe).sorted()

    override fun exists(name: String): Boolean = store.get(keyFor(name)) != null

    override fun save(name: String, state: IntArray, squareSide: Int, moveCount: Int, mordie: SavedMordie?, labels: String?) {
        val key = keyFor(name)
        store.set(key, SaveFileCodec.encode(squareSide, state, moveCount, mordie, labels))
    }

    override fun isValidName(name: String): Boolean = SaveNames.isSafe(name)

    override fun load(name: String): SavedBoard? = store.get(keyFor(name))?.let { SaveFileCodec.decode(name, it) }

    private fun keyFor(name: String): String {
        SaveNames.requireSafe(name)
        return PREFIX + name
    }

    private companion object {
        const val PREFIX = "mordovorot.save."
    }
}
