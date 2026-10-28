package web

import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.SolutionStep
import web.drag.Axis
import web.drag.DragShift

class AnnouncementsTest {

    @Test
    fun `a line is named from 1, with its direction`() {
        assertEquals("Row 2 left", Announcements.line(Arrow.LEFT, 1))
        assertEquals("Row 1 right", Announcements.line(Arrow.RIGHT, 0))
        assertEquals("Column 4 up", Announcements.line(Arrow.UP, 3))
        assertEquals("Column 3 down", Announcements.line(Arrow.DOWN, 2))
    }

    @Test
    fun `an arrow shift adds the move count only when it starts a move`() {
        assertEquals("Row 2 left", Announcements.shift(Arrow.LEFT, 1, newMove = null))
        assertEquals("Row 2 left, 1 move", Announcements.shift(Arrow.LEFT, 1, newMove = 1))
        assertEquals("Column 4 up, 5 moves", Announcements.shift(Arrow.UP, 3, newMove = 5))
    }

    @Test
    fun `a drag says how far, either way`() {
        assertEquals("Row 1 left 2", Announcements.drag(DragShift(Axis.ROW, 0, -2), newMove = null))
        assertEquals("Row 3 right 1, 2 moves", Announcements.drag(DragShift(Axis.ROW, 2, 1), newMove = 2))
        assertEquals("Column 2 up 3", Announcements.drag(DragShift(Axis.COLUMN, 1, -3), newMove = null))
        assertEquals("Column 1 down 2", Announcements.drag(DragShift(Axis.COLUMN, 0, 2), newMove = null))
    }

    @Test
    fun `the win, a new game and the solution screen`() {
        assertEquals("Solved in 1 move", Announcements.solved(1))
        assertEquals("Solved in 23 moves", Announcements.solved(23))
        assertEquals("New 4x4 game", Announcements.newGame(4))
        assertEquals("Solution, 12 moves", Announcements.solution(12))
        assertEquals("Solution, 1 move", Announcements.solution(1))
    }

    @Test
    fun `a load says the save's name, size and moves, or that it's solved`() {
        assertEquals("Loaded 'foo', 5x5, 12 moves", Announcements.loaded("foo", 5, 12, solved = false))
        assertEquals("Loaded 'foo', 3x3, 1 move", Announcements.loaded("foo", 3, 1, solved = false))
        assertEquals("Loaded 'done', 4x4, solved in 14 moves", Announcements.loaded("done", 4, 14, solved = true))
        assertEquals("Loaded 'sol', 4x4, solution, 12 moves", Announcements.loaded("sol", 4, 0, solved = false, solutionSteps = 12))
    }

    @Test
    fun `a solution step says its number and its shift in words, or the start`() {
        val steps = listOf(
            SolutionStep(isRow = true, index = 0, forward = false, count = 1),
            SolutionStep(isRow = false, index = 2, forward = false, count = 2),
            SolutionStep(isRow = true, index = 1, forward = true, count = 1),
            SolutionStep(isRow = false, index = 0, forward = true, count = 3),
        )

        assertEquals("Start, step 0 of 4", Announcements.step(steps, current = null))
        assertEquals("Step 1 of 4: Row 1 left 1", Announcements.step(steps, current = 0))
        assertEquals("Step 2 of 4: Column 3 up 2", Announcements.step(steps, current = 1))
        assertEquals("Step 3 of 4: Row 2 right 1", Announcements.step(steps, current = 2))
        assertEquals("Step 4 of 4: Column 1 down 3", Announcements.step(steps, current = 3))
    }

    @Test
    fun `a board read says each row from 1, then how many tiles are in place`() {
        val texts = listOf("5", "2", "9", "1", "4", "6", "7", "8", "3")
        val home = listOf(false, true, false, false, false, true, true, true, false)
        assertEquals("Row 1: 5, 2, 9. Row 2: 1, 4, 6. Row 3: 7, 8, 3. 4 tiles in place.", Announcements.board(texts, home, 3))
    }

    @Test
    fun `a board read counts 1 tile and 0 tiles, and says letters phonetically`() {
        val texts = listOf("B", "A", "D", "C")
        assertEquals("Row 1: bravo, alpha. Row 2: delta, charlie. 0 tiles in place.", Announcements.board(texts, List(4) { false }, 2))
        assertEquals("Row 1: bravo, alpha. Row 2: delta, charlie. 1 tile in place.", Announcements.board(texts, listOf(false, false, false, true), 2))
    }

    @Test
    fun `a label is spoken letter by letter in the phonetic alphabet, numbers as they are`() {
        assertEquals("alpha", Announcements.spoken("A"))
        assertEquals("bravo charlie", Announcements.spoken("BC"))
        assertEquals("juliet yankee", Announcements.spoken("JY"))
        assertEquals("x-ray zulu", Announcements.spoken("XZ"))
        assertEquals("12", Announcements.spoken("12"))
        assertEquals("0", Announcements.spoken("0"))
    }
}
