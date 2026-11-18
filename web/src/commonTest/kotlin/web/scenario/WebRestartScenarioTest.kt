package web.scenario

import kotlin.test.Test

/** GH-349: the Restart button and its save question. Stubs: WU2 fills them in. */
class WebRestartScenarioTest {
    @Test
    fun restartIsDimmedAtTheStart() {
        // TODO: 1. Start a new 4x4 game
        // TODO: 2. Restart is dimmed
    }

    @Test
    fun restartIsLiveAfterAShift() {
        // TODO: 1. Start a game
        // TODO: 2. Make one shift
        // TODO: 3. Restart is live
    }

    @Test
    fun aLoadedGameWithItsStartKnownRestartsLive() {
        // TODO: 1. Load a save with Mordie data and 7 moves
        // TODO: 2. Restart is live
    }

    @Test
    fun aLoadedGameWithoutItsStartIsDimmedUntilTheFirstShift() {
        // TODO: 1. Load a save with no Mordie data
        // TODO: 2. Restart is dimmed
        // TODO: 3. Make one shift
        // TODO: 4. Restart is live
    }

    @Test
    fun restartIsHiddenOnTheSolutionScreen() {
        // TODO: 1. Give up and open Mordie's solution
        // TODO: 2. The toolbar has no Restart
    }

    @Test
    fun restartIsHiddenDuringTheTutorial() {
        // TODO: 1. Start the tutorial
        // TODO: 2. The toolbar has no Restart
    }

    @Test
    fun restartIsDisabledWhileADialogIsOpen() {
        // TODO: 1. Make a shift
        // TODO: 2. Open the Save dialog
        // TODO: 3. Restart is disabled
    }

    @Test
    fun withMovesRestartAsksToSaveFirst() {
        // TODO: 1. Make three shifts
        // TODO: 2. Press Restart
        // TODO: 3. `Save your game first?` opens
        // TODO: 4. Focus is on Cancel
        // TODO: 5. The board has not changed
    }

    @Test
    fun cancelChangesNothing() {
        // TODO: 1. Press Restart with moves
        // TODO: 2. Press Cancel
        // TODO: 3. The board and Moves are unchanged
        // TODO: 4. Focus is back on Restart
    }

    @Test
    fun escapeIsCancel() {
        // TODO: 1. Press Restart with moves
        // TODO: 2. Press Escape
        // TODO: 3. The dialog is gone and nothing changed
    }

    @Test
    fun dontSaveRestartsTheGame() {
        // TODO: 1. Make three shifts
        // TODO: 2. Press Restart, then Don't save
        // TODO: 3. The board is the start arrangement
        // TODO: 4. Moves is 0
        // TODO: 5. Size and labels are as before
    }

    @Test
    fun dontSaveKeepsTheSaveName() {
        // TODO: 1. Load the save "foo", shift, press Restart, then Don't save
        // TODO: 2. The next Save dialog shows "foo"
    }

    @Test
    fun dontSaveClearsUnsavedProgress() {
        // TODO: 1. Make a shift, press Restart, then Don't save
        // TODO: 2. The game counts as having no unsaved moves
    }

    @Test
    fun saveOpensTheSaveDialogWithTheCurrentName() {
        // TODO: 1. Load "foo", shift, press Restart, then Save
        // TODO: 2. The Save dialog opens with "foo"
    }

    @Test
    fun aSuccessfulSaveRestartsTheGame() {
        // TODO: 1. Press Restart, then Save, then confirm
        // TODO: 2. The save holds the moves made
        // TODO: 3. The game is back at the start
    }

    @Test
    fun aCancelledSaveRestartsNothing() {
        // TODO: 1. Press Restart, then Save, then Cancel
        // TODO: 2. The board and Moves are unchanged
    }

    @Test
    fun aNameWithSpacesRestartsNothing() {
        // TODO: 1. Press Restart, then Save with a name that has spaces
        // TODO: 2. The board and Moves are unchanged
    }

    @Test
    fun aFailedSaveRestartsNothing() {
        // TODO: 1. Make saving fail
        // TODO: 2. Press Restart, then Save
        // TODO: 3. The board and Moves are unchanged
    }

    @Test
    fun aWonBoardRestartsAtOnce() {
        // TODO: 1. Solve the board
        // TODO: 2. Press Restart
        // TODO: 3. No dialog opens
        // TODO: 4. The board is the start arrangement at Moves 0
    }

    @Test
    fun aLoadedGameGoesBackToMordiesOriginal() {
        // TODO: 1. Load a save with Mordie data, shift, press Restart, then Don't save
        // TODO: 2. The board is Mordie's original at Moves 0
    }

    @Test
    fun aLoadedGameWithoutAStartGoesBackToTheBoardAsLoaded() {
        // TODO: 1. Load a save with no Mordie data and 7 moves
        // TODO: 2. Shift, press Restart, then Don't save
        // TODO: 3. The board is as loaded
        // TODO: 4. Moves is 7
    }

    @Test
    fun aSharedBoardRestartsToTheSharedArrangement() {
        // TODO: 1. Open a share link with labels
        // TODO: 2. Shift, press Restart, then Don't save
        // TODO: 3. The board is the shared arrangement
        // TODO: 4. The sender's labels stay
    }

    @Test
    fun restartKeepsTheTileLabels() {
        // TODO: 1. Choose Letters
        // TODO: 2. Shift, press Restart, then Don't save
        // TODO: 3. The labels are still Letters
    }

    @Test
    fun aSecondRestartReturnsToTheSameStart() {
        // TODO: 1. Restart once
        // TODO: 2. Shift again and restart
        // TODO: 3. The board is the same start arrangement
    }

    @Test
    fun aHeldMoveIsDroppedByRestart() {
        // TODO: 1. Change the labels so a wave runs
        // TODO: 2. Press an arrow, which is held
        // TODO: 3. Press Restart
        // TODO: 4. The held move is not made
    }
}
