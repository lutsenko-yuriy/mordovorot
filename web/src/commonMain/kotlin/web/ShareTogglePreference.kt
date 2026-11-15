package web

import storage.PreferenceRepository

/** Whether one of the Share dialog's toggles is on (GH-267, GH-272), kept under [key] (`share_labels` for
 *  `Include my tile labels`, `share_progress` for `Include my progress`) as `on` or `off`, like [PlayModePreference]:
 *  a failed read means off, a failed write isn't remembered. */
class ShareTogglePreference(private val store: PreferenceRepository, private val key: String) {

    fun load(): Boolean = try {
        store.get(key) == ON
    } catch (e: Exception) {
        false
    }

    /** Whether the write succeeded. */
    fun save(on: Boolean): Boolean = try {
        store.set(key, if (on) ON else OFF)
        true
    } catch (e: Exception) {
        false
    }

    private companion object {
        const val ON = "on"
        const val OFF = "off"
    }
}
