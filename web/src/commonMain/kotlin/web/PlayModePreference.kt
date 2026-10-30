package web

import storage.PreferenceRepository

/** The player's chosen [PlayMode] (GH-171), kept like `KeyboardLayoutPreference`: a failed read
 *  means no preference, a failed write isn't remembered. */
class PlayModePreference(private val store: PreferenceRepository) {

    /** `null` when nothing is stored, the name is unknown, or the read fails. */
    fun load(): PlayMode? = try {
        PlayMode.fromWireName(store.get(KEY))
    } catch (e: Exception) {
        null
    }

    /** Whether the write succeeded. */
    fun save(mode: PlayMode): Boolean = try {
        store.set(KEY, mode.wireName)
        true
    } catch (e: Exception) {
        false
    }

    private companion object {
        const val KEY = "input_mode"
    }
}
