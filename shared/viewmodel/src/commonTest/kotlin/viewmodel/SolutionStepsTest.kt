package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import mordie.SolutionMove

class SolutionStepsTest {

    @Test
    fun `each move goes the shortest way round, ties going right or down`() {
        val moves = listOf(
            SolutionMove(isRow = true, index = 0, amount = 1),
            SolutionMove(isRow = true, index = 1, amount = 3),
            SolutionMove(isRow = false, index = 2, amount = 2),
            SolutionMove(isRow = false, index = 3, amount = 3),
        )

        assertEquals(
            listOf(
                SolutionStep(isRow = true, index = 0, forward = true, count = 1),
                SolutionStep(isRow = true, index = 1, forward = false, count = 1),
                SolutionStep(isRow = false, index = 2, forward = true, count = 2),
                SolutionStep(isRow = false, index = 3, forward = false, count = 1),
            ),
            SolutionSteps.of(moves, side = 4),
        )
    }

    @Test
    fun `a step is numbered from 1 and names its line from 1, with an arrow and a count`() {
        val steps = listOf(
            SolutionStep(isRow = true, index = 0, forward = true, count = 1),
            SolutionStep(isRow = true, index = 1, forward = false, count = 2),
            SolutionStep(isRow = false, index = 2, forward = true, count = 1),
            SolutionStep(isRow = false, index = 4, forward = false, count = 1),
        )

        assertEquals(
            listOf("1. Row 1 → ×1", "2. Row 2 ← ×2", "3. Col 3 ↓ ×1", "4. Col 5 ↑ ×1"),
            SolutionSteps.format(steps),
        )
    }

    @Test
    fun `step numbers are zero-padded to the total's width, so every line is as wide`() {
        val step = SolutionStep(isRow = false, index = 2, forward = false, count = 1)

        assertEquals("7. Col 3 ↑ ×1", SolutionSteps.format(List(7) { step }).last())
        assertEquals("03. Col 3 ↑ ×1", SolutionSteps.format(List(10) { step })[2])
        val long = SolutionSteps.format(List(147) { step })
        assertEquals("001. Col 3 ↑ ×1", long.first())
        assertEquals("147. Col 3 ↑ ×1", long.last())
        assertEquals(1, long.map { it.length }.toSet().size)
    }
}
