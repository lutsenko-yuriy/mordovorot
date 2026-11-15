package web

import storage.PreferenceRepository

/** Whether the first-launch tutorial has been seen (GH-288), kept under `tutorial` as `seen`. A failed read counts
 *  as seen, so a storage that throws doesn't show the tutorial on every launch; a failed write isn't remembered. */
class TutorialPreference(private val store: PreferenceRepository) {

    fun seen(): Boolean = try {
        store.get(KEY) == SEEN
    } catch (e: Exception) {
        true
    }

    /** Whether the write succeeded. */
    fun markSeen(): Boolean = try {
        store.set(KEY, SEEN)
        true
    } catch (e: Exception) {
        false
    }

    private companion object {
        const val KEY = "tutorial"
        const val SEEN = "seen"
    }
}
