package viewmodel

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SolutionReplayTest {

    /** A 4x4 two steps from solved: row 0 right twice, then column 1 up once. */
    private val original = intArrayOf(2, 3, 0, 13, 4, 1, 6, 7, 8, 5, 10, 11, 12, 9, 14, 15)
    private val rowStep = SolutionStep(isRow = true, index = 0, forward = true, count = 2)
    private val colStep = SolutionStep(isRow = false, index = 1, forward = false, count = 1)
    private val afterRowShift = intArrayOf(13, 2, 3, 0, 4, 1, 6, 7, 8, 5, 10, 11, 12, 9, 14, 15)
    private val afterRowStep = intArrayOf(0, 13, 2, 3, 4, 1, 6, 7, 8, 5, 10, 11, 12, 9, 14, 15)
    private val solved = IntArray(16) { it }

    private val replay = SolutionReplay(original, side = 4, steps = listOf(rowStep, colStep))

    @Test
    fun `a new replay is at the start of the original arrangement with no step highlighted`() {
        assertTrue(replay.atStart)
        assertFalse(replay.atEnd)
        assertNull(replay.step)
        assertContentEquals(original, replay.tiles)
    }

    @Test
    fun `Next applies one shift, highlights its step, and returns it as a one-shift step`() {
        assertEquals(rowStep.copy(count = 1), replay.next())

        assertContentEquals(afterRowShift, replay.tiles)
        assertEquals(0, replay.step)
        assertFalse(replay.atStart)

        replay.next()
        assertContentEquals(afterRowStep, replay.tiles)
        assertEquals(0, replay.step)

        assertEquals(colStep.copy(count = 1), replay.next())
        assertEquals(1, replay.step)
    }

    @Test
    fun `the last Next reaches the solved board, and Next does nothing there`() {
        repeat(3) { replay.next() }

        assertTrue(replay.atEnd)
        assertContentEquals(solved, replay.tiles)
        assertNull(replay.next())
        assertContentEquals(solved, replay.tiles)
    }

    @Test
    fun `Prev undoes the last shift and returns the shift it made, back to no highlight at the start`() {
        repeat(3) { replay.next() }

        assertEquals(colStep.copy(forward = true, count = 1), replay.previous())
        assertContentEquals(afterRowStep, replay.tiles)
        assertEquals(0, replay.step)

        assertEquals(rowStep.copy(forward = false, count = 1), replay.previous())
        assertContentEquals(afterRowShift, replay.tiles)
        replay.previous()

        assertTrue(replay.atStart)
        assertNull(replay.step)
        assertContentEquals(original, replay.tiles)
        assertNull(replay.previous())
    }

    @Test
    fun `Restart goes back to the original arrangement`() {
        repeat(2) { replay.next() }

        replay.restart()

        assertTrue(replay.atStart)
        assertNull(replay.step)
        assertContentEquals(original, replay.tiles)
    }

    @Test
    fun `tiles is a copy`() {
        replay.tiles[0] = 99

        assertContentEquals(original, replay.tiles)
    }
}
