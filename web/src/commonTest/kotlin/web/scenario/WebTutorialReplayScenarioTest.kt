package web.scenario

import kotlin.test.Test

/** GH-289 (WU0) scenario stubs: the Tutorial button, key 8, the save question, and Skip and the end of a replay. The WU that wires the replay fills them in, assertion first. */
class WebTutorialReplayScenarioTest {
    // The button
    @Test
    fun `Tutorial shows beside Give Up mid-game`() = webTest {
        // TODO: 1. Start a game and make one shift.
        // TODO: 2. The Tutorial button shows.
        // TODO: 3. Give Up shows too.
    }

    @Test
    fun `Tutorial shows beside Mordie's solution on a solved board`() = webTest {
        // TODO: 1. Solve the board.
        // TODO: 2. The Tutorial button shows.
        // TODO: 3. Mordie's solution shows.
    }

    @Test
    fun `Tutorial stands alone on a solved board with no solution to show`() = webTest {
        // TODO: 1. Solve the board before Mordie has found his solution.
        // TODO: 2. The Give Up row has no other button.
        // TODO: 3. The Tutorial button shows.
    }

    @Test
    fun `Tutorial is hidden on the solution screen`() = webTest {
        // TODO: 1. Open Mordie's solution.
        // TODO: 2. The Tutorial button is gone.
    }

    @Test
    fun `Tutorial is hidden during steps 1 to 4`() = webTest {
        // TODO: 1. Start the tutorial.
        // TODO: 2. The Tutorial button is gone.
    }

    @Test
    fun `Tutorial is hidden at step 5`() = webTest {
        // TODO: 1. Play steps 1 to 4.
        // TODO: 2. The Tutorial button is gone.
    }

    @Test
    fun `Tutorial is back at step 6`() = webTest {
        // TODO: 1. Play steps 1 to 4.
        // TODO: 2. Wait out the 3 seconds.
        // TODO: 3. The Tutorial button shows.
    }

    @Test
    fun `Tutorial shows no digit on touch`() = webTest {
        // TODO: 1. Start on a touch screen.
        // TODO: 2. The Tutorial button shows.
        // TODO: 3. It has no digit.
    }

    @Test
    fun `Tutorial is hidden when the tutorial switch is off`() = webTest {
        // TODO: 1. Start with the switch off.
        // TODO: 2. The Tutorial button is gone.
    }

    // Key 8
    @Test
    fun `8 starts the replay when there is no unsaved progress`() = webTest {
        // TODO: 1. Press 8.
        // TODO: 2. The tutorial board shows at step 1.
    }

    @Test
    fun `8 does nothing on touch`() = webTest {
        // TODO: 1. Press 8 on a touch screen.
        // TODO: 2. The game is unchanged.
    }

    @Test
    fun `8 does nothing during the tutorial`() = webTest {
        // TODO: 1. Start the tutorial.
        // TODO: 2. Press 8.
        // TODO: 3. The tutorial is still on the same step.
    }

    @Test
    fun `8 does nothing on the solution screen`() = webTest {
        // TODO: 1. Open the solution screen.
        // TODO: 2. Press 8.
        // TODO: 3. Nothing changes.
    }

    // A replay with no unsaved progress
    @Test
    fun `the replay starts at once`() = webTest {
        // TODO: 1. Click Tutorial.
        // TODO: 2. No dialog opens.
        // TODO: 3. The tutorial board shows at step 1.
    }

    @Test
    fun `the replay leaves saves untouched`() = webTest {
        // TODO: 1. Seed a save.
        // TODO: 2. Click Tutorial.
        // TODO: 3. The save is unchanged.
    }

    @Test
    fun `the replay clears the current save name`() = webTest {
        // TODO: 1. Load a save.
        // TODO: 2. Click Tutorial.
        // TODO: 3. The current save name is empty.
    }

    @Test
    fun `focus goes to the board on a computer`() = webTest {
        // TODO: 1. Click Tutorial.
        // TODO: 2. Focus is on the board.
    }

    @Test
    fun `focus goes to the page start on touch`() = webTest {
        // TODO: 1. Click Tutorial on a touch screen.
        // TODO: 2. Focus is at the page start.
    }

    // The save question
    @Test
    fun `with unsaved progress, Tutorial opens the save question`() = webTest {
        // TODO: 1. Make a shift.
        // TODO: 2. Click Tutorial.
        // TODO: 3. The save question opens.
        // TODO: 4. The tutorial has not started.
    }

    @Test
    fun `the question opens on Cancel`() = webTest {
        // TODO: 1. Open the save question.
        // TODO: 2. Focus is on Cancel.
    }

    @Test
    fun `Save opens the Save dialog`() = webTest {
        // TODO: 1. Open the save question.
        // TODO: 2. Choose Save.
        // TODO: 3. The Save dialog opens with the name prefilled.
    }

    @Test
    fun `a successful save starts the replay`() = webTest {
        // TODO: 1. Choose Save.
        // TODO: 2. Confirm the name.
        // TODO: 3. The tutorial board shows at step 1.
    }

    @Test
    fun `a cancelled save starts nothing`() = webTest {
        // TODO: 1. Choose Save.
        // TODO: 2. Cancel the Save dialog.
        // TODO: 3. The game is as it was.
        // TODO: 4. Focus is on the Tutorial button.
    }

    @Test
    fun `a name the save rejects starts nothing`() = webTest {
        // TODO: 1. Choose Save.
        // TODO: 2. Enter a name with spaces.
        // TODO: 3. The tutorial has not started.
    }

    @Test
    fun `Don't save starts the replay`() = webTest {
        // TODO: 1. Choose Don't save.
        // TODO: 2. The tutorial board shows at step 1.
    }

    @Test
    fun `Cancel starts nothing`() = webTest {
        // TODO: 1. Choose Cancel.
        // TODO: 2. The game is as it was.
        // TODO: 3. Focus is on the Tutorial button.
    }

    @Test
    fun `Escape does what Cancel does`() = webTest {
        // TODO: 1. Press Escape in the question.
        // TODO: 2. The game is as it was.
        // TODO: 3. Focus is on the Tutorial button.
    }

    // Skip and the end of a replay
    @Test
    fun `Skip in a replay opens the restore prompt when there are saves`() = webTest {
        // TODO: 1. Seed a save and start a replay.
        // TODO: 2. Click Skip.
        // TODO: 3. `Restore a saved game?` opens.
    }

    @Test
    fun `Skip in a replay opens the size picker when there are no saves`() = webTest {
        // TODO: 1. Start a replay.
        // TODO: 2. Click Skip.
        // TODO: 3. The size picker opens.
    }

    @Test
    fun `cancelling that picker keeps the tutorial board`() = webTest {
        // TODO: 1. Skip a replay with no saves.
        // TODO: 2. Cancel the picker.
        // TODO: 3. The tutorial board stays as an ordinary game.
    }

    @Test
    fun `Skip in a replay opens a share link waiting in the address`() = webTest {
        // TODO: 1. Have a share link in the address.
        // TODO: 2. Start a replay and click Skip.
        // TODO: 3. The shared board opens.
    }

    @Test
    fun `finishing a replay opens no restore and no picker`() = webTest {
        // TODO: 1. Play a replay to step 6.
        // TODO: 2. No dialog opens.
        // TODO: 3. The tutorial board stays the game.
    }

    @Test
    fun `the leave-site prompt is off during the replay`() = webTest {
        // TODO: 1. Start a replay and make the asked shift.
        // TODO: 2. The leave-site prompt does not apply.
    }
}
