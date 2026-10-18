package web

/** One frame of the page (GH-110): what [WebRenderer] draws. [tiles] are 1-based, row by row. */
data class WebState(
    val side: Int,
    val tiles: List<Int>,
    val moves: Int,
    /** Mordie's line while playing, its ending (or `Congratulations ✓`) once solved; `null` when silent. */
    val mordie: String?,
    val solved: Boolean,
    /** The last status message (D4), cleared by the next action. */
    val status: String?,
    val dialog: WebDialog?,
) {
    val arrowsEnabled: Boolean get() = !solved
}

/** The modal dialog open over the board. */
sealed interface WebDialog {
    /** The new-game size picker, with Cancel. */
    data class Size(val sizes: List<Int>) : WebDialog

    /** The name being typed, with the overwrite warning when a save by that name exists. */
    data class Save(val name: String, val warning: String?) : WebDialog

    /** The toolbar's Load, or with [restore] the startup offer to restore one of [saves]. */
    data class Load(val restore: Boolean, val saves: List<SaveEntry>) : WebDialog

    /** A save no sequence of shifts solves: Repair & load or New game, and no Exit. */
    data class Unsolvable(val message: String) : WebDialog
}

/** A save as a Load row: its [label] is `name (NxN)`, with the unsolvable mark when it applies. */
data class SaveEntry(val name: String, val label: String)
