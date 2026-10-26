package web

import viewmodel.GiveUpButton

private const val ARROW_PREFIX = "arrow-"

/** Where focus goes when the control [key] named is gone or disabled in [state] (GH-164): the
 *  next thing that control's screen offers, else the start of the page. */
fun fallbackFocus(key: String, state: WebState): Focus = when {
    // The shift solved the board: what a won board offers, in Tab order.
    key.startsWith(ARROW_PREFIX) && state.solved ->
        if (state.giveUp == GiveUpButton.MORDIES_SOLUTION) Focus.GiveUpRow else Focus.NewButton
    // The button for the other direction, as the replay can only go one way at its ends.
    key == Focus.ReplayNext.key -> Focus.ReplayPrev
    key == Focus.ReplayPrev.key || key == Focus.ReplayRestart.key -> Focus.ReplayNext
    key == Focus.GiveUpRow.key && state.solution != null -> Focus.ReplayNext
    else -> Focus.PageStart
}
