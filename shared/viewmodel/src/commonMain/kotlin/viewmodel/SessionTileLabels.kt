package viewmodel

import board_model.BoardSize

/** Where the current labels came from (GH-139); lowercased, `tile_labels_changed.from_source`.
 *  [LETTERS_UNAVAILABLE]: numbers stand in for a scheme too small for the board (GH-160). */
enum class TileLabelsSource { DEFAULT, PREFERENCE, SAVE, LETTERS_UNAVAILABLE }

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
        resetToPreference(BoardSize.DEFAULT)
    }

    /** A new [side]x[side] game's labels. */
    fun resetToPreference(side: Int) =
        show(effectivePreference, if (preferred == null) TileLabelsSource.DEFAULT else TileLabelsSource.PREFERENCE, side)

    /** A loaded (or repaired) save's labels; [fromSave] is `null` for a save with none. */
    fun applySave(fromSave: TileLabels?, side: Int) {
        if (fromSave == null) return resetToPreference(side)
        show(fromSave, TileLabelsSource.SAVE, side)
    }

    /** Numbers stand in for a scheme that doesn't fit [side]; the preference is kept. */
    private fun show(labels: TileLabels, from: TileLabelsSource, side: Int) {
        val fits = labels.fits(side)
        current = if (fits) labels else TileLabels.NUMBERS
        source = if (fits) from else TileLabelsSource.LETTERS_UNAVAILABLE
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
