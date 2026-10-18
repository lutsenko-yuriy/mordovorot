package viewmodel

import mordie.SolutionMove

/** One step of Mordie's solution as the solution screen shows and replays it (GH-126): [count]
 *  shifts of row or column [index] (0-based), right/down when [forward], otherwise left/up. */
data class SolutionStep(val isRow: Boolean, val index: Int, val forward: Boolean, val count: Int)

/** Turns Mordie's moves into [SolutionStep]s and the screen's step list (GH-126). */
internal object SolutionSteps {

    /** Each move the shortest way round its line; a tie goes right or down. */
    fun of(moves: List<SolutionMove>, side: Int): List<SolutionStep> = moves.map {
        val back = side - it.amount
        if (it.amount <= back) SolutionStep(it.isRow, it.index, forward = true, it.amount)
        else SolutionStep(it.isRow, it.index, forward = false, back)
    }

    /** `03. Col 3 ↑ ×1`: numbers zero-padded to the total's width, lines counted from 1. */
    fun format(steps: List<SolutionStep>): List<String> {
        val width = steps.size.toString().length
        return steps.mapIndexed { i, step ->
            val line = if (step.isRow) "Row" else "Col"
            "${(i + 1).toString().padStart(width, '0')}. $line ${step.index + 1} ${arrow(step)} ×${step.count}"
        }
    }

    private fun arrow(step: SolutionStep) = when {
        step.isRow -> if (step.forward) "→" else "←"
        else -> if (step.forward) "↓" else "↑"
    }
}
