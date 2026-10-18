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
}
