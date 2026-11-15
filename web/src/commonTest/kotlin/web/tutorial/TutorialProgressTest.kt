package web.tutorial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import web.drag.Axis.COLUMN
import web.drag.Axis.ROW
import web.tutorial.Verdict.BLOCKED
import web.tutorial.Verdict.COUNTS
import web.tutorial.Verdict.DONE
import web.tutorial.Verdict.IGNORED

/** GH-288: the matching table of the plan's decision 2, row by row. Steps: row 0 left 1, column 1 up 1, row 2 right 2, column 3 down 1. */
class TutorialProgressTest {

    private val progress = TutorialProgress()

    private fun atStep(step: Int) = progress.apply {
        val steps = listOf(Triple(ROW, 0, -1), Triple(COLUMN, 1, -1), Triple(ROW, 2, 2))
        steps.take(step - 1).forEach { (axis, line, n) -> assertEquals(DONE, check(axis, line, n)) }
    }

    @Test
    fun `the asked shift ends a one-tile step`() {
        assertEquals(DONE, progress.check(ROW, 0, -1))
        assertEquals(2, progress.step)
    }

    @Test
    fun `another line, or the same index on the other axis, is blocked`() {
        assertEquals(BLOCKED, progress.check(ROW, 1, -1))
        assertEquals(BLOCKED, progress.check(COLUMN, 0, -1))
        assertEquals(1, progress.step)
    }

    @Test
    fun `the wrong direction is blocked`() {
        assertEquals(BLOCKED, progress.check(ROW, 0, 1))
    }

    @Test
    fun `more tiles than asked are blocked`() {
        assertEquals(BLOCKED, progress.check(ROW, 0, -2))
        assertEquals(1, progress.step)
    }

    @Test
    fun `a drag released at 0 tiles is neither a shift nor blocked`() {
        assertEquals(IGNORED, progress.check(ROW, 0, 0))
        assertEquals(0, progress.totalBlocked)
    }

    @Test
    fun `step 3 takes two tiles in one drag`() {
        atStep(3)
        assertEquals(DONE, progress.check(ROW, 2, 2))
        assertEquals(4, progress.step)
    }

    @Test
    fun `step 3 takes one tile and then another`() {
        atStep(3)

        assertEquals(COUNTS, progress.check(ROW, 2, 1))
        assertTrue(progress.begun)
        assertEquals(1, progress.tilesLeft)
        assertEquals(DONE, progress.check(ROW, 2, 1))
    }

    @Test
    fun `step 3 takes a half turn the other way, which fixes the direction`() {
        atStep(3)

        assertEquals(DONE, progress.check(ROW, 2, -2))
    }

    @Test
    fun `step 3's first tile fixes the direction, so the other way is then blocked`() {
        atStep(3)
        assertEquals(COUNTS, progress.check(ROW, 2, -1))

        assertEquals(BLOCKED, progress.check(ROW, 2, 1))
        assertEquals(DONE, progress.check(ROW, 2, -1))
    }

    @Test
    fun `step 3 blocks a third tile`() {
        atStep(3)
        assertEquals(COUNTS, progress.check(ROW, 2, 1))

        assertEquals(BLOCKED, progress.check(ROW, 2, 2))
    }

    @Test
    fun `step 4 ends the guided steps`() {
        atStep(4)

        assertEquals(DONE, progress.check(COLUMN, 3, 1))

        assertEquals(TutorialScript.SOLVED_STEP, progress.step)
        assertNull(progress.asked)
        assertEquals(IGNORED, progress.check(COLUMN, 3, 1))
    }

    @Test
    fun `a step does not take a half turn the other way when the other directions are asked`() {
        assertFalse(TutorialScript.shifts[0].halfTurn)
        assertEquals(BLOCKED, progress.check(ROW, 0, 1))
    }

    @Test
    fun `blocked shifts count for the step and the whole`() {
        progress.check(ROW, 1, 1)
        progress.check(ROW, 0, -2)
        assertEquals(2, progress.blockedInStep)

        progress.check(ROW, 0, -1)
        assertEquals(0, progress.blockedInStep)
        progress.check(ROW, 0, 1)
        assertEquals(1, progress.blockedInStep)
        assertEquals(3, progress.totalBlocked)
    }
}
