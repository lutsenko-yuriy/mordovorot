package view.tui

import kotlin.test.Test

/**
 * Covers GH-18's keyboard-driven board interaction loop: `TuiView` dispatching arrow-key cursor
 * movement and Enter/Space activation on the perimeter ring, in `--keyboard` mode. Mirrors
 * `TuiViewBoardTest` (GH-3's mouse equivalent) but driven via keyboard events - no clicks.
 */
class TuiViewKeyboardBoardTest {

    @Test
    fun `an arrow-key walk around the ring followed by Enter calls the expected shift`() {
        // TODO: 1. Build a FakeTuiPresenter/FakeTerminal pair with the view in keyboard mode.
        // TODO: 2. Script Arrow-key events walking the cursor to a known ring position (e.g. LEFT[1]).
        // TODO: 3. Script an Enter event.
        // TODO: 4. Play the view, then verify presenter.calls contains "shiftLeft(1)".
    }

    @Test
    fun `Space also activates the highlighted arrow, same as Enter`() {
        // TODO: 1. Same cursor walk as above.
        // TODO: 2. Script a Space event instead of Enter.
        // TODO: 3. Play the view, then verify the matching shift call happened.
    }

    @Test
    fun `F5, F6, F7 open the Save, Load, and Exit dialogs regardless of cursor position`() {
        // TODO: 1. Move the cursor to an arbitrary ring position first.
        // TODO: 2. In separate runs, script an F5, F6, and F7 event.
        // TODO: 3. Verify the last frame contains "Save game", "Load game", and
        //          "Save before quitting?" respectively.
    }

    @Test
    fun `the board repaints after every cursor move`() {
        // TODO: 1. Script two Arrow-key events with no activation.
        // TODO: 2. Play the view.
        // TODO: 3. Verify terminal.frames.size == 3 (one initial paint plus one per move).
    }

    @Test
    fun `play enters raw mode without enabling mouse reporting, and restores on the way out`() {
        // TODO: 1. Build a keyboard-mode view with no scripted events (immediate EOF).
        // TODO: 2. Play the view.
        // TODO: 3. Verify terminal.rawModeEntered and terminal.restored are true, and
        //          terminal.mouseReportingEnabled is false.
    }

    @Test
    fun `EndOfInput from the terminal ends the loop cleanly`() {
        // TODO: 1. Build a keyboard-mode view with no scripted events.
        // TODO: 2. Play the view.
        // TODO: 3. Verify presenter.calls contains no shift calls.
    }
}
