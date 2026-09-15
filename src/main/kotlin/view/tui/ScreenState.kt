package view.tui

import InputMode

/** Default (unsolved) board title. */
const val TITLE_UNSOLVED = "Mordovorot"

/** Title shown once [ScreenState.arrowsEnabled] goes false (the board is solved). */
const val TITLE_SOLVED = "Congratulations ✓"

/**
 * Everything [ScreenRenderer] needs to draw one frame of the board screen: the title, the
 * board's current tile values (0-based, GH-10 dialect - [ScreenRenderer] applies the 1-based
 * display offset) and side, and whether the shift arrows are interactive. `arrowsEnabled = false`
 * is how [viewmodel.ViewModel.isSolved] reaching the TUI shows up here - see the ticket's
 * solved-state note (dimmed, non-clickable arrows, live toolbar, `Congratulations ✓` title -
 * see [view.tui.TuiView]'s class KDoc for the full solved-state behavior).
 */
data class ScreenState(
    val title: String,
    val board: List<Int>,
    val squareSide: Int,
    val arrowsEnabled: Boolean,
    /** The modal overlay to draw on top of the board (WU4), or `null` for the plain board. */
    val dialog: Dialog? = null,
    /** A one-shot status line from the viewModel (e.g. "Saved as 'x'.", "Not quitting - ...") -
     *  board-only, since dialogs show their own [Dialog.message] instead (WU4). */
    val message: String? = null,
    /** The highlighted shift arrow in keyboard mode (GH-18) - `null` in mouse mode and whenever
     *  [arrowsEnabled] is false (the board is solved, so there's nothing for a cursor to land
     *  on). Set by [view.tui.KeyboardInput.decorateBoard], never by [forBoard] itself. */
    val cursor: ArrowCursor? = null,
    /** A bottom-left reminder of the keyboard controls (GH-18), or `null` in mouse mode. */
    val controlsHint: String? = null,
    /** Whether the toolbar shows `[Save F5]`-style labels (keyboard mode) instead of
     *  `[ Save ]` (mouse mode) - see [BoardLayout.toolbarButtons]'s KDoc for why this must be
     *  carried on [ScreenState] rather than decided independently by each construction site. */
    val toolbarShortcuts: Boolean = false,
    /** The other input modes offered on the toolbar (GH-30) - never the mode the view is
     *  already in. Empty in console mode (no toolbar at all); set by [TuiInput.decorateBoard]
     *  ([MouseInput]'s WU3, `KeyboardInput`'s WU4), never by [forBoard] itself, same as
     *  [cursor]/[controlsHint]. */
    val modeButtons: List<InputMode> = emptyList(),
) {
    companion object {
        /** Builds the board screen's [ScreenState] from the viewModel's current query results. */
        fun forBoard(board: List<Int>, squareSide: Int, solved: Boolean): ScreenState =
            ScreenState(
                title = if (solved) TITLE_SOLVED else TITLE_UNSOLVED,
                board = board,
                squareSide = squareSide,
                arrowsEnabled = !solved,
            )
    }
}
