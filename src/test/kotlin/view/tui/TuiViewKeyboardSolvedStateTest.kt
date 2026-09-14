package view.tui

import kotlin.test.Test

/**
 * Covers GH-18's keyboard-driven solved-state view: the cursor disappearing and arrow
 * keys/Enter/Space becoming inert once solved, the toolbar (and its F5/F6/F7 shortcuts) staying
 * live, and the transition back to a navigable board when a load restores an unsolved game.
 * Mirrors `TuiViewSolvedStateTest` (GH-3's mouse equivalent).
 */
class TuiViewKeyboardSolvedStateTest {

    @Test
    fun `once solved, no cursor is rendered and arrow keys and Enter are inert`() {
        // TODO: 1. Build a FakeTuiPresenter with solved = true, in keyboard mode.
        // TODO: 2. Script an Arrow event followed by an Enter event.
        // TODO: 3. Play the view, then verify presenter.calls has no shift calls and the last
        //          frame contains "Congratulations ✓".
    }

    @Test
    fun `F5, F6, F7 still open their dialogs after solve`() {
        // TODO: 1. Build a FakeTuiPresenter with solved = true.
        // TODO: 2. In separate runs, script an F5, F6, and F7 event.
        // TODO: 3. Verify each run's last frame contains "Save game", "Load game", and
        //          "Save before quitting?" respectively.
    }

    @Test
    fun `loading an unsolved save from the Congratulations screen restores the cursor and re-enables navigation`() {
        // TODO: 1. Build a presenter that starts solved = true with one save available, and
        //          flips solved = false on loadGame (mirroring TuiViewSolvedStateTest's delegate
        //          pattern).
        // TODO: 2. Script an F6 event, a Down event, a Tab event, an Enter event to load the
        //          unsolved save.
        // TODO: 3. Script an Arrow event walking the cursor to LEFT[0], then an Enter event.
        // TODO: 4. Play the view, then verify presenter.calls contains "loadGame(...)" and
        //          "shiftLeft(0)", and the last frame shows "Mordovorot", not "Congratulations".
    }
}
