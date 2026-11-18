package web.scenario

import kotlin.test.Test

/**
 * GH-342: the label wave on the board and the preview strip, and the group slide in the Tile labels dialog.
 * Stubs: `implement` replaces each TODO with driver code over `WebHarness.frames`.
 */
class WebLabelWaveScenarioTest {

    // The board wave
    @Test
    fun pickingARowStartsABoardWaveFromTheOldLabels() = webTest {
        // TODO: 1. Pick Letters in the Tile labels dialog
        // TODO: 2. A board wave starts
        // TODO: 3. Its old texts are 1 … 16
        // TODO: 4. The board's new texts are A … P
    }

    @Test
    fun pickingARowStartsAPreviewWave() = webTest {
        // TODO: 1. Pick Letters
        // TODO: 2. A preview wave starts, from 1 2 3 to A B C
        // TODO: 3. The preview wave ends before the board wave ends
    }

    @Test
    fun unchangedLabelsDoNotWave() = webTest {
        // TODO: 1. Mark Numbers, which is already marked
        // TODO: 2. No board wave starts
        // TODO: 3. No preview wave starts
    }

    @Test
    fun rapidPicksEachWaveFromThePreviousTarget() = webTest {
        // TODO: 1. Press ArrowDown three times
        // TODO: 2. Three board waves start
        // TODO: 3. Each one starts from the previous wave's target texts
        // TODO: 4. The last frame's board texts are the last marked scheme's
    }

    @Test
    fun theBoardWaveTakesTheSameTimeOnEverySize() = webTest {
        // TODO: 1. Pick Letters on a 3x3 board, then on a 9x9 board
        // TODO: 2. The wave is over after 600 ms on both
        // TODO: 3. The gap between tiles is smaller on 9x9
    }

    // Cancel and OK
    @Test
    fun cancelWavesTheBoardBack() = webTest {
        // TODO: 1. Pick Letters
        // TODO: 2. Press Cancel
        // TODO: 3. The closing frame starts a wave back to 1 … 16
    }

    @Test
    fun escapeWavesTheBoardBack() = webTest {
        // TODO: 1. Pick Letters
        // TODO: 2. Press Escape
        // TODO: 3. The closing frame starts a wave back to 1 … 16
    }

    @Test
    fun okStartsNoWaveAndNeverShowsTheOldLabels() = webTest {
        // TODO: 1. Pick Letters
        // TODO: 2. Press OK
        // TODO: 3. No frame after the pick starts a wave
        // TODO: 4. No frame after the pick shows 1 … 16
    }

    // The groups
    @Test
    fun openingAnotherGroupStartsASlideFromTheOpenOne() = webTest {
        // TODO: 1. Press the Latin letters header while Numbers is open
        // TODO: 2. A slide starts from Numbers
        // TODO: 3. The labels start no wave
    }

    @Test
    fun pressingTheOpenGroupStartsNoSlide() = webTest {
        // TODO: 1. Press the open group's header
        // TODO: 2. No slide starts
    }

    @Test
    fun openingTheDialogAnimatesNothing() = webTest {
        // TODO: 1. Open the Tile labels dialog
        // TODO: 2. No wave starts
        // TODO: 3. No slide starts
    }

    // Interruption
    @Test
    fun aGroupPressDuringAWaveLeavesTheWaveTargetsInPlace() = webTest {
        // TODO: 1. Pick Letters to start a wave
        // TODO: 2. Press the Latin letters header at once
        // TODO: 3. The slide starts from the group that was open
        // TODO: 4. The board still shows A … P
    }

    // Where it does not play
    @Test
    fun labelsChangedOutsideTheDialogDoNotWave() = webTest {
        // TODO: 1. Accept a share link's labels
        // TODO: 2. No wave starts
        // TODO: 3. Start a new game, then load a save with its own labels
        // TODO: 4. No wave starts
    }

    @Test
    fun theLoadPickerThatOpensTheSameDialogWaves() = webTest {
        // TODO: 1. Load a 6x6 save with Letters labels
        // TODO: 2. The picker opens
        // TODO: 3. Pick Cyrillic letter pairs
        // TODO: 4. A board wave starts
    }

    // What does not change
    @Test
    fun analyticsSendsNothingNew() = webTest {
        // TODO: 1. Pick three rows
        // TODO: 2. Open two groups
        // TODO: 3. Press Cancel
        // TODO: 4. Only screen_labels_dialog and dialog_cancelled are sent
    }

    @Test
    fun focusAndKeysAreUnchanged() = webTest {
        // TODO: 1. Open the dialog, pick a row, open a group and press Cancel
        // TODO: 2. Each frame's focus matches what WebLabelGroupsScenarioTest already expects
    }
}
