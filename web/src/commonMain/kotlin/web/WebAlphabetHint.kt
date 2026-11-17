package web

import viewmodel.AlphabetHint
import viewmodel.GameViewModel

/** The alphabet hint's flow (GH-332): `[ Hint ]` / 8 opens the board's letter scheme's whole alphabet. */
internal class WebAlphabetHint(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val modal: WebModal,
) {

    /** The button's state: hidden on the solution screen and in the tutorial, dimmed when the board shows no letters (a number scheme, or Numbers standing in on a large board). */
    fun button(): AlphabetHintButton = when {
        game.tutorial.running || viewModel.solution() != null -> AlphabetHintButton.HIDDEN
        hint() == null -> AlphabetHintButton.DIMMED
        else -> AlphabetHintButton.LIVE
    }

    /** `[ Hint 8 ]`, or 8 with [openedFrom] `shortcut`; a dimmed or hidden button does nothing. Focus goes back to [opener]. */
    fun open(openedFrom: String = "button", opener: Focus = Focus.AlphabetHintButton) {
        if (button() != AlphabetHintButton.LIVE) return
        val hint = hint() ?: return
        game.runFlow(opener) {
            viewModel.alphabetHintOpened(openedFrom)
            modal.ask<Unit>(WebDialog.AlphabetHint(hint))
            game.cancelled("alphabet_hint")
        }
    }

    /** The dialog's OK. */
    fun onClosed() = modal.answer<WebDialog.AlphabetHint, Unit>(Unit)

    private fun hint() = AlphabetHint.of(viewModel.tileLabels(), viewModel.squareSide())
}
