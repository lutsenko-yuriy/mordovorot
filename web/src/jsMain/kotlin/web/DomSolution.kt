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

/** Prev / Next / Restart, each disabled where it has nowhere to go (GH-126). */
internal fun replayButtons(solution: SolutionState, game: WebGame, dialogOpen: Boolean): Element = element("div", "replay").apply {
    append(
        button("◀ Prev") { game.onReplayPrevious() }.also { it.disabled = dialogOpen || solution.atStart },
        button("Next ▶") { game.onReplayNext() }.also { it.disabled = dialogOpen || solution.atEnd },
        button("↻ Restart") { game.onReplayRestart() }.also { it.disabled = dialogOpen || solution.atStart },
    )
}

/** `Give Up`, disabled until Mordie's solution is revealed, or `Mordie's solution` after a win;
 *  `null` when the row is empty (GH-126). */
internal fun giveUpRow(giveUp: GiveUpButton, game: WebGame, dialogOpen: Boolean): Element? {
    val button = when (giveUp) {
        GiveUpButton.HIDDEN -> return null
        GiveUpButton.MORDIES_SOLUTION -> button("Mordie's solution") { game.onMordiesSolution() }
        GiveUpButton.GIVE_UP, GiveUpButton.DIMMED -> button("Give Up") { game.onGiveUp() }
    }
    button.disabled = dialogOpen || giveUp == GiveUpButton.DIMMED
    return element("div", "give-up").apply { append(button) }
}
