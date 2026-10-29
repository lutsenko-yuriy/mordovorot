package view.tui

/**
 * GH-3's mouse-driven [TuiInput], extracted verbatim from [TuiView] for GH-18's input-strategy
 * seam: a click hit-tests against the current layout and becomes [InputAction.Activate] whatever
 * it resolves to (including [HitTarget.Nothing] - the board loop still repaints on dead space,
 * same as before this extraction); the dialog-loop keys map onto the matching [InputAction]
 * one-for-one. The Controls dialog (GH-170), which 6 opens from the keyboard, also takes
 * [DialogKeys]' keys; that's its only state.
 */
class MouseInput : TuiInput {

    private val controlsKeys = DialogKeys()

    override fun prepare(terminal: Terminal) {
        terminal.enableMouseReporting()
    }

    override fun onBoardEvent(event: TerminalEvent, layout: BoardLayout): InputAction = when (event) {
        is TerminalEvent.MouseClick -> InputAction.Activate(layout.hitTest(event.x, event.y))
        is TerminalEvent.Wheel -> InputAction.Scroll(if (event.up) -1 else 1)
        TerminalEvent.EndOfInput -> InputAction.Quit
        // 6 is the one key the toolbar shows here (GH-170): Controls.
        TerminalEvent.KeyPress('6') -> InputAction.Activate(HitTarget.ToolbarControls, shortcut = true)
        // Other keys only matter while a dialog's modal loop is reading events directly - the
        // mouse-driven board screen acts only on clicks, wheel notches and 6 (GH-126).
        else -> InputAction.None
    }

    override fun onDialogEvent(event: TerminalEvent, dialog: Dialog, layout: DialogLayout): InputAction = when {
        event is TerminalEvent.MouseClick -> InputAction.Activate(layout.hitTest(event.x, event.y))
        dialog.kind == Dialog.Kind.CONTROLS -> controlsKeys.onEvent(event, dialog)
        else -> plainDialogAction(event)
    }

    private fun plainDialogAction(event: TerminalEvent): InputAction = when (event) {
        is TerminalEvent.KeyPress -> InputAction.TextChar(event.char)
        TerminalEvent.Backspace -> InputAction.EraseChar
        TerminalEvent.Enter -> InputAction.Submit
        TerminalEvent.Escape -> InputAction.Cancel
        TerminalEvent.EndOfInput -> InputAction.Quit
        else -> InputAction.None
    }

    override fun onDialogOpened() = controlsKeys.onOpened()

    override fun decorateBoard(state: ScreenState): ScreenState = state

    override fun decorateDialog(dialog: Dialog): Dialog =
        if (dialog.kind == Dialog.Kind.CONTROLS) controlsKeys.decorate(dialog) else dialog
}
