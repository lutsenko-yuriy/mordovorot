package view.tui

import viewmodel.ShiftKey

/**
 * What [TuiView] does in response to one [TerminalEvent], as decided by whichever [TuiInput] is
 * active - the single vocabulary both the board loop and every dialog loop consume, so neither
 * branches on a raw [TerminalEvent] itself. [Activate] carries the same [HitTarget] a mouse click
 * would resolve to, whether it actually came from a click ([MouseInput]) or a keyboard
 * confirm on the currently-focused control (GH-18's `KeyboardInput`).
 */
sealed class InputAction {
    /** [shortcut]: a toolbar key rather than a click (GH-170) - what `opened_from` and
     *  `input_mode_switched.trigger` report. */
    data class Activate(val target: HitTarget, val shortcut: Boolean = false) : InputAction()
    /** A keyboard shift (GH-101), tagged with the [key] that made it - kept apart from
     *  [Activate] so a click or toolbar target never carries one. */
    data class Shift(val target: HitTarget, val key: ShiftKey) : InputAction()
    data class TextChar(val char: Char) : InputAction()
    object EraseChar : InputAction()
    object Submit : InputAction()
    object Cancel : InputAction()
    /** Redraw without otherwise acting - a keyboard selection change (GH-170) is the only
     *  source of this today; a mouse click never produces it (see [MouseInput]). */
    object Redraw : InputAction()
    /** Scrolls the solution screen's step list by [delta] rows (GH-126): a wheel notch, W/S, K/J. */
    data class Scroll(val delta: Int) : InputAction()
    object Quit : InputAction()
    object None : InputAction()
}

/**
 * The per-mode input strategy [TuiView] delegates to (GH-18): translating a raw [TerminalEvent]
 * into an [InputAction], and decorating the [ScreenState]/[Dialog] each repaint draws with
 * whatever presentation the mode owns (a selected tile, focus highlight, toolbar shortcut
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

    /** Called once, right when a new modal dialog loop begins, so a stateful mode (GH-18's
     *  `KeyboardInput`) can reset its per-session state (e.g. dialog focus) at a precise
     *  boundary - including when one dialog opens another with no board repaint in between (the
     *  exit flow's Yes -> Save handoff). Default no-op; [MouseInput] resets its Controls dialog keys (GH-170). */
    fun onDialogOpened() {}

    /** Decorates a freshly-built board [ScreenState] with this mode's presentation (selection,
     *  controls hint, toolbar shortcut labels) before it's rendered and laid out. Identity for
     *  [MouseInput]. */
    fun decorateBoard(state: ScreenState): ScreenState

    /** Decorates a freshly-built [Dialog] with this mode's focus highlight before it's rendered
     *  and laid out. Identity for [MouseInput] outside the Controls dialog (GH-170). */
    fun decorateDialog(dialog: Dialog): Dialog
}
