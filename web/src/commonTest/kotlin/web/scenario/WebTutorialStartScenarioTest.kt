package web.scenario

import kotlin.test.Test

/** GH-288 (WU0) scenario stubs: when the tutorial shows, how it starts and how it ends (Skip, the share link, the leave-site prompt). The WU that wires the tutorial fills them in. */
class WebTutorialStartScenarioTest {

    @Test
    fun `the tutorial shows on a first launch`() = webTest {
        // TODO: 1. Launch with no saves and no 'seen' mark.
        // TODO: 2. The board is the tutorial board.
        // TODO: 3. The panel shows step 1's text.
        // TODO: 4. No dialog is open.
    }

    @Test
    fun `the toolbar and the Give Up row are replaced by the panel`() = webTest {
        // TODO: 1. Launch as a first launch.
        // TODO: 2. The panel has Controls, Labels and Skip.
        // TODO: 3. There is no toolbar.
        // TODO: 4. There is no Give Up row.
    }

    @Test
    fun `a player with a save does not see the tutorial`() = webTest {
        // TODO: 1. Seed a save.
        // TODO: 2. Launch.
        // TODO: 3. `Restore a saved game?` opens.
        // TODO: 4. There is no panel.
    }

    @Test
    fun `a corrupted save also counts as a save`() = webTest {
        // TODO: 1. Seed a corrupted entry.
        // TODO: 2. Launch.
        // TODO: 3. There is no panel.
    }

    @Test
    fun `a seen tutorial does not show again`() = webTest {
        // TODO: 1. Mark the tutorial seen, with no saves.
        // TODO: 2. Launch.
        // TODO: 3. The size picker opens.
        // TODO: 4. There is no panel.
    }

    @Test
    fun `a reload halfway restarts from step 1`() = webTest {
        // TODO: 1. Make step 1's shift.
        // TODO: 2. Relaunch on the same storage.
        // TODO: 3. The board is the tutorial board again.
        // TODO: 4. The panel shows step 1.
    }

    @Test
    fun `Skip with no saves opens the size picker`() = webTest {
        // TODO: 1. Press Skip.
        // TODO: 2. The panel is gone and the toolbar is back.
        // TODO: 3. The size picker opens.
    }

    @Test
    fun `Skip with a save asks to restore`() = webTest {
        // TODO: 1. Seed a save, mark the tutorial not seen, launch.
        // TODO: 2. Press Skip.
        // TODO: 3. `Restore a saved game?` opens.
    }

    @Test
    fun `Cancel in the size picker after Skip keeps the tutorial board`() = webTest {
        // TODO: 1. Press Skip.
        // TODO: 2. Cancel the size picker.
        // TODO: 3. The tutorial board stays.
        // TODO: 4. The toolbar is back.
    }

    @Test
    fun `Skip marks the tutorial seen`() = webTest {
        // TODO: 1. Press Skip.
        // TODO: 2. Relaunch.
        // TODO: 3. There is no panel.
    }

    @Test
    fun `finishing the tutorial marks it seen`() = webTest {
        // TODO: 1. Play the four steps.
        // TODO: 2. Wait 3 s.
        // TODO: 3. Relaunch with no moves kept.
        // TODO: 4. There is no panel.
    }

    @Test
    fun `a share link at launch waits for the tutorial`() = webTest {
        // TODO: 1. Launch with a share link.
        // TODO: 2. The tutorial shows.
        // TODO: 3. The link is still in the address.
        // TODO: 4. The shared board isn't opened.
    }

    @Test
    fun `after the last step the waiting link asks first`() = webTest {
        // TODO: 1. Launch with a share link.
        // TODO: 2. Play to step 6.
        // TODO: 3. `Open the shared board?` opens.
    }

    @Test
    fun `Yes opens the shared board in place of the tutorial board`() = webTest {
        // TODO: 1. Launch with a share link and play to step 6.
        // TODO: 2. Answer Yes.
        // TODO: 3. The shared board is the game.
        // TODO: 4. The link is cleared from the address.
    }

    @Test
    fun `Cancel on the waiting link keeps the tutorial board and opens no picker`() = webTest {
        // TODO: 1. Launch with a share link and play to step 6.
        // TODO: 2. Answer Cancel.
        // TODO: 3. The tutorial board stays.
        // TODO: 4. No size picker opens.
    }

    @Test
    fun `Skip at step 1 opens the waiting link straight away`() = webTest {
        // TODO: 1. Launch with a share link.
        // TODO: 2. Press Skip with no moves made.
        // TODO: 3. The shared board opens without a question.
    }

    @Test
    fun `a link arriving mid-tutorial waits`() = webTest {
        // TODO: 1. Be on step 2.
        // TODO: 2. A `hashchange` brings a share link.
        // TODO: 3. The board and the panel are unchanged.
        // TODO: 4. After step 6 the link is asked about.
    }

    @Test
    fun `no leave-site prompt during steps 1 to 5`() = webTest {
        // TODO: 1. Make step 1's shift.
        // TODO: 2. The page isn't asked to confirm leaving.
    }

    @Test
    fun `the leave-site prompt returns after step 6`() = webTest {
        // TODO: 1. Play to step 6.
        // TODO: 2. The page asks to confirm leaving.
    }
}
