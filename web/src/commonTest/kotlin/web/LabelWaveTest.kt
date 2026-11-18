package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** GH-342: the label wave's timing. */
class LabelWaveTest {

    @Test
    fun theBoardWaveEndsAtTheSameTimeOnEverySize() {
        for (side in 3..9) assertEquals(LabelWave.TOTAL_MS, LabelWave.board(side * side).endMs, 1e-9, "side $side")
    }

    @Test
    fun theGapBetweenTilesShrinksAsTheBoardGrows() {
        val steps = (3..9).map { LabelWave.board(it * it).stepMs }

        assertEquals(steps.sortedDescending(), steps)
        assertEquals(50.0, steps.first(), 1e-9)
    }

    @Test
    fun aTilesNewLabelArrivesAsTheNextOldOneLeaves() {
        val wave = LabelWave.board(16)

        for (i in 0 until 15) assertEquals(wave.arrivesAt(i), wave.leavesAt(i + 1), 1e-9, "tile $i")
    }

    @Test
    fun thePreviewUsesAFixedGapAndEndsBeforeTheBoard() {
        val preview = LabelWave.preview(3)

        assertEquals(40.0, preview.stepMs, 0.0)
        assertEquals(270.0, preview.endMs, 1e-9)
        for (side in 3..9) assertTrue(preview.endMs < LabelWave.board(side * side).endMs, "side $side")
    }

    @Test
    fun aWaveIsOverOnlyAtItsEnd() {
        val wave = LabelWave.board(16)

        assertFalse(wave.over(599.0))
        assertTrue(wave.over(600.0))
    }

    @Test
    fun theGroupSlideIsOverAfter250Ms() {
        assertFalse(LabelWave.slideOver(249.0))
        assertTrue(LabelWave.slideOver(250.0))
    }

    private fun state(labels: viewmodel.TileLabels, side: Int = 2, opening: Boolean = false, drag: web.drag.LineOffset? = null) =
        WebState(
            side = side, tiles = List(side * side) { it + 1 }, moves = 0, mordie = null, solved = false, status = null, dialog = null,
            labels = labels, opening = opening, drag = drag,
        )

    @Test
    fun aChangedSchemeWavesFromTheFrameBeforeWhateverChangedIt() {
        val before = state(viewmodel.TileLabels.NUMBERS)
        val now = state(viewmodel.TileLabels.LETTERS)

        assertEquals(before.tileTexts, now.labelWaveFrom(before))
    }

    @Test
    fun theSameSchemeStartsNoWave() {
        val frame = state(viewmodel.TileLabels.LETTERS)

        assertNull(frame.labelWaveFrom(frame))
    }

    @Test
    fun anotherSizeOrAPageStillOpeningOnlyArrives() {
        val before = state(viewmodel.TileLabels.NUMBERS)

        assertEquals<List<String?>?>(List(9) { null }, state(viewmodel.TileLabels.LETTERS, side = 3).labelWaveFrom(before))
        assertEquals<List<String?>?>(List(4) { null }, state(viewmodel.TileLabels.LETTERS, opening = true).labelWaveFrom(before))
    }

    @Test
    fun aDragUnderWayStartsNoWave() {
        val before = state(viewmodel.TileLabels.NUMBERS)
        val dragging = state(viewmodel.TileLabels.LETTERS, drag = web.drag.LineOffset(web.drag.Axis.ROW, 0, 3.0))

        assertNull(dragging.labelWaveFrom(before))
    }
}
