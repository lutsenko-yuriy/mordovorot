package web.scenario

import kotlin.test.Test

/** GH-262 scenarios: the New version available notice and the Update flow, through [WebHarness]. */
class WebUpdateScenarioTest {

    @Test
    fun `no notice until a new version is ready`() = webTest {
        // TODO: 1. Start a game on a page where no new version is ready
        // TODO: 2. The footer shows the credit text and no Update link
        // TODO: 3. No update_available event has fired
    }

    @Test
    fun `a version found at load shows the notice`() = webTest {
        // TODO: 1. Start a page with a new version ready at load
        // TODO: 2. The footer reads `v<version> · New version available. Update`, the credit text is gone
        // TODO: 3. update_available {check_trigger: load, current_version: 9.9.9} fires once
    }

    @Test
    fun `returning to the tab finds a new version`() = webTest {
        // TODO: 1. Start a game with nothing ready
        // TODO: 2. Make a new version ready, then return to the tab
        // TODO: 3. The notice appears
        // TODO: 4. update_available {check_trigger: tab_return} fires once
    }

    @Test
    fun `the end of a game finds a new version`() = webTest {
        // TODO: 1. Make a new version ready, then solve a 3x3 game: the notice appears, check_trigger is game_end
        // TODO: 2. Repeat with Give Up -> Yes: the same happens
    }

    @Test
    fun `checks run at the right moments`() = webTest {
        // TODO: 1. A check happens at start, on each tab return, on the solved transition and after Give Up -> Yes
        // TODO: 2. Give Up -> No does not check
    }

    @Test
    fun `Update without unsaved progress reloads at once`() = webTest {
        // TODO: 1. With the notice up and no shifts since a new game, click Update
        // TODO: 2. No dialog opens
        // TODO: 3. apply() is called
        // TODO: 4. update_applied {had_unsaved_progress: false, confirmed: false} fires
    }

    @Test
    fun `Update with unsaved progress asks first`() = webTest {
        // TODO: 1. With the notice up, make 3 shifts, then click Update
        // TODO: 2. The dialog `Update to the new version?` opens with focus on Cancel
        // TODO: 3. screen_update_dialog {board_size, move_count: 3} fires
        // TODO: 4. The board and its moves are untouched
    }

    @Test
    fun `confirming the dialog updates`() = webTest {
        // TODO: 1. Open the dialog with unsaved progress and press Update
        // TODO: 2. apply() is called
        // TODO: 3. update_applied {had_unsaved_progress: true, confirmed: true} fires
        // TODO: 4. The browser's leave-site prompt does not ask a second time
    }

    @Test
    fun `Cancel keeps the game and the notice`() = webTest {
        // TODO: 1. Open the dialog and press Cancel
        // TODO: 2. The dialog closes, focus returns to the Update link, the board and moves are unchanged
        // TODO: 3. dialog_cancelled {dialog: update, via: button} fires
        // TODO: 4. The notice is still shown
    }

    @Test
    fun `Escape in the dialog is Cancel`() = webTest {
        // TODO: 1. Open the dialog and press Escape
        // TODO: 2. It does what Cancel does, with dialog_cancelled {dialog: update, via: escape}
    }

    @Test
    fun `updating leaves the saves alone`() = webTest {
        // TODO: 1. Save a game, then update through the dialog
        // TODO: 2. The save store holds the same content afterwards
    }

    @Test
    fun `Update is ignored while another dialog is open`() = webTest {
        // TODO: 1. Open the Save dialog
        // TODO: 2. Update does nothing, and the page behind the dialog is inert
    }
}
