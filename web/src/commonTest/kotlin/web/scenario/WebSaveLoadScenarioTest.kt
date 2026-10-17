package web.scenario

import kotlin.test.Test

/** GH-110 scenarios: Save and Load in the browser, kept in the in-memory store. Same setup as
 *  [WebStartupScenarioTest]; a "reload" is a new `WebGame` over the same store. */
class WebSaveLoadScenarioTest {

    @Test
    fun `Save prefills the current save name and warns before overwriting`() {
        // WU5
        // TODO: 1. Load "foo"; press Save: prefilled "foo", warning "'foo' already exists - it will be overwritten."
        // TODO: 2. Confirm: the status is "Saved as 'foo'."
        // TODO: 3. screen_save_dialog {opened_from: toolbar, prefilled: true} fired.
    }

    @Test
    fun `Save rejects whitespace, slash and dot-dot names, and a blank name is Cancel`() {
        // WU5 (as the terminal: the dialog closes; the message goes to the status line, D4)
        // TODO: 1. Save as "a b": nothing stored; status "'a b' isn't a usable save name (no spaces) - not saved."
        // TODO: 2. Save as "a/b" and "..": nothing stored; status "Could not save as '…': Save name must not be
        //          blank, contain a path separator, or contain '..'".
        // TODO: 3. Save with a blank name: nothing stored, no status, as Cancel.
    }

    @Test
    fun `Save Cancel stores nothing`() {
        // WU5
        // TODO: 1. Press Save, Cancel: the store is unchanged.
        // TODO: 2. dialog_cancelled {dialog: save} fired.
    }

    @Test
    fun `Load lists every save with its size and marks unsolvable ones`() {
        // WU5
        // TODO: 1. Seed "even" (4x4) and "odd" (3x3, unsolvable); press Load.
        // TODO: 2. The listing shows "even (4x4)" and "odd (3x3)" with the unsolvable mark.
        // TODO: 3. screen_load_dialog {save_file_count: 2} fired.
    }

    @Test
    fun `Load with no saves says so`() {
        // WU5
        // TODO: 1. Empty store; press Load: the Load dialog says "No saves found."
    }

    @Test
    fun `loading an unsolvable save - New game opens the size picker`() {
        // WU5
        // TODO: 1. Load "odd"; choose New game.
        // TODO: 2. The size picker opens; screen_size_dialog {opened_from: unsolvable_save} fired.
    }

    @Test
    fun `Mordie's data round-trips through a browser save`() {
        // WU5
        // TODO: 1. Play with Mordie found at N=11; save as "m"; start a new game.
        // TODO: 2. Load "m": Mordie's spot shows the same Found line, and no search restarts.
    }

    @Test
    fun `saves survive a reload`() {
        // WU5
        // TODO: 1. Save "foo".
        // TODO: 2. A new WebGame over the same store: the startup restore offers "foo".
        // TODO: 3. Yes: the same board and Moves.
    }
}
