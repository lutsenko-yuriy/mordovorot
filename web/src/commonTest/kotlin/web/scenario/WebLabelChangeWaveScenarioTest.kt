package web.scenario

import kotlin.test.Test

/** GH-343: the label wave on every label change outside the Tile labels dialog, and the arriving half alone on a page load or another board size. Stubs: WU1 fills them in. */
class WebLabelChangeWaveScenarioTest {

    @Test
    fun aSharePromptOpeningWavesFromTheOldLabels() {
        // TODO: 1. Start on Numbers
        // TODO: 2. A link with Pairs labels arrives
        // TODO: 3. The prompt opens
        // TODO: 4. The frame starts a wave whose old texts are the Numbers
        // TODO: 5. The board shows Pairs behind the prompt
    }

    @Test
    fun noWavesTheBoardBackToThePlayersLabels() {
        // TODO: 1. Open the prompt as in aSharePromptOpeningWavesFromTheOldLabels
        // TODO: 2. Press No (and, in another run, Escape)
        // TODO: 3. The frame starts a wave from the Pairs texts
        // TODO: 4. The board shows Numbers again
    }

    @Test
    fun yesStartsNoWave() {
        // TODO: 1. Open the prompt as in aSharePromptOpeningWavesFromTheOldLabels
        // TODO: 2. Press Yes
        // TODO: 3. No frame after it starts a wave
        // TODO: 4. The player's preference is Pairs
    }

    @Test
    fun aLinkWithTheLabelsAlreadyShownStartsNoWave() {
        // TODO: 1. Start on Pairs
        // TODO: 2. A link with Pairs labels arrives
        // TODO: 3. No prompt opens and no frame starts a wave
    }

    @Test
    fun aLinkAtStartupArrivesOnly() {
        // TODO: 1. Start with a link with Pairs labels
        // TODO: 2. The startup frame starts a wave
        // TODO: 3. Every old text in it is empty
    }

    @Test
    fun aLinkToAnotherSizeArrivesOnly() {
        // TODO: 1. Start on 4x4
        // TODO: 2. A link to a 5x5 board with Pairs labels arrives
        // TODO: 3. The wave's old texts are all empty
    }

    @Test
    fun aNewGameBackToThePreferenceWaves() {
        // TODO: 1. Start on 4x4 with Numbers as the preference
        // TODO: 2. Make a Letters board without changing the preference
        // TODO: 3. Press New game
        // TODO: 4. The frame starts a wave from the Letters texts
    }

    @Test
    fun aNewGameInTheSameSchemeDoesNotWave() {
        // TODO: 1. Start on 4x4 with Numbers
        // TODO: 2. Press New game
        // TODO: 3. No frame starts a wave
    }

    @Test
    fun aNewGameOfAnotherSizeArrivesOnly() {
        // TODO: 1. Start on a size the preference fits, with a Letters-only preference
        // TODO: 2. Press New game and choose a size the preference doesn't fit
        // TODO: 3. Numbers stand in and the wave's old texts are all empty
    }

    @Test
    fun aToolbarLoadWithOtherLabelsWaves() {
        // TODO: 1. Save a board with Letters labels
        // TODO: 2. Start a game with Numbers
        // TODO: 3. Load the save
        // TODO: 4. The frame starts a wave from the Numbers texts
    }

    @Test
    fun aToolbarLoadWithTheSameLabelsDoesNotWave() {
        // TODO: 1. Save a board with Numbers labels
        // TODO: 2. Load it into a game with Numbers
        // TODO: 3. No frame starts a wave
    }

    @Test
    fun repairAndLoadWavesLikeALoad() {
        // TODO: 1. Save a damaged board with Letters labels
        // TODO: 2. Start a game with Numbers
        // TODO: 3. Choose Repair & load
        // TODO: 4. The frame starts a wave from the Numbers texts
    }

    @Test
    fun aSaveRestoredAtStartupArrivesOnly() {
        // TODO: 1. Seed an autosave with Letters labels
        // TODO: 2. Start the page with Numbers as the preference
        // TODO: 3. The startup frame starts a wave
        // TODO: 4. Every old text in it is empty
    }

    @Test
    fun aPlainStartupStartsNoWave() {
        // TODO: 1. Start the page with nothing saved
        // TODO: 2. No frame starts a wave
    }

    @Test
    fun theTutorialReplayWavesBackToThePreference() {
        // TODO: 1. Set Letters as the preference
        // TODO: 2. Run the tutorial from the toolbar
        // TODO: 3. Finish it
        // TODO: 4. The frame that goes back to the preference starts a wave
    }

    @Test
    fun skipThenRestoreWavesBothHalves() {
        // TODO: 1. Seed an autosave with Letters labels and start the tutorial
        // TODO: 2. Press Skip
        // TODO: 3. The restored board's frame starts a wave
        // TODO: 4. Its old texts are the tutorial board's, not empty
    }

    @Test
    fun analyticsSendNothingNew() {
        // TODO: 1. Answer a share prompt, start a new game and load a save with other labels
        // TODO: 2. The events sent are the ones sent before this change, and no new event
    }
}
