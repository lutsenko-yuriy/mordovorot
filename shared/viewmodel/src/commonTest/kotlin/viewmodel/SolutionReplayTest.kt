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
    fun `a new replay is ready on the original arrangement with no step highlighted`() {
        assertEquals(Playback.READY, replay.playback)
        assertNull(replay.step)
        assertContentEquals(original, replay.tiles)
        assertFalse(replay.shift())
        assertContentEquals(original, replay.tiles)
    }

    @Test
    fun `Play highlights the first step and each shift moves one tile along`() {
        replay.playPause()

        assertEquals(Playback.PLAYING, replay.playback)
        assertEquals(0, replay.step)
        assertFalse(replay.shift())
        assertContentEquals(afterRowShift, replay.tiles)
        assertTrue(replay.shift())
        assertContentEquals(afterRowStep, replay.tiles)
        assertEquals(0, replay.step, "the finished step stays highlighted until the next shift")
    }

    @Test
    fun `the last shift of the last step ends the replay on the solved board`() {
        replay.playPause()
        repeat(2) { replay.shift() }

        assertTrue(replay.shift())

        assertEquals(1, replay.step)
        assertEquals(Playback.ENDED, replay.playback)
        assertContentEquals(solved, replay.tiles)
        assertFalse(replay.shift())
        assertContentEquals(solved, replay.tiles)
    }

    @Test
    fun `Pause mid-step holds, and Play resumes where it stopped`() {
        replay.playPause()
        replay.shift()

        replay.playPause()

        assertEquals(Playback.PAUSED, replay.playback)
        assertTrue(replay.pausedThisRun)
        assertFalse(replay.shift())
        assertContentEquals(afterRowShift, replay.tiles)
        replay.playPause()
        assertEquals(Playback.PLAYING, replay.playback)
        assertTrue(replay.shift())
        assertContentEquals(afterRowStep, replay.tiles)
    }

    @Test
    fun `Play again after the end restarts from the original arrangement`() {
        replay.playPause()
        replay.playPause()
        replay.playPause()
        repeat(3) { replay.shift() }

        replay.playPause()

        assertEquals(Playback.PLAYING, replay.playback)
        assertEquals(0, replay.step)
        assertFalse(replay.pausedThisRun)
        assertContentEquals(original, replay.tiles)
    }

    @Test
    fun `move plays a whole step, or the rest of one paused mid-way`() {
        replay.playPause()

        assertEquals(rowStep, replay.move())
        assertContentEquals(afterRowStep, replay.tiles)
        assertEquals(colStep, replay.move())
        assertEquals(Playback.ENDED, replay.playback)
        assertNull(replay.move())

        replay.playPause()
        replay.shift()
        assertEquals(rowStep.copy(count = 1), replay.move())
        assertContentEquals(afterRowStep, replay.tiles)
    }

    @Test
    fun `move does nothing unless playing`() {
        assertNull(replay.move())
        replay.playPause()
        replay.playPause()
        assertNull(replay.move())
        assertContentEquals(original, replay.tiles)
    }

    @Test
    fun `tiles is a copy`() {
        replay.tiles[0] = 99

        assertContentEquals(original, replay.tiles)
    }
}
