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
 * to offer Keyboard/Console on the toolbar.
 */
class MouseInput : TuiInput {

    override val switchTrigger = "toolbar"

    override fun prepare(terminal: Terminal) {
        terminal.enableMouseReporting()
    }

    override fun onBoardEvent(event: TerminalEvent, layout: BoardLayout): InputAction = when (event) {
        is TerminalEvent.MouseClick -> InputAction.Activate(layout.hitTest(event.x, event.y))
        TerminalEvent.EndOfInput -> InputAction.Quit
        // Keys/Backspace/Enter/Escape/Resize/Arrow/FunctionKey/Tab/BackTab only matter while a
        // dialog's modal loop is reading events directly - the mouse-driven board screen ignores
        // everything but clicks, same as before this extraction.
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
        state.copy(modeButtons = listOf(InputMode.KEYBOARD, InputMode.CONSOLE))

    override fun decorateDialog(dialog: Dialog): Dialog = dialog
}
