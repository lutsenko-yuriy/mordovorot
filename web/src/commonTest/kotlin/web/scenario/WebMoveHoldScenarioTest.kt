package web.scenario

import kotlin.test.Test

/** GH-343: a move pressed during a label wave waits for it, one at most. Runs with the harness's real LabelMotion on the test clock. Stubs: WU2 fills them in. */
class WebMoveHoldScenarioTest {

    @Test
    fun anArrowPressedDuringTheWaveIsMadeWhenItEnds() {
        // TODO: 1. Pick Letters in the dialog, then press Cancel to start a wave
        // TODO: 2. Click an arrow
        // TODO: 3. The board hasn't shifted at 599 ms
        // TODO: 4. It has shifted at 600 ms
    }

    @Test
    fun theLatestPressWins() {
        // TODO: 1. Start a wave as in anArrowPressedDuringTheWaveIsMadeWhenItEnds
        // TODO: 2. Click one arrow, then another
        // TODO: 3. At 600 ms exactly one shift is made
        // TODO: 4. It is the second arrow's
    }

    @Test
    fun aShiftKeyIsHeldToo() {
        // TODO: 1. Start a wave
        // TODO: 2. Press a board shift key
        // TODO: 3. The shift is made when the wave ends
    }

    @Test
    fun aClickedArrowMovesFocusToTheBoardAtOnce() {
        // TODO: 1. Start a wave and click an arrow
        // TODO: 2. Focus is on the board immediately, before the wave ends
    }

    @Test
    fun aReplayStepIsHeld() {
        // TODO: 1. Open the solution screen and start a wave with the Labels dialog's Cancel
        // TODO: 2. Press Next
        // TODO: 3. The step is made when the wave ends
    }

    @Test
    fun aToolbarActionDropsTheHeldMove() {
        // TODO: 1. Start a wave and click an arrow
        // TODO: 2. Press a toolbar button
        // TODO: 3. When the wave would have ended, no shift is made
    }

    @Test
    fun aDragStartFinishesTheWaveAndDropsAHeldMove() {
        // TODO: 1. Start a wave and click an arrow
        // TODO: 2. Start a drag
        // TODO: 3. The wave is over
        // TODO: 4. The held shift is dropped
    }

    @Test
    fun withoutMotionNothingWaits() {
        // TODO: 1. Run with reduced motion and change the labels
        // TODO: 2. Click an arrow
        // TODO: 3. The shift is made at once
    }

    @Test
    fun theTutorialJudgesAHeldShiftWhenItIsMade() {
        // TODO: 1. Start the tutorial's shift step with a wave running
        // TODO: 2. Press the shift
        // TODO: 3. The tutorial advances when the shift is made, not at the press
    }

    @Test
    fun theShiftsAnalyticsComeOnceAtTheShift() {
        // TODO: 1. Start a wave and click an arrow
        // TODO: 2. No shift event is sent at the click
        // TODO: 3. One shift event and one announcement come when the shift is made
    }
}
