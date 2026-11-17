package web

import org.w3c.dom.Element
import texts.texts
import viewmodel.GiveUpButton
import viewmodel.SolutionState
import web.a11y.asList
import web.a11y.asListItem
import web.a11y.focusKey

/** The solution screen's numbered steps, the current one marked (GH-126). */
internal fun stepList(solution: SolutionState): Element = element("div", "steps").apply {
    // A list, without the numbers an <ol> would add to the steps' own.
    asList(this)
    solution.steps.forEachIndexed { i, step ->
        append(text(if (i == solution.current) "step current" else "step", step).apply {
            asListItem(this, current = i == solution.current)
        })
    }
}

/** Prev / Next / Restart, each disabled where it has nowhere to go (GH-126). [game] is read only on a
 *  click: the first frame is drawn before the renderer has its game. */
internal fun replayButtons(solution: SolutionState, game: () -> WebGame, dialogOpen: Boolean): Element = element("div", "replay").apply {
    append(
        button(texts().page.replayPrev) { game().solution.onReplayPrevious() }.focusKey(Focus.ReplayPrev).also { it.disabled = dialogOpen || solution.atStart },
        button(texts().page.replayNext) { game().solution.onReplayNext() }.focusKey(Focus.ReplayNext).also { it.disabled = dialogOpen || solution.atEnd },
        button(texts().page.replayRestart) { game().solution.onReplayRestart() }.focusKey(Focus.ReplayRestart).also { it.disabled = dialogOpen || solution.atStart },
    )
}

/** `Give Up`, disabled until Mordie's solution is revealed, or `Mordie's solution` after a win, then `Hint` (GH-332), dimmed for
 *  a scheme with no letters, and `Tutorial` (GH-289) when [WebState.tutorialButton] says so, never dimmed; `null` when the row
 *  is empty (GH-126). [game] is read only on a click, as in [replayButtons]. */
internal fun giveUpRow(state: WebState, game: () -> WebGame, dialogOpen: Boolean): Element? {
    val giveUp = state.giveUp
    val first = when (giveUp) {
        GiveUpButton.HIDDEN -> null
        GiveUpButton.MORDIES_SOLUTION -> button(texts().page.mordiesSolution) { game().solution.onMordiesSolution() }
        GiveUpButton.GIVE_UP, GiveUpButton.DIMMED -> button(texts().page.giveUp) { game().solution.onGiveUp() }
    }?.also {
        it.disabled = dialogOpen || giveUp == GiveUpButton.DIMMED
        it.focusKey(Focus.GiveUpRow).withDigit(state.digitOf(ToolbarButton.GIVE_UP_ROW))
    }
    val hint = if (state.alphabetHint != AlphabetHintButton.HIDDEN) button(texts().page.alphabetHint) { game().alphabetHint.open() }.also {
        it.disabled = dialogOpen || state.alphabetHint == AlphabetHintButton.DIMMED
        it.focusKey(Focus.AlphabetHintButton).withDigit(state.digitOf(ToolbarButton.ALPHABET_HINT))
    } else null
    val replay = if (state.tutorialButton) button(texts().page.tutorial) { game().tutorial.onReplay("button") }.also {
        it.disabled = dialogOpen
        it.focusKey(Focus.TutorialReplay).withDigit(state.digitOf(ToolbarButton.TUTORIAL))
    } else null
    val buttons = listOfNotNull(first, hint, replay)
    if (buttons.isEmpty()) return null
    return element("div", "give-up").apply { append(*buttons.toTypedArray()) }
}
