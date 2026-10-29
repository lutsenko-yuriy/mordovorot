package view.tui

import InputMode

/**
 * GH-3's mouse-driven [TuiInput], extracted verbatim from [TuiView] for GH-18's input-strategy
 * seam: a click hit-tests against the current layout and becomes [InputAction.Activate] whatever
 * it resolves to (including [HitTarget.Nothing] - the board loop still repaints on dead space,
 * same as before this extraction); the dialog-loop keys map onto the matching [InputAction]
 * one-for-one. Stateless - a fresh instance behaves identically to any other.
 *
 * [decorateBoard] is no longer a pure identity as of GH-30: it sets [ScreenState.modeButtons]
 * to offer the Keyboard switch on the toolbar.
 */
class MouseInput : TuiInput {

    override fun prepare(terminal: Terminal) {
        terminal.enableMouseReporting()
    }

    override fun onBoardEvent(event: TerminalEvent, layout: BoardLayout): InputAction = when (event) {
        is TerminalEvent.MouseClick -> InputAction.Activate(layout.hitTest(event.x, event.y))
        is TerminalEvent.Wheel -> InputAction.Scroll(if (event.up) -1 else 1)
        TerminalEvent.EndOfInput -> InputAction.Quit
        // 6 is the one key the toolbar shows here (GH-170): the mode switch until WU2's Controls.
        TerminalEvent.KeyPress('6') -> InputAction.Activate(HitTarget.ToolbarMode(InputMode.KEYBOARD), shortcut = true)
        // Other keys only matter while a dialog's modal loop is reading events directly - the
        // mouse-driven board screen acts only on clicks, wheel notches and 6 (GH-126).
        else -> InputAction.None
    }

    override fun onDialogEvent(event: TerminalEvent, dialog: Dialog, layout: DialogLayout): InputAction = when (event) {
        is TerminalEvent.KeyPress -> InputAction.TextChar(event.char)
        TerminalEvent.Backspace -> InputAction.EraseChar
        TerminalEvent.Enter -> InputAction.Submit
        TerminalEvent.Escape -> InputAction.Cancel
        is TerminalEvent.MouseClick -> InputAction.Activate(layout.hitTest(event.x, event.y))
        TerminalEvent.EndOfInput -> InputAction.Quit
        else -> InputAction.None
    }

    override fun decorateBoard(state: ScreenState): ScreenState =
        state.copy(modeButtons = listOf(InputMode.KEYBOARD))

    override fun decorateDialog(dialog: Dialog): Dialog = dialog
}
