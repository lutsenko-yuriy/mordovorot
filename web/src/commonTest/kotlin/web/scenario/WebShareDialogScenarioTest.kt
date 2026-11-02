package web.scenario

import kotlin.test.Test

/** GH-243 scenarios: the Share dialog and Copy link, through [WebHarness]. */
class WebShareDialogScenarioTest {

    @Test
    fun `Share opens the dialog with the starting board's link`() = webTest {
        // TODO: 1. Start a 4x4 game from a known board and make 3 shifts
        // TODO: 2. Press Share
        // TODO: 3. The link holds the original arrangement, not the shifted one
        // TODO: 4. The dialog's note says `from the same 4x4 board`
        // TODO: 5. Focus is on Copy link
        // TODO: 6. screen_share_dialog {board_size: 4, start_known: true} fires
    }

    @Test
    fun `the link opens the same board, with no moves and no labels`() = webTest {
        // TODO: 1. Share from a game that has the Roman labels and moves
        // TODO: 2. Feed the shown link to a fresh page as its fragment
        // TODO: 3. The board equals the original arrangement, with Moves 0 and the new page's own label preference
    }

    @Test
    fun `an unknown start shares the current board`() = webTest {
        // TODO: 1. Load an old save that has no Mordie data and has moves
        // TODO: 2. Press Share
        // TODO: 3. The link holds the current board
        // TODO: 4. The note reads `from this 4x4 board as it is now`
        // TODO: 5. start_known is false
    }

    @Test
    fun `the QR code is the link`() = webTest {
        // TODO: 1. Open Share
        // TODO: 2. The dialog's QR matrix equals QrCode.encode(link)
        // TODO: 3. The link field and the QR carry the same text
    }

    @Test
    fun `Copy link copies and says so`() = webTest {
        // TODO: 1. Open Share with a fake clipboard that succeeds
        // TODO: 2. Click Copy link: the clipboard holds the link, the button reads `Copied!`, the status line says `Link copied.`, and the dialog stays open
        // TODO: 3. board_share_link_copied {board_size, start_known, via: button} fires
        // TODO: 4. Press it from the keyboard (a click with detail 0): via is enter_key
    }

    @Test
    fun `a blocked clipboard selects the link and tells the player`() = webTest {
        // TODO: 1. Open Share with a clipboard that refuses
        // TODO: 2. Click Copy link
        // TODO: 3. The fallback is tried first
        // TODO: 4. Then the link field is focused with the link selected
        // TODO: 5. The status line says `Couldn't copy the link - it's selected, so copy it yourself.`
        // TODO: 6. No board_share_link_copied fires
    }

    @Test
    fun `Close and Escape cancel the dialog and focus returns to Share`() = webTest {
        // TODO: 1. Open Share and press Close: dialog_cancelled {dialog: share, via: button} fires and focus is back on Share
        // TODO: 2. Open it again and press Escape: via is escape
        // TODO: 3. The game is unchanged
    }

    @Test
    fun `key 7 opens Share in keyboard mode only, including after a win and on the solution screen`() = webTest {
        // TODO: 1. In keyboard mode, 7 opens the dialog and Escape puts focus back on Share
        // TODO: 2. In mouse mode, 7 does nothing
        // TODO: 3. After a win, 7 opens it
        // TODO: 4. On the solution screen, 7 opens it
        // TODO: 5. The toolbar lists [ New ] [ Save ] [ Load ] [ Labels ] [ Share ] with 7 shown on Share
    }
}
