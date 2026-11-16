package web

import viewmodel.GiveUpButton

private const val ARROW_PREFIX = "arrow-"

/** The Give Up row's buttons (GH-289). */
private val ROW_BUTTONS = setOf(Focus.GiveUpRow.key, Focus.TutorialReplay.key)

/** Where focus goes when the control [key] named is gone or disabled in [state] (GH-164): the
 *  next thing that control's screen offers, else the start of the page. */
fun fallbackFocus(key: String, state: WebState): Focus {
    val winning = key.startsWith(ARROW_PREFIX) || key == Focus.ReadBoard.key || key == Focus.Board.key
    val panelButton = key == Focus.TutorialControls.key || key == Focus.TutorialLabels.key || key == Focus.TutorialSkip.key
    return when {
        // Step 5 (GH-288): not [ Skip ], so a held or repeated Enter can't skip.
        winning && state.solved && state.guidance != null -> Focus.TutorialLabels
        // The shift solved the board (Read board too, as a drag can solve it; a key shift, GH-171), or the tutorial's panel is gone from one: what a won board offers, in Tab order.
        (winning || panelButton) && state.solved ->
            if (state.giveUp == GiveUpButton.MORDIES_SOLUTION) Focus.GiveUpRow else Focus.NewButton
        // The tutorial's panel is gone from a game still in play: on to the board, where the keys work.
        panelButton && state.selection != null -> Focus.Board
        // The button for the other direction, as the replay can only go one way at its ends.
        key == Focus.ReplayNext.key -> Focus.ReplayPrev
        key == Focus.ReplayPrev.key || key == Focus.ReplayRestart.key -> Focus.ReplayNext
        key in ROW_BUTTONS && state.solution != null -> Focus.ReplayNext
        // A new game's row is dimmed or hidden, or the tutorial's replay has begun: on to the board, where the keys work (GH-171, GH-289).
        key in ROW_BUTTONS && state.selection != null -> Focus.Board
        // A redraw under an open dialog (GH-289): a control that is gone or disabled passes focus to the dialog's own default, never to the page behind it.
        state.dialog != null -> defaultFocus(state.dialog)
        else -> Focus.PageStart
    }
}
