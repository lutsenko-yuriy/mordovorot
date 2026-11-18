package web.scenario

import kotlin.test.Test

/** GH-343: a row of the Tile labels dialog that doesn't fit the board shows its limit in a bubble after a press. Stubs: WU3 fills them in. */
class WebLabelLimitScenarioTest {

    @Test
    fun pressingARowThatDoesNotFitSetsTheTip() {
        // TODO: 1. Start on 6x6 and open the Labels dialog
        // TODO: 2. Press Letters
        // TODO: 3. The dialog's tip is Letters
    }

    @Test
    fun pressingItPicksNothing() {
        // TODO: 1. Press Letters on 6x6
        // TODO: 2. The marked label is unchanged
        // TODO: 3. The board's labels and the preference are unchanged
        // TODO: 4. No analytics event is sent
    }

    @Test
    fun focusGoesToThatRow() {
        // TODO: 1. Press Letters on 6x6
        // TODO: 2. Focus is on the Letters row
    }

    @Test
    fun pressingARowThatFitsClearsTheTip() {
        // TODO: 1. Press Letters on 6x6
        // TODO: 2. Press a row that fits
        // TODO: 3. The tip is empty
    }

    @Test
    fun openingAnotherGroupClearsTheTip() {
        // TODO: 1. Press Letters on 6x6
        // TODO: 2. Press another group's header
        // TODO: 3. The tip is empty
    }

    @Test
    fun closingTheDialogDropsTheTip() {
        // TODO: 1. Press Letters on 6x6
        // TODO: 2. Press Cancel, then reopen the dialog
        // TODO: 3. The tip is empty
        // TODO: 4. Do the same with OK
    }

    @Test
    fun theTipStartsNoWaveOrSlide() {
        // TODO: 1. Press Letters on 6x6
        // TODO: 2. No frame starts a board wave, a preview wave or a group slide
    }
}
