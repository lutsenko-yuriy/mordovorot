package viewmodel

import storage.PreferenceRepository

/** The player's chosen keyboard layout (GH-170), kept like [TileLabelsPreference]: a failed read
 *  means no preference, a failed write isn't remembered. */
class KeyboardLayoutPreference(private val store: PreferenceRepository) {

    /** `null` when nothing is stored, the name is unknown, or the read fails. */
    fun load(): KeyboardLayout? = try {
        KeyboardLayout.fromWireName(store.get(KEY))
    } catch (e: Exception) {
        null
    }

    /** Whether the write succeeded. */
    fun save(layout: KeyboardLayout): Boolean = try {
        store.set(KEY, layout.wireName)
        true
    } catch (e: Exception) {
        false
    }

    private companion object {
        const val KEY = "keyboard_layout"
    }
}
