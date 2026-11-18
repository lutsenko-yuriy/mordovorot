package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.TileLabelGroup
import viewmodel.TileLabels

/** GH-342: which label waves and group slides a [LabelMotion] keeps running, starts and drops, on a clock the test sets. */
class LabelMotionTest {

    private var now = 0.0
    private val motion = LabelMotion { now }

    private fun frame(selected: TileLabels, open: TileLabelGroup = TileLabelGroup.of(selected), dialog: Boolean = true, tiles: List<Int> = listOf(1, 2, 3, 4)) =
        WebState(
            side = 2, tiles = tiles, moves = 0, mordie = null, solved = false, status = null,
            dialog = if (dialog) WebDialog.Labels(emptyList(), selected, open = open) else null,
            labels = selected,
        )

    private val numbers = frame(TileLabels.NUMBERS)
    private val letters = frame(TileLabels.LETTERS)

    /** [letters] drawn after [numbers] at time 0: a wave under way. */
    private fun startedLetters() {
        motion.start(null, numbers, still = false)
        motion.start(numbers, letters, still = false)
    }

    @Test
    fun aPickedRowStartsAWaveFromTheOldTexts() {
        startedLetters()

        val first = assertNotNull(motion.tile(0))
        assertEquals(numbers.tileTexts[0], first.old)
        assertEquals(0.0, first.leaveMs, 1e-9)
        assertEquals(LabelWave.board(4).stepMs, first.arriveMs, 1e-9)
        assertEquals(LabelWave.board(4).leavesAt(3), assertNotNull(motion.tile(3)).leaveMs, 1e-9)
    }

    @Test
    fun theDelaysCountDownAsTheWaveRuns() {
        startedLetters()
        now = 100.0

        assertEquals(-100.0, assertNotNull(motion.tile(0)).leaveMs, 1e-9)
        assertEquals(LabelWave.board(4).arrivesAt(2) - 100.0, assertNotNull(motion.tile(2)).arriveMs, 1e-9)
    }

    @Test
    fun aRebuiltFrameKeepsTheWaveWhileItRuns() {
        startedLetters()
        val id = assertNotNull(motion.tile(0)).id
        now = 300.0

        motion.start(letters, letters, still = false)

        assertEquals(id, assertNotNull(motion.tile(0)).id)
    }

    @Test
    fun aWaveIsDroppedOnceItIsOver() {
        startedLetters()
        now = LabelWave.board(4).endMs

        motion.start(letters, letters, still = false)

        assertNull(motion.tile(0))
    }

    @Test
    fun aWaveIsDroppedWhenTheTilesMove() {
        startedLetters()
        now = 100.0

        motion.start(letters, frame(TileLabels.LETTERS, dialog = false, tiles = listOf(2, 1, 3, 4)), still = false)

        assertNull(motion.tile(0))
    }

    @Test
    fun aWaveIsDroppedWhenTheLabelsChangeOutsideTheDialog() {
        startedLetters()
        now = 100.0
        val closed = frame(TileLabels.LETTERS, dialog = false)
        motion.start(letters, closed, still = false)
        assertNotNull(motion.tile(0))

        motion.start(closed, frame(TileLabels.PAIRS, dialog = false), still = false)

        assertNull(motion.tile(0))
    }

    @Test
    fun aNewPickDuringAWaveStartsFromTheRunningWavesTarget() {
        startedLetters()
        now = 100.0

        motion.start(letters, frame(TileLabels.PAIRS), still = false)

        val first = assertNotNull(motion.tile(0))
        assertEquals(letters.tileTexts[0], first.old)
        assertEquals(0.0, first.leaveMs, 1e-9)
    }

    @Test
    fun cancelWavesBackFromTheMarkedSchemeToTheOldOne() {
        motion.start(null, numbers, still = false)
        motion.start(numbers, letters, still = false)
        now = 700.0

        motion.start(letters, frame(TileLabels.NUMBERS, dialog = false), still = false)

        assertEquals(letters.tileTexts[0], assertNotNull(motion.tile(0)).old)
    }

    @Test
    fun okStartsNoWave() {
        motion.start(null, numbers, still = false)
        motion.start(numbers, letters, still = false)
        now = 700.0

        motion.start(letters, frame(TileLabels.LETTERS, dialog = false), still = false)

        assertNull(motion.tile(0))
    }

    @Test
    fun reducedMotionStartsAndKeepsNothing() {
        startedLetters()

        motion.start(numbers, letters, still = true)

        assertNull(motion.tile(0))
        assertNull(motion.swatch(0))
        assertNull(motion.leavingGroup())
    }

    @Test
    fun thePreviewWavesWithAFixedGapAndEndsBeforeTheBoard() {
        startedLetters()

        assertEquals(numbers.dialogPreview()[0], assertNotNull(motion.swatch(0)).old)
        assertEquals(0.0, assertNotNull(motion.swatch(0)).leaveMs, 1e-9)
        assertEquals(LabelWave.PREVIEW_STEP_MS, assertNotNull(motion.swatch(1)).leaveMs, 1e-9)

        now = 300.0
        motion.start(letters, letters, still = false)

        assertNull(motion.swatch(0))
        assertNotNull(motion.tile(0))
    }

    @Test
    fun aGroupPressStartsASlideFromTheOpenGroupAndItEnds() {
        motion.start(null, numbers, still = false)
        now = 10.0
        motion.start(numbers, frame(TileLabels.NUMBERS, open = TileLabelGroup.LATIN), still = false)

        assertEquals(TileLabelGroup.NUMBERS, motion.leavingGroup())
        now = 110.0
        assertEquals(-100.0, motion.slideDelayMs(), 1e-9)
        assertEquals(150.0, assertNotNull(motion.slideRemainingMs()), 1e-9)

        now = 10.0 + LabelWave.GROUP_MS
        val pressed = frame(TileLabels.NUMBERS, open = TileLabelGroup.LATIN)
        motion.start(pressed, pressed, still = false)

        assertNull(motion.leavingGroup())
        assertNull(motion.slideRemainingMs())
    }

    @Test
    fun aGroupPressKeepsARunningBoardWave() {
        startedLetters()
        now = 100.0

        motion.start(letters, frame(TileLabels.LETTERS, open = TileLabelGroup.CYRILLIC), still = false)

        assertEquals(TileLabelGroup.LATIN, motion.leavingGroup())
        assertNotNull(motion.tile(0))
    }

    @Test
    fun closingTheDialogDropsThePreviewAndTheSlide() {
        motion.start(null, numbers, still = false)
        motion.start(numbers, frame(TileLabels.NUMBERS, open = TileLabelGroup.LATIN), still = false)
        motion.start(frame(TileLabels.NUMBERS, open = TileLabelGroup.LATIN), frame(TileLabels.NUMBERS, dialog = false), still = false)

        assertNull(motion.leavingGroup())
        assertNull(motion.swatch(0))
    }

    @Test
    fun aNinebyNineBoardWavesEveryTileWithinTheTotal() {
        val side = 9
        val tiles = (1..side * side).toList()
        val before = WebState(side = side, tiles = tiles, moves = 0, mordie = null, solved = false, status = null, dialog = WebDialog.Labels(emptyList(), TileLabels.NUMBERS), labels = TileLabels.NUMBERS)
        val after = before.copy(dialog = WebDialog.Labels(emptyList(), TileLabels.PAIRS), labels = TileLabels.PAIRS)
        motion.start(null, before, still = false)
        motion.start(before, after, still = false)

        val last = assertNotNull(motion.tile(side * side - 1))

        assertTrue(last.arriveMs + LabelWave.MOTION_MS <= LabelWave.TOTAL_MS + 1e-6)
        assertEquals(LabelWave.TOTAL_MS, last.arriveMs + LabelWave.MOTION_MS, 1e-6)
    }

    private fun WebState.dialogPreview() = (dialog as WebDialog.Labels).preview
}
