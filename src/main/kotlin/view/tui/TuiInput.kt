package view.tui

/**
 * What [TuiView] does in response to one [TerminalEvent], as decided by whichever [TuiInput] is
 * active - the single vocabulary both the board loop and every dialog loop consume, so neither
 * branches on a raw [TerminalEvent] itself. [Activate] carries the same [HitTarget] a mouse click
 * would resolve to, whether it actually came from a click ([MouseInput]) or a keyboard
 * confirm on the currently-focused control (GH-18's `KeyboardInput`).
 */
sealed class InputAction {
    data class Activate(val target: HitTarget) : InputAction()
    data class TextChar(val char: Char) : InputAction()
    object EraseChar : InputAction()
    object Submit : InputAction()
    object Cancel : InputAction()
    /** Redraw without otherwise acting - a keyboard cursor move (GH-18) is the only source of
     *  this today; a mouse click never produces it (see [MouseInput]). */
    object Redraw : InputAction()
    object Quit : InputAction()
    object None : InputAction()
}

/**
 * The per-mode input strategy [TuiView] delegates to (GH-18): translating a raw [TerminalEvent]
 * into an [InputAction], and decorating the [ScreenState]/[Dialog] each repaint draws with
 * whatever presentation the mode owns (a keyboard cursor, focus highlight, toolbar shortcut
 * labels). [MouseInput] is today's mouse behaviour, extracted verbatim; GH-18's `KeyboardInput`
 * is the other implementation.
 */
interface TuiInput {
    /** Runs once, right after [Terminal.enterRawMode] - mouse reporting on for [MouseInput],
     *  left off for a keyboard-only mode. */
    fun prepare(terminal: Terminal)

    /** Translates one event read while the board screen owns input. [layout] is the geometry
     *  the frame just drawn was built against - same draw/hit-test coupling every layout class
     *  in this package already keeps. */
    fun onBoardEvent(event: TerminalEvent, layout: BoardLayout): InputAction

    /** Translates one event read while a dialog's modal loop owns input. */
    fun onDialogEvent(event: TerminalEvent, dialog: Dialog, layout: DialogLayout): InputAction

    /** Decorates a freshly-built board [ScreenState] with this mode's presentation (cursor,
     *  controls hint, toolbar shortcut labels) before it's rendered and laid out. Identity for
     *  [MouseInput]. */
    fun decorateBoard(state: ScreenState): ScreenState

    /** Decorates a freshly-built [Dialog] with this mode's focus highlight before it's rendered
     *  and laid out. Identity for [MouseInput]. */
    fun decorateDialog(dialog: Dialog): Dialog
}
