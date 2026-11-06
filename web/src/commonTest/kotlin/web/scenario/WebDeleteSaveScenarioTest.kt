package web.scenario

import kotlin.test.Test

/** GH-251 scenarios: deleting a save from the Load dialog and the startup restore dialog, in the
 *  browser, through [WebHarness]. Each name notes the work unit that turns it green. */
class WebDeleteSaveScenarioTest {

    @Test
    fun `WU2 the trash icon opens a confirmation with focus on Cancel`() = webTest {
        // TODO: 1. Seed saves `foo` and `bar`, start a game, open Load.
        // TODO: 2. Press the trash icon on `foo`.
        // TODO: 3. The dialog is the `Delete 'foo'?` confirmation, with focus on Cancel.
        // TODO: 4. Both saves are still in the store.
    }

    @Test
    fun `WU2 Cancel and Escape in the confirmation keep the save and show the list again`() = webTest {
        // TODO: 1. Open the confirmation for `foo`.
        // TODO: 2. Press Cancel: the Load list is back with both saves and no status message.
        // TODO: 3. Open the confirmation again and press Escape: same result.
        // TODO: 4. The store is untouched.
        // TODO: 5. After each, focus is back on that row's trash button.
        // TODO: 6. screen_load_dialog was reported once for the whole Load session, not again when the list is shown again.
    }

    @Test
    fun `WU2 Delete removes the save, says so and shows the rest`() = webTest {
        // TODO: 1. Seed `foo` and `bar`, open Load, open the confirmation for `foo`.
        // TODO: 2. Press Delete.
        // TODO: 3. The store holds only `bar`, and the Load list shows only `bar (4x4)`.
        // TODO: 4. The status reads `Deleted 'foo'.`
        // TODO: 5. The board and move count are untouched.
    }

    @Test
    fun `WU2 after a Delete focus moves to the next save, or the previous one when the last went`() = webTest {
        // TODO: 1. Seed three saves, open Load, delete the middle one: focus is on the load button of the row that took its place.
        // TODO: 2. Delete the last row: focus is on the load button of the new last row.
    }

    @Test
    fun `WU2 the startup restore dialog deletes too, and another save still restores`() = webTest {
        // TODO: 1. Seed `foo` and `bar` and reload: the `Restore a saved game?` dialog is open.
        // TODO: 2. Delete `foo` from it.
        // TODO: 3. The dialog stays open with only `bar`.
        // TODO: 4. Choose `bar`: the game restores it.
    }

    @Test
    fun `WU2 deleting the last save at startup opens the size picker`() = webTest {
        // TODO: 1. Seed one save and reload: the restore dialog opens.
        // TODO: 2. Delete it.
        // TODO: 3. The restore dialog closes and the board-size picker opens.
        // TODO: 4. The status reads `Deleted 'foo'.`
        // TODO: 5. The startup decision reported is `all_deleted`, followed by screen_size_dialog with opened_from=startup.
        // TODO: 6. Repeat with two saves deleted one after the other: the restore dialog stays after the first, and `all_deleted` is reported only after the second.
    }

    @Test
    fun `WU2 deleting the last save mid-game returns to the game`() = webTest {
        // TODO: 1. Seed one save, start a game, make a shift, open Load.
        // TODO: 2. Delete the only save.
        // TODO: 3. No dialog is open (not the size picker, not an empty Load list).
        // TODO: 4. The board and move count are as before.
        // TODO: 5. The status reads `Deleted 'foo'. No saves left.`
        // TODO: 6. No dialog_cancelled was reported.
        // TODO: 7. Focus is on the `[ Load ]` button.
    }

    @Test
    fun `WU2 deleting the current save clears the Save dialog's name, and another save does not`() = webTest {
        // TODO: 1. Seed `foo` and `bar`, load `foo`.
        // TODO: 2. Delete `bar` from the Load dialog, then open Save: the name field still opens with `foo`.
        // TODO: 3. Open Load again, delete `foo`, then open Save: the name field opens empty.
    }

    @Test
    fun `WU2 corrupted and unsolvable saves can be deleted`() = webTest {
        // TODO: 1. Seed an unsolvable 3x3 save, and put a corrupted entry straight into the store.
        // TODO: 2. Open Load: both are listed (`odd (3x3) unsolvable`, and the corrupted one by name alone) with a trash icon.
        // TODO: 3. Delete each in turn: both are gone from the store and the list.
        // TODO: 4. screen_delete_save_dialog and save_deleted report save_state=unsolvable for the first and save_state=corrupted for the second.
    }

    @Test
    fun `WU2 a save that is already gone, or that storage refuses to delete, leaves the list and the board untouched`() = webTest {
        // TODO: 1. Open the confirmation for `foo`, then remove `foo` from the store behind the page's back.
        // TODO: 2. Press Delete: the status reads `No save named 'foo' - it was already deleted.`
        // TODO: 3. Repeat with a store that refuses the removal: the status reads `Could not delete 'foo'.`
        // TODO: 4. The board is untouched, and save_deleted reports `not_found` and `error`.
        // TODO: 5. After each, the list is shown again and focus is back on that row's trash button.
    }

    @Test
    fun `WU2 deleting a save does not make the game unsaved progress`() = webTest {
        // TODO: 1. Load `foo`, so there is no unsaved progress.
        // TODO: 2. Delete `foo` from Load.
        // TODO: 3. The leave-site prompt still does not ask.
    }

    @Test
    fun `WU2 deleting reports its screen view and event without the save's name, and by key in keyboard mode`() = webTest {
        // TODO: 1. Open Load mid-game, press the trash icon with the mouse: screen_delete_save_dialog is reported with opened_from=toolbar, save_state=ok and save_file_count=2.
        // TODO: 2. Cancel: dialog_cancelled reports dialog=delete_save.
        // TODO: 3. Delete the save: save_deleted reports result=success, trigger=command, was_current_save and remaining_save_count=1.
        // TODO: 4. No reported property contains the save's name.
        // TODO: 5. Repeat in keyboard mode with Enter on the icon: input_method is keyboard.
        // TODO: 6. dialog_cancelled carries via=button for Cancel and via=escape for Escape.
        // TODO: 7. Repeat from the startup restore dialog: save_deleted reports trigger=startup_prompt and screen_delete_save_dialog reports opened_from=startup.
    }
}
