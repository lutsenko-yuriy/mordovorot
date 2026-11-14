package web

import storage.PreferenceRepository

/** Whether the Share dialog's `Include my tile labels` toggle is on (GH-267), kept under `share_labels` as `on`
 *  or `off`, like [PlayModePreference]: a failed read means off, a failed write isn't remembered. */
class ShareLabelsPreference(private val store: PreferenceRepository) {

    fun load(): Boolean = try {
        store.get(KEY) == ON
    } catch (e: Exception) {
        false
    }

    /** Whether the write succeeded. */
    fun save(on: Boolean): Boolean = try {
        store.set(KEY, if (on) ON else OFF)
        true
    } catch (e: Exception) {
        false
    }

    private companion object {
        const val KEY = "share_labels"
        const val ON = "on"
        const val OFF = "off"
    }
}
