package web.scenario

import kotlin.test.Test

/** GH-349: the R key, the button's key label and focus. Stubs: WU2 fills them in. */
class WebRestartKeysScenarioTest {
    @Test
    fun rRestartsAGameWithMoves() {
        // TODO: 1. Make a shift
        // TODO: 2. Press R
        // TODO: 3. The save question opens
    }

    @Test
    fun rOnAWonBoardRestartsAtOnce() {
        // TODO: 1. Solve the board
        // TODO: 2. Press R
        // TODO: 3. The game is back at the start
    }

    @Test
    fun theRussianKeyAtKeyRRestarts() {
        // TODO: 1. Switch to a Russian layout
        // TODO: 2. Press the key at the R position (`к`)
        // TODO: 3. The save question opens
    }

    @Test
    fun controlROrCommandRIsLeftToTheBrowser() {
        // TODO: 1. Press Ctrl+R
        // TODO: 2. Nothing happens in the game
    }

    @Test
    fun rDoesNothingOnTouch() {
        // TODO: 1. Use a touch device
        // TODO: 2. Press R
        // TODO: 3. Nothing happens
    }

    @Test
    fun rDoesNothingDuringTheTutorial() {
        // TODO: 1. Start the tutorial
        // TODO: 2. Press R
        // TODO: 3. Nothing happens
    }

    @Test
    fun rDoesNothingWhileADialogIsOpen() {
        // TODO: 1. Open the Save dialog
        // TODO: 2. Press R
        // TODO: 3. Nothing happens
    }

    @Test
    fun rDoesNothingWhileRestartIsDimmed() {
        // TODO: 1. Start a game
        // TODO: 2. Press R
        // TODO: 3. Nothing happens
    }

    @Test
    fun onTheSolutionScreenWithTheBoardFocusedRIsTheReplaysRestart() {
        // TODO: 1. Open Mordie's solution and step forward
        // TODO: 2. Press R
        // TODO: 3. The replay goes back to the original arrangement
    }

    @Test
    fun aHeldRIsOnePress() {
        // TODO: 1. Hold R
        // TODO: 2. The save question opens once
    }

    @Test
    fun theToolbarNamesTheKeyOnAComputer() {
        // TODO: 1. Use a computer
        // TODO: 2. The button reads `Restart R`
        // TODO: 3. On touch it reads `Restart`
    }

    @Test
    fun aSuccessfulRestartFocusesTheBoardOnAComputer() {
        // TODO: 1. Restart with Don't save
        // TODO: 2. Focus is on the board
    }

    @Test
    fun onTouchFocusGoesToNew() {
        // TODO: 1. Restart on touch
        // TODO: 2. Focus is on `[ New ]`
    }

    @Test
    fun cancelAfterRGoesBackWhereRWasPressed() {
        // TODO: 1. Press R with focus on a toolbar button
        // TODO: 2. Press Cancel
        // TODO: 3. Focus is back where R was pressed
    }
}
