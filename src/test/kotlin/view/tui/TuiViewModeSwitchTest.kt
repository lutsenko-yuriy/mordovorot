package view.tui

import kotlin.test.Test

/**
 * Covers GH-30's TUI-side mode-switch triggers: the mouse toolbar's Keyboard/Console buttons
 * and the keyboard mode's F7/F8 shortcuts, both reaching an injected `ModeSwitcher` with the
 * right target mode and trigger, plus the exception unwinding cleanly out of `TuiView.play()`.
 */
class TuiViewModeSwitchTest {

    @Test
    fun `clicking the toolbar's Keyboard button requests a switch to keyboard mode with trigger=toolbar`() {
        // TODO: 1. Build a TuiView in mouse mode with a RecordingModeSwitcher injected.
        // TODO: 2. Click the toolbar's Keyboard button position (from BoardLayout.modeButtons).
        // TODO: 3. Verify switcher.calls contains (target = KEYBOARD, trigger = "toolbar").
    }

    @Test
    fun `clicking the toolbar's Console button requests a switch to console mode with trigger=toolbar`() {
        // TODO: 1. Build a TuiView in mouse mode with a RecordingModeSwitcher injected.
        // TODO: 2. Click the toolbar's Console button position (from BoardLayout.modeButtons).
        // TODO: 3. Verify switcher.calls contains (target = CONSOLE, trigger = "toolbar").
    }

    @Test
    fun `pressing F7 in keyboard mode requests a switch to mouse mode with trigger=shortcut, even when solved`() {
        // TODO: 1. Build a TuiView in keyboard mode (KeyboardInput) with a RecordingModeSwitcher,
        //          on an already-solved FakeTuiPresenter.
        // TODO: 2. Send TerminalEvent.FunctionKey(7).
        // TODO: 3. Verify switcher.calls contains (target = MOUSE, trigger = "shortcut") even
        //          though the board is solved.
    }

    @Test
    fun `pressing F8 in keyboard mode requests a switch to console mode with trigger=shortcut`() {
        // TODO: 1. Build a TuiView in keyboard mode (KeyboardInput) with a RecordingModeSwitcher.
        // TODO: 2. Send TerminalEvent.FunctionKey(8).
        // TODO: 3. Verify switcher.calls contains (target = CONSOLE, trigger = "shortcut").
    }

    @Test
    fun `a mode switch requested from inside an open dialog unwinds cleanly and restores the terminal`() {
        // TODO: 1. Open the Save dialog, then trigger a mode switch (toolbar click reaching the
        //          switcher) while the dialog is open - switcher throws ModeSwitchRequestedException.
        // TODO: 2. Verify the exception propagates out of TuiView.play().
        // TODO: 3. Verify FakeTerminal.restore() was called (via the finally block).
    }
}
