package view.tui

/** Default (unsolved) board title. */
const val TITLE_UNSOLVED = "Mordovorot"

/** Title shown once [ScreenState.arrowsEnabled] goes false (the board is solved). */
const val TITLE_SOLVED = "Congratulations ✓"

/**
 * Everything [ScreenRenderer] needs to draw one frame of the board screen: the title, the
 * board's current tile values (0-based, GH-10 dialect - [ScreenRenderer] applies the 1-based
 * display offset) and side, and whether the shift arrows are interactive. `arrowsEnabled = false`
 * is how [presenter.Presenter.isSolved] reaching the TUI shows up here - see the ticket's
 * solved-state note (dimmed, non-clickable arrows, live toolbar). Confirmed product decision:
 * WU3 has no live toolbar or Congratulations screen to hand the player off to yet, so once
 * solved, Ctrl+C is the only way out until WU4/5 land - this is accepted, not a dead-end bug.
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
