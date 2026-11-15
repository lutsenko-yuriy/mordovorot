package web.tutorial

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import web.Arrow
import web.drag.Axis

/** GH-288: the tutorial board and the four shifts that solve it. */
class TutorialScriptTest {

    private fun play(shifts: List<AskedShift>): BoardImpl {
        val board = BoardImpl(TutorialScript.SIDE).apply { restoreState(TutorialScript.board) }
        for (shift in shifts) repeat(shift.tiles) {
            when (shift.arrow) {
                Arrow.LEFT -> board.shiftLeft(shift.line)
                Arrow.RIGHT -> board.shiftRight(shift.line)
                Arrow.UP -> board.shiftUp(shift.line)
                Arrow.DOWN -> board.shiftDown(shift.line)
            }
        }
        return board
    }

    @Test
    fun `the four shifts solve the board, and three do not`() {
        assertTrue(play(TutorialScript.shifts).isCorrect())
        assertFalse(play(TutorialScript.shifts.dropLast(1)).isCorrect())
    }

    @Test
    fun `each step asks another line than the step before, so Moves reads 1, 2, 3, 4`() {
        TutorialScript.shifts.zipWithNext().forEach { (a, b) -> assertNotEquals(a.axis to a.line, b.axis to b.line) }
    }

    @Test
    fun `two row steps and two column steps`() {
        assertEquals(listOf(Axis.ROW, Axis.COLUMN, Axis.ROW, Axis.COLUMN), TutorialScript.shifts.map { it.axis })
    }

    @Test
    fun `only step 3 is a half turn`() {
        assertEquals(listOf(false, false, true, false), TutorialScript.shifts.map { it.halfTurn })
    }

    @Test
    fun `step 2 moves the wrapping tile from the top of its column to the bottom`() {
        val board = play(TutorialScript.shifts.take(1)).boardArray
        val ask = TutorialScript.shifts[1]
        assertEquals(TutorialScript.WRAPPING_TILE, board[ask.line])

        val after = play(TutorialScript.shifts.take(2)).boardArray
        assertEquals(TutorialScript.WRAPPING_TILE, after[(TutorialScript.SIDE - 1) * TutorialScript.SIDE + ask.line])
    }
}
