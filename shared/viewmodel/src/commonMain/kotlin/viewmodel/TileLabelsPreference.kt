package viewmodel

import storage.PreferenceRepository

/** The player's chosen tile labels (GH-139), kept in a [PreferenceRepository]. A storage error
 *  never reaches the player: a failed read means no preference, a failed write isn't remembered. */
class TileLabelsPreference(private val store: PreferenceRepository) {

    /** `null` when nothing is stored, the name is unknown, or the read fails. */
    fun load(): TileLabels? = try {
        TileLabels.fromWireName(store.get(KEY))
    } catch (e: Exception) {
        null
    }

    /** Whether the write succeeded. */
    fun save(labels: TileLabels): Boolean = try {
        store.set(KEY, labels.wireName)
        true
    } catch (e: Exception) {
        false
    }

    private companion object {
        const val KEY = "tile_labels"
    }
}
