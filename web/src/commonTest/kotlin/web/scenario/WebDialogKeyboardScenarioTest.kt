package web.scenario

import kotlin.test.Test

/** GH-163 scenarios: the web's dialogs from the keyboard - focus on open, Escape, focus on close -
 *  through [WebHarness], reading the frame's `focus`. Same setup as [WebStartupScenarioTest]. */
class WebDialogKeyboardScenarioTest {

    // Focus on open

    @Test
    fun `the size picker opens focused on the current board's size`() = webTest {
        // TODO: 1. Start on a 4x4 board; New - focus is on the 4x4 choice.
        // TODO: 2. Pick 5x5, New again - focus is on 5x5.
    }

    @Test
    fun `Save opens focused on the name field`() = webTest {
        // TODO: 1. Save - focus is on the name field.
    }

    @Test
    fun `Load opens focused on the first save, or on Cancel when there are none`() = webTest {
        // TODO: 1. No saves: Load - "No saves found.", focus is on Cancel.
        // TODO: 2. Cancel, seed two saves, Load - focus is on the first save in the list.
    }

    @Test
    fun `Can't be solved opens focused on Repair & load`() = webTest {
        // TODO: 1. Seed an unsolvable 3x3 save; toolbar Load, pick it - Can't be solved opens, focus is on Repair & load.
    }

    @Test
    fun `Tile labels opens focused on the marked scheme and follows a pick`() = webTest {
        // TODO: 1. Labels - focus is on Numbers (marked).
        // TODO: 2. Pick Letters - focus is on Letters.
    }

    @Test
    fun `Give Up opens focused on No`() = webTest {
        // TODO: 1. Mordie reveals his solution; Give Up - focus is on No.
    }

    // Escape

    @Test
    fun `Escape cancels each dialog like its Cancel button`() = webTest {
        // TODO: 1. New -> Escape: closed, board unchanged; dialog_cancelled {size, via: escape}.
        // TODO: 2. Save -> Escape: nothing saved; dialog_cancelled {save, via: escape}.
        // TODO: 3. Load -> Escape: nothing loaded; dialog_cancelled {load, via: escape}.
        // TODO: 4. Labels -> pick Letters -> Escape: Numbers back on the board; dialog_cancelled {labels, via: escape}.
        // TODO: 5. Give Up -> Escape: game goes on; dialog_cancelled {give_up, via: escape}.
    }

    @Test
    fun `Cancel through the button reports via button`() = webTest {
        // TODO: 1. New -> Cancel, Labels -> Cancel, Give Up -> No - each dialog_cancelled has via: button.
    }

    @Test
    fun `Escape on Restore opens the size picker and counts as new_game`() = webTest {
        // TODO: 1. Seed one save; start - Restore a saved game? is open.
        // TODO: 2. Escape - the size picker opens; startup_restore_decision {decision: new_game}.
    }

    @Test
    fun `Escape does nothing on Can't be solved or with no dialog open`() = webTest {
        // TODO: 1. No dialog: Escape - the frame is unchanged, no event.
        // TODO: 2. Toolbar Load of an unsolvable save: Escape - Can't be solved stays open, no event.
    }

    // Focus on close

    @Test
    fun `closing a toolbar dialog gives focus back to its button`() = webTest {
        // TODO: 1. New -> pick 4x4 - focus is on New.
        // TODO: 2. Save -> Escape - focus is on Save.
        // TODO: 3. Load -> Cancel - focus is on Load.
        // TODO: 4. Labels -> OK - focus is on Labels.
    }

    @Test
    fun `Give Up's No goes back to Give Up, Yes moves on to Next`() = webTest {
        // TODO: 1. Mordie reveals; Give Up -> No - focus is on Give Up.
        // TODO: 2. Give Up -> Yes - the solution screen, focus is on Next.
    }

    @Test
    fun `a dialog that leads to another hands focus to the next one`() = webTest {
        // TODO: 1. Seed one save; start, Cancel Restore - the size picker opens, focus is on 4x4.
        // TODO: 2. Seed an unsolvable save; toolbar Load, pick it - Can't be solved, focus is on Repair & load.
        // TODO: 3. Repair & load - focus is back on Load.
        // TODO: 4. The frame drawn between the two dialogs asks for no focus.
    }

    @Test
    fun `startup dialogs close to the start of the page`() = webTest {
        // TODO: 1. Seed one save; start, Restore -> pick it - focus goes to the start of the page.
        // TODO: 2. Reload with no saves; the size picker -> Cancel - focus goes to the start of the page.
    }
}
