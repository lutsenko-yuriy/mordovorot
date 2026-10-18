package web

import viewmodel.GiveUpButton
import viewmodel.SolutionState
import web.drag.LineOffset

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
    /** The line being dragged and how far, before the release snaps it. */
    val drag: LineOffset? = null,
    /** The row above the toolbar (GH-126). */
    val giveUp: GiveUpButton = GiveUpButton.HIDDEN,
    /** Mordie's solution screen (GH-126); `null` off it. */
    val solution: SolutionState? = null,
    /** The replay shift just made, for this frame to slide its line from. */
    val slide: LineOffset? = null,
) {
    val arrowsEnabled: Boolean get() = !solved && solution == null
    val movesShown: Boolean get() = solution == null
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

    /** The give-up confirmation (GH-126): Yes or No. */
    data object GiveUp : WebDialog
}

/** A save as a Load row: its [label] is `name (NxN)`, with the unsolvable mark when it applies. */
data class SaveEntry(val name: String, val label: String)
