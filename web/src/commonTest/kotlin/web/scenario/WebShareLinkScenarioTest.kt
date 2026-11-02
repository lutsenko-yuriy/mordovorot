package web.scenario

import kotlin.test.Test

/** GH-243 scenarios: opening a share link at startup and on `hashchange`, through [WebHarness]. */
class WebShareLinkScenarioTest {

    @Test
    fun `a link at startup starts that board at once, skipping the restore prompt`() = webTest {
        // TODO: 1. Seed a save, so a restore prompt would normally show
        // TODO: 2. Start the page with the fragment #b=<5x5 board>
        // TODO: 3. The 5x5 board is on screen at Moves 0, with no Load or size dialog
        // TODO: 4. A Mordie search starts on that board
        // TODO: 5. The status line says `Started a shared 5x5 board.`
        // TODO: 6. app_launched, then shared_board_opened {result: started, board_size: 5}
        // TODO: 7. No startup_restore_prompt_shown and no new_game_size_selected
    }

    @Test
    fun `a started link is a fresh game, and the address bar is cleaned`() = webTest {
        // TODO: 1. Start with a link
        // TODO: 2. The address fake had clearFragment called once
        // TODO: 3. There is no unsaved progress and the save name is cleared
        // TODO: 4. Labels are back to the player's preference, not the sender's
        // TODO: 5. Reload over the same store with no link: the usual startup runs and the shared game is not restored
    }

    @Test
    fun `a bad link says why, then the usual startup`() = webTest {
        // TODO: 1. For each of a garbled board, a 12x12 board and an unsolvable 3x3, start with that link
        // TODO: 2. The Can't open this board dialog shows the matching message, with no Repair offer
        // TODO: 3. Press OK: the usual startup follows (the restore prompt with a save present, the size picker without)
        // TODO: 4. shared_board_opened {result: rejected, reason: invalid, unsupported_size or unsolvable} fires, with no board_size
        // TODO: 5. The address is cleaned
    }

    @Test
    fun `a fragment without a board changes nothing`() = webTest {
        // TODO: 1. Start with #about, then with #x=1
        // TODO: 2. The usual startup runs
        // TODO: 3. No shared_board_opened fires
        // TODO: 4. The address is left alone
    }

    @Test
    fun `a link opened in a playing tab with no unsaved progress starts at once`() = webTest {
        // TODO: 1. startOn, no shifts
        // TODO: 2. A hashchange arrives with a 4x4 link
        // TODO: 3. The board changes at once with no dialog, and the status line says `Started a shared 4x4 board.`
        // TODO: 4. shared_board_opened {result: started, board_size: 4} fires
    }

    @Test
    fun `a link opened over unsaved progress asks first, and Cancel keeps the game`() = webTest {
        // TODO: 1. Shift once, so there is unsaved progress
        // TODO: 2. A hashchange arrives with a link
        // TODO: 3. Open the shared board? shows, focused on Cancel
        // TODO: 4. Press Cancel: the game and its moves are unchanged and the link is cleared from the address
        // TODO: 5. No shared_board_opened fires
        // TODO: 6. Repeat, then press Open: the shared board starts and the event fires
    }

    @Test
    fun `a link that arrives during an open dialog waits until it closes, and the latest wins`() = webTest {
        // TODO: 1. Open Save, so a dialog is up
        // TODO: 2. Two links arrive in a row
        // TODO: 3. Nothing changes while the dialog is open
        // TODO: 4. Cancel the dialog: the second link's board starts
        // TODO: 5. Only one shared_board_opened fires
    }

    @Test
    fun `a bad link in a playing tab shows the notice and keeps the game`() = webTest {
        // TODO: 1. Shift once
        // TODO: 2. An unsolvable link arrives
        // TODO: 3. The notice shows, and OK returns to the same game with the same moves
        // TODO: 4. shared_board_opened {result: rejected, reason: unsolvable} fires
    }
}
