package view.tui

import InputMode
import viewmodel.GiveUpButton
import viewmodel.SolutionState
import viewmodel.TileLabels

/** Default (unsolved) board title. */
const val TITLE_UNSOLVED = "Mordovorot"

/** Title shown once [ScreenState.arrowsEnabled] goes false (the board is solved). */
const val TITLE_SOLVED = "Congratulations ✓"

/**
 * Everything [ScreenRenderer] needs to draw one frame of the board screen: the title, the
 * board's current tile values (0-based, GH-10 dialect - [ScreenRenderer] draws them in
 * [tileLabels]) and side, and whether the shift arrows are interactive. `arrowsEnabled = false`
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
    /** Keyboard mode's selected tile (GH-170), drawn with its row and column marked - `null` in
     *  mouse mode and whenever [arrowsEnabled] is false (solved). Set by
     *  [view.tui.KeyboardInput.decorateBoard], never by [forBoard] itself. */
    val selection: TileSelection? = null,
    /** Whether the shift arrows are drawn - keyboard mode hides them (GH-101). Their space stays
     *  reserved, so the board doesn't move on a mode switch. */
    val showArrows: Boolean = true,
    /** Keyboard mode's selection hint lines, left of the board (GH-101). */
    val selectionHint: List<String> = emptyList(),
    /** Keyboard mode's shift hint lines, right of the board (GH-101). */
    val shiftHint: List<String> = emptyList(),
    /** A bottom-left reminder of the keyboard controls (GH-18), or `null` in mouse mode. */
    val controlsHint: String? = null,
    /** Whether the toolbar shows `[Save 2]`-style labels (keyboard mode) instead of
     *  `[ Save ]` (mouse mode) - see [BoardLayout.toolbarButtons]'s KDoc for why this must be
     *  carried on [ScreenState] rather than decided independently by each construction site. */
    val toolbarShortcuts: Boolean = false,
    /** The other input modes offered on the toolbar (GH-30) - never the mode the view is
     *  already in. Set by [TuiInput.decorateBoard]
     *  ([MouseInput]'s WU3, `KeyboardInput`'s WU4), never by [forBoard] itself, same as
     *  [selection]/[controlsHint]. */
    val modeButtons: List<InputMode> = emptyList(),
    /** Moves made this game (GH-62), drawn under the title. */
    val moveCount: Int = 0,
    /** What Mordie says under [moveCount] (GH-83), without the `Mordie:` prefix; `null` for none. */
    val mordieLine: String? = null,
    /** The Give Up row's button (GH-126). */
    val giveUpButton: GiveUpButton = GiveUpButton.HIDDEN,
    /** The solution screen (GH-126), `null` off it. */
    val solution: SolutionState? = null,
    /** The first step [solution]'s list shows, from [StepListWindow]. */
    val stepOffset: Int = 0,
    /** How the tiles are labelled (GH-139). */
    val tileLabels: TileLabels = TileLabels.DEFAULT,
) {
    companion object {
        /** Builds the board screen's [ScreenState] from the viewModel's current query results. */
        fun forBoard(
            board: List<Int>,
            squareSide: Int,
            solved: Boolean,
            moveCount: Int = 0,
            /** The title once solved - Mordie's ending (GH-83) when there is one. */
            solvedTitle: String = TITLE_SOLVED,
            mordieLine: String? = null,
            giveUpButton: GiveUpButton = GiveUpButton.HIDDEN,
            /** On the solution screen the board isn't playable: no arrows, no `Moves:` (GH-126). */
            solution: SolutionState? = null,
            tileLabels: TileLabels = TileLabels.DEFAULT,
        ): ScreenState =
            ScreenState(
                title = if (solved) solvedTitle else TITLE_UNSOLVED,
                board = board,
                squareSide = squareSide,
                arrowsEnabled = !solved && solution == null,
                showArrows = solution == null,
                moveCount = moveCount,
                mordieLine = mordieLine,
                giveUpButton = giveUpButton,
                solution = solution,
                tileLabels = tileLabels,
            )
    }

    /** The geometry this state is drawn and hit-tested against. */
    fun layout(terminalSize: TerminalSize): BoardLayout =
        BoardLayout(terminalSize, squareSide, arrowsEnabled, toolbarShortcuts, modeButtons, giveUpButton, solution)
}
