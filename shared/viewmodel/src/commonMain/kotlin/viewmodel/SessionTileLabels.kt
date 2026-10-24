package viewmodel

/** Where the current labels came from (GH-139); lowercased, `tile_labels_changed.from_source`. */
enum class TileLabelsSource { DEFAULT, PREFERENCE, SAVE }

/** The session's tile labels (GH-139): a save's labels beat the player's preference, which beats
 *  [TileLabels.DEFAULT]. One per session, like [CurrentSaveName], so a mode switch keeps them.
 *  The preference is read once, here. */
class SessionTileLabels(private val preference: TileLabelsPreference) {

    var preferred: TileLabels? = preference.load()
        private set

    var current: TileLabels = TileLabels.DEFAULT
        private set

    var source: TileLabelsSource = TileLabelsSource.DEFAULT
        private set

    /** What a save's labels are compared with: the preference, or the default without one. */
    val effectivePreference: TileLabels get() = preferred ?: TileLabels.DEFAULT

    init {
        resetToPreference()
    }

    /** A new game's labels. */
    fun resetToPreference() {
        current = effectivePreference
        source = if (preferred == null) TileLabelsSource.DEFAULT else TileLabelsSource.PREFERENCE
    }

    /** A loaded (or repaired) save's labels; [fromSave] is `null` for a save with none. */
    fun applySave(fromSave: TileLabels?) {
        if (fromSave == null) return resetToPreference()
        current = fromSave
        source = TileLabelsSource.SAVE
    }

    /** The dialog's OK: returns `false`, changing nothing, when [to] is already shown. A failed
     *  preference write still relabels; the choice just isn't remembered after this session. */
    fun choose(to: TileLabels): Boolean {
        if (to == current) return false
        current = to
        preferred = to
        source = TileLabelsSource.PREFERENCE
        preference.save(to)
        return true
    }
}
