package view.tui

/** Default (unsolved) board title. */
const val TITLE_UNSOLVED = "Mordovorot"

/** Title shown once the board is solved. */
const val TITLE_SOLVED = "Congratulations ✓"

/**
 * Everything [ScreenRenderer] needs to draw one frame of the board screen: the title, the
 * board's current tile values (0-based, GH-10 dialect - [ScreenRenderer] applies the 1-based
 * display offset) and side, and whether the shift arrows are interactive. [forBoard] couples
 * `arrowsEnabled` to `solved` as the ticket's solved-state note intends (dimmed, non-clickable
 * arrows, live toolbar) - but [TuiView] currently forces `arrowsEnabled` back to `true` after
 * calling [forBoard], since WU3 has no Congratulations screen or live toolbar yet to hand the
 * player off to (round 2/3 audit findings on PR #22). The title still flips on solve; only the
 * arrow-disabling half of `isSolved()` is deferred.
 */
data class ScreenState(
    val title: String,
    val board: List<Int>,
    val squareSide: Int,
    val arrowsEnabled: Boolean,
) {
    companion object {
        /** Builds the board screen's [ScreenState] from the presenter's current query results. */
        fun forBoard(board: List<Int>, squareSide: Int, solved: Boolean): ScreenState =
            ScreenState(
                title = if (solved) TITLE_SOLVED else TITLE_UNSOLVED,
                board = board,
                squareSide = squareSide,
                arrowsEnabled = !solved,
            )
    }
}
