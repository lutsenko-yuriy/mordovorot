package web.scenario

import kotlin.test.Test

/** GH-272 scenarios: the Share dialog's `Include my progress` toggle, through [WebHarness]. Stubs until WU2. */
class WebShareProgressScenarioTest {

    @Test
    fun `the toggle starts off and names the sender's progress`() = webTest {
        // TODO: 1. Make 3 moves and open Share
        // TODO: 2. The toggle reads `Include my progress (3 moves, not solved)`
        // TODO: 3. The toggle is off and the link has no `p=`
        // TODO: 4. `screen_share_dialog` is sent with `include_progress: false`
    }

    @Test
    fun `turning it on adds the progress to the link and QR at once`() = webTest {
        // TODO: 1. Make 3 moves and open Share
        // TODO: 2. Flip the toggle
        // TODO: 3. The link ends in `&p=n3`
        // TODO: 4. The QR code matches the link
    }

    @Test
    fun `flipping it puts Copy link back and keeps focus on the toggle`() = webTest {
        // TODO: 1. Copy the link: the button reads `Copied!`
        // TODO: 2. Flip the toggle
        // TODO: 3. The button reads `Copy link`
        // TODO: 4. Focus is on the progress toggle
    }

    @Test
    fun `flipping it sends share_progress_toggled`() = webTest {
        // TODO: 1. Flip the toggle on, then off
        // TODO: 2. Two `share_progress_toggled` events are sent, with `enabled` true and then false
    }

    @Test
    fun `copying the link sends includes_progress`() = webTest {
        // TODO: 1. Copy with the toggle on, then with it off
        // TODO: 2. `board_share_link_copied` carries `includes_progress` true, then false
    }

    @Test
    fun `a solved board offers solved in N moves`() = webTest {
        // TODO: 1. Solve the board in 2 moves and open Share
        // TODO: 2. The toggle reads `Include my progress (solved in 2 moves)`
        // TODO: 3. Flip it: the link ends in `&p=s2`
    }

    @Test
    fun `after Give Up the toggle offers gave up after N moves`() = webTest {
        // TODO: 1. Make 3 moves and give up
        // TODO: 2. Open Share
        // TODO: 3. The toggle reads `Include my progress (gave up after 3 moves)`
        // TODO: 4. Flip it: the link ends in `&p=g3`
    }

    @Test
    fun `solving, then opening Mordie's solution counts as solved`() = webTest {
        // TODO: 1. Solve the board and open Mordie's solution
        // TODO: 2. Open Share
        // TODO: 3. The toggle reads `(solved in N moves)`
    }

    @Test
    fun `a loaded save offers the same progress as the live game`() = webTest {
        // TODO: 1. Make 3 moves and save
        // TODO: 2. Start a new game and load the save
        // TODO: 3. Open Share
        // TODO: 4. The toggle reads `(3 moves, not solved)`
    }

    @Test
    fun `a gave-up save from before GH-273 sends gave up with no count`() = webTest {
        // TODO: 1. Load an old solution-screen save
        // TODO: 2. Open Share
        // TODO: 3. The toggle reads `Include my progress (gave up)`
        // TODO: 4. Flip it: the link ends in `&p=g`
    }

    @Test
    fun `at 0 moves the toggle is dimmed and can't be switched on`() = webTest {
        // TODO: 1. Open Share with no moves made
        // TODO: 2. The toggle reads `Include my progress` and is disabled
        // TODO: 3. Pressing it changes nothing and the link has no `p=`
    }

    @Test
    fun `giving up at 0 moves keeps the toggle dimmed`() = webTest {
        // TODO: 1. Load a 0-move save, give up and open Share
        // TODO: 2. The toggle is dimmed and the link has no `p=`
    }

    @Test
    fun `a count of 1 reads 1 move in the toggle`() = webTest {
        // TODO: 1. Make 1 move and open Share
        // TODO: 2. The toggle reads `(1 move, not solved)`
    }

    @Test
    fun `an unknown start still sends the real count`() = webTest {
        // TODO: 1. Load a save with moves and an unknown start
        // TODO: 2. Open Share
        // TODO: 3. The toggle is enabled
        // TODO: 4. Flip it: the link carries the count
    }

    @Test
    fun `a sender who opened a shared link sends their own progress`() = webTest {
        // TODO: 1. Open a link with `p=s12`
        // TODO: 2. Make 2 moves and open Share
        // TODO: 3. The toggle reads `(2 moves, not solved)`
        // TODO: 4. Flip it: the link ends in `&p=n2`
    }

    @Test
    fun `the choice is remembered, starting off the first time`() = webTest {
        // TODO: 1. Turn the toggle on
        // TODO: 2. Reload the page
        // TODO: 3. Open Share and the toggle is on
        // TODO: 4. A fresh profile starts off
    }

    @Test
    fun `a remembered on at 0 moves shows off and is kept`() = webTest {
        // TODO: 1. Turn the toggle on and reload
        // TODO: 2. Start a new game and open Share (0 moves): the toggle is off
        // TODO: 3. Make a move: the toggle is on again
    }

    @Test
    fun `both toggles can be on in one link`() = webTest {
        // TODO: 1. Turn the labels toggle on, then the progress toggle
        // TODO: 2. The link carries both `l=` and `p=`
    }
}
