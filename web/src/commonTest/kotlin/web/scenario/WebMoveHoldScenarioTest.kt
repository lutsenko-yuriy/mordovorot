package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import mordie.SearchProgress
import mordie.SolutionMove
import viewmodel.TileLabels
import web.Arrow
import web.Focus

/** GH-343: a move pressed during a label wave waits for it, one at most. Runs with the harness's real LabelMotion on the test clock. */
@OptIn(ExperimentalCoroutinesApi::class)
class WebMoveHoldScenarioTest {

    private val start = shown(ONE_SHIFT_FROM_SOLVED)

    /** [ONE_SHIFT_FROM_SOLVED] with row 1 shifted one tile right. */
    private val rowOneRight = shown(intArrayOf(3, 0, 1, 2, 7, 4, 5, 6) + IntArray(8) { it + 8 })

    private fun TestScope.page() = WebHarness(backgroundScope, motion = true).startOn()

    /** Picks Letters in the dialog, then Cancel: the board waves back to the numbers. */
    private fun WebHarness.startWave() {
        game.labels.open()
        game.labels.onSelected(TileLabels.LETTERS)
        game.labels.onAnswered(ok = false)
        assertTrue(waving)
    }

    private fun TestScope.passMs(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun anArrowPressedDuringTheWaveIsMadeWhenItEnds() = webTest {
        val page = page()
        page.startWave()

        page.game.onArrow(Arrow.LEFT, 0)
        passMs(599)
        assertEquals(start, page.state.tiles)
        assertFalse(page.state.solved)

        passMs(2)
        assertTrue(page.state.solved)
    }

    @Test
    fun theLatestPressWins() = webTest {
        val page = page()
        page.startWave()

        page.game.onArrow(Arrow.LEFT, 0)
        page.game.onArrow(Arrow.RIGHT, 1)
        passMs(601)

        assertEquals(rowOneRight, page.state.tiles)
        assertEquals(1, page.state.moves)
    }

    @Test
    fun aShiftKeyIsHeldToo() = webTest {
        val page = page()
        page.startWave()

        page.press("ArrowLeft", from = Focus.Board)
        passMs(599)
        assertEquals(start, page.state.tiles)

        passMs(2)
        // The selection starts on row 0: Left puts it in place.
        assertTrue(page.state.solved)
    }

    @Test
    fun aClickedArrowMovesFocusToTheBoardAtOnce() = webTest {
        val page = page()
        page.startWave()

        page.game.onArrow(Arrow.LEFT, 0, byMouse = true)

        assertEquals(Focus.Board, page.state.focus)
        assertEquals(start, page.state.tiles)
    }

    @Test
    fun aReplayStepIsHeld() = webTest {
        val page = WebHarness(backgroundScope, motion = true).startOn(REPLAY_START)
        page.mordieSearches.latest.progress = SearchProgress(2, found = true, elapsedMs = 0, solution = REPLAY_PATH)
        repeat(5) { page.game.onArrow(Arrow.UP, 3) }
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)
        val at = page.state.solution!!.current
        page.startWave()

        page.game.solution.onReplayNext()
        passMs(599)
        assertEquals(at, page.state.solution!!.current)

        passMs(2)
        assertTrue(page.state.solution!!.current != at)
    }

    @Test
    fun aToolbarActionDropsTheHeldMove() = webTest {
        val page = page()
        page.startWave()

        page.game.onArrow(Arrow.LEFT, 0)
        page.game.onSave()
        page.game.onSaveAnswered(null)
        passMs(700)

        assertEquals(start, page.state.tiles)
    }

    @Test
    fun aDragStartFinishesTheWaveAndDropsAHeldMove() = webTest {
        val page = page()
        page.startWave()

        page.game.onArrow(Arrow.LEFT, 0)
        page.game.onDragStart(2, 0, 100.0, 100.0, 40.0)
        page.game.onDragMove(110.0, 100.0)
        assertFalse(page.waving)

        page.game.onDragCancel()
        passMs(700)
        assertEquals(start, page.state.tiles)
    }

    @Test
    fun withoutMotionNothingWaits() = webTest {
        val page = WebHarness(backgroundScope, motion = true, reducedMotion = true).startOn()
        page.game.labels.open()
        page.game.labels.onSelected(TileLabels.LETTERS)
        page.game.labels.onAnswered(ok = false)
        assertFalse(page.waving)

        page.game.onArrow(Arrow.LEFT, 0)

        assertTrue(page.state.solved)
    }

    @Test
    fun theTutorialJudgesAHeldShiftWhenItIsMade() = webTest {
        val page = WebHarness(backgroundScope, motion = true, firstLaunch = true).start()
        page.startWave()

        page.stepByArrow(1)
        assertTrue(page.analytics.events.none { it.name == "tutorial_step_completed" })
        passMs(601)

        assertEquals(1, page.props("tutorial_step_completed").size)
    }

    @Test
    fun theShiftsAnalyticsComeOnceAtTheShift() = webTest {
        val page = page()
        page.startWave()

        val said = page.said.size

        page.game.onArrow(Arrow.LEFT, 0)
        assertTrue(page.props("board_solved").isEmpty())
        assertEquals(said, page.said.size)

        passMs(601)
        assertEquals(1, page.props("board_solved").size)
        assertEquals(said + 1, page.said.size)
    }

    private companion object {
        /** 4x4 two moves from solved: row 0 right once, then column 1 down twice. */
        val REPLAY_START = intArrayOf(9, 2, 3, 0, 4, 13, 6, 7, 8, 1, 10, 11, 12, 5, 14, 15)
        val REPLAY_PATH = listOf(SolutionMove(isRow = true, index = 0, amount = 1), SolutionMove(isRow = false, index = 1, amount = 2))
    }
}
