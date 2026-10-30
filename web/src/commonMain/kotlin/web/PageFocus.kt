package web

import viewmodel.GiveUpButton

private const val ARROW_PREFIX = "arrow-"

/** Where focus goes when the control [key] named is gone or disabled in [state] (GH-164): the
 *  next thing that control's screen offers, else the start of the page. */
fun fallbackFocus(key: String, state: WebState): Focus = when {
    // Keyboard mode just ended, so the board is no longer a Tab stop (GH-171).
    key == Focus.Board.key && state.mode == PlayMode.MOUSE -> Focus.ControlsButton
    // The shift solved the board (Read board too, as a drag can solve it; a key shift, GH-171): what a won board offers, in Tab order.
    (key.startsWith(ARROW_PREFIX) || key == Focus.ReadBoard.key || key == Focus.Board.key) && state.solved ->
        if (state.giveUp == GiveUpButton.MORDIES_SOLUTION) Focus.GiveUpRow else Focus.NewButton
    // The button for the other direction, as the replay can only go one way at its ends.
    key == Focus.ReplayNext.key -> Focus.ReplayPrev
    key == Focus.ReplayPrev.key || key == Focus.ReplayRestart.key -> Focus.ReplayNext
    key == Focus.GiveUpRow.key && state.solution != null -> Focus.ReplayNext
    // A new game's row is dimmed or hidden: on to the board, where the keys work (GH-171).
    key == Focus.GiveUpRow.key && state.selection != null -> Focus.Board
    else -> Focus.PageStart
}
