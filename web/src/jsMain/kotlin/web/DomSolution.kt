package web

import org.w3c.dom.Element
import viewmodel.GiveUpButton
import viewmodel.SolutionState

/** The solution screen's numbered steps, the current one marked (GH-126). */
internal fun stepList(solution: SolutionState): Element = element("div", "steps").apply {
    // A list, without the numbers an <ol> would add to the steps' own.
    setAttribute("role", "list")
    solution.steps.forEachIndexed { i, step ->
        append(text(if (i == solution.current) "step current" else "step", step).apply {
            setAttribute("role", "listitem")
            if (i == solution.current) setAttribute("aria-current", "step")
        })
    }
}

/** Prev / Next / Restart, each disabled where it has nowhere to go (GH-126). [game] is read only on a
 *  click: the first frame is drawn before the renderer has its game. */
internal fun replayButtons(solution: SolutionState, game: () -> WebGame, dialogOpen: Boolean): Element = element("div", "replay").apply {
    append(
        button("◀ Prev") { game().solution.onReplayPrevious() }.focusKey(Focus.ReplayPrev).also { it.disabled = dialogOpen || solution.atStart },
        button("Next ▶") { game().solution.onReplayNext() }.focusKey(Focus.ReplayNext).also { it.disabled = dialogOpen || solution.atEnd },
        button("↻ Restart") { game().solution.onReplayRestart() }.focusKey(Focus.ReplayRestart).also { it.disabled = dialogOpen || solution.atStart },
    )
}

/** `Give Up`, disabled until Mordie's solution is revealed, or `Mordie's solution` after a win;
 *  `null` when the row is empty (GH-126). [game] is read only on a click, as in [replayButtons]. */
internal fun giveUpRow(giveUp: GiveUpButton, game: () -> WebGame, dialogOpen: Boolean): Element? {
    val button = when (giveUp) {
        GiveUpButton.HIDDEN -> return null
        GiveUpButton.MORDIES_SOLUTION -> button("Mordie's solution") { game().solution.onMordiesSolution() }
        GiveUpButton.GIVE_UP, GiveUpButton.DIMMED -> button("Give Up") { game().solution.onGiveUp() }
    }
    button.disabled = dialogOpen || giveUp == GiveUpButton.DIMMED
    button.focusKey(Focus.GiveUpRow)
    return element("div", "give-up").apply { append(button) }
}
