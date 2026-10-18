package view.tui

import InputMode
import board_model.BoardSize
import viewmodel.GiveUpButton
import viewmodel.ShiftKey

/** Bottom-left controls reminder (GH-18/GH-30/GH-44) - F5/F6/F9/F7/Esc stay live even when
 *  the selection keys don't. Must fit 80 chars, the default/fallback terminal width
 *  ([ScreenRenderer.Canvas.put] has no wrap or ellipsis, unlike dialog content - audit findings
 *  on PR #40 and PR #53). The board keys have their own side hints (GH-101). */
private const val CONTROLS_HINT = "F5/F6/F9 Save/Load/New · F8 Give Up · F7 Mouse · Esc"

private val SELECTION_HINT = listOf("W/S  row", "A/D  column")
private val SHIFT_HINT = listOf("←/→ H/L  shift row", "↑/↓ K/J  shift column")
private val SOLUTION_HINT = listOf("←/→ A/D  step", "Space/Enter  next", "R  restart", "W/S  scroll")

private val SELECT_KEYS = mapOf('w' to Direction.UP, 's' to Direction.DOWN, 'a' to Direction.LEFT, 'd' to Direction.RIGHT)
private val HJKL_KEYS = mapOf('h' to Direction.LEFT, 'j' to Direction.DOWN, 'k' to Direction.UP, 'l' to Direction.RIGHT)

/**
 * GH-18's keyboard-driven [TuiInput] - the only stateful one, working the session's shared
 * [KeyboardSelection] and owning the dialog's focus index into its [DialogFocus] ring.
 *
 * Board (GH-101): W/A/S/D select a row or column, H/J/K/L and the arrow keys shift it (see
 * [LineSelection]), letters in either case; F5/F6 open Save/Load, F9 (GH-44 WU3) opens the
 * New-game size picker, F7 (GH-30) requests a switch to mouse mode, and Escape opens Exit - all
 * staying live even when solved (the selection and shift keys go inert instead, the highlight
 * stops rendering). F8 (GH-126) is the Give Up row's button. On the solution screen, the
 * left/right arrows or A/D are Prev/Next (Space and Enter Next too), R is Restart, and W/S or
 * the up/down arrows scroll the steps. Escape replaces a function key for Exit - board and dialog events are
 * mutually exclusive, so it can't collide with Escape's dialog-level Cancel.
 *
 * Dialog: Tab/Down/Right advance the focus ring, Shift-Tab/Up/Left go back, Enter activates
 * whatever's focused (submit, select a row, or click a button), Escape cancels, typing only ever
 * edits the text field. Space is always text inside a dialog, never an activator.
 */
class KeyboardInput(private val selection: KeyboardSelection) : TuiInput {

    override val switchTrigger = "shortcut"

    private var squareSide = BoardSize.DEFAULT
    private var arrowsEnabled = true
    private var giveUpButton = GiveUpButton.HIDDEN
    private var onSolutionScreen = false

    /** Reset to `0` by [onDialogOpened] at the start of every modal loop, and defensively
     *  whenever [decorateBoard] sees no dialog. */
    private var dialogFocusIndex = 0

    override fun prepare(terminal: Terminal) {
        // Keyboard mode never enables mouse reporting.
    }

    override fun onBoardEvent(event: TerminalEvent, layout: BoardLayout): InputAction {
        val letter = (event as? TerminalEvent.KeyPress)?.char?.lowercaseChar()
        return when {
            // F5/F6/F7/F9/Escape stay live regardless of arrowsEnabled - the toolbar never goes dead.
            event is TerminalEvent.FunctionKey -> functionKeyAction(event.n)
            event == TerminalEvent.Escape -> InputAction.Activate(HitTarget.ToolbarExit)
            event == TerminalEvent.EndOfInput -> InputAction.Quit
            onSolutionScreen -> solutionAction(event, letter)
            !arrowsEnabled -> InputAction.None // solved: nothing to select or shift
            letter in SELECT_KEYS -> {
                selection.current = selection.current.select(SELECT_KEYS.getValue(letter!!), squareSide)
                InputAction.Redraw
            }
            letter in HJKL_KEYS -> shift(HJKL_KEYS.getValue(letter!!), ShiftKey.HJKL)
            event is TerminalEvent.Arrow -> shift(event.direction, ShiftKey.ARROW)
            else -> InputAction.None
        }
    }

    private fun solutionAction(event: TerminalEvent, letter: Char?): InputAction = when {
        letter == 'a' || event == TerminalEvent.Arrow(Direction.LEFT) -> InputAction.Activate(HitTarget.ReplayPrevious)
        letter == 'd' || letter == ' ' || event == TerminalEvent.Arrow(Direction.RIGHT) || event == TerminalEvent.Enter ->
            InputAction.Activate(HitTarget.ReplayNext)
        letter == 'r' -> InputAction.Activate(HitTarget.ReplayRestart)
        letter == 'w' || event == TerminalEvent.Arrow(Direction.UP) -> InputAction.Scroll(-1)
        letter == 's' || event == TerminalEvent.Arrow(Direction.DOWN) -> InputAction.Scroll(1)
        else -> InputAction.None
    }

    private fun shift(direction: Direction, key: ShiftKey): InputAction =
        when (val outcome = selection.current.shift(direction)) {
            is LineSelection.ShiftOutcome.Shift -> InputAction.Shift(outcome.target, key)
            is LineSelection.ShiftOutcome.Switched -> {
                selection.current = outcome.selection
                InputAction.Redraw
            }
        }

    override fun onDialogEvent(event: TerminalEvent, dialog: Dialog, layout: DialogLayout): InputAction {
        val focus = DialogFocus(dialog)
        if (focus.size == 0) {
            return when (event) {
                TerminalEvent.Escape -> InputAction.Cancel
                TerminalEvent.EndOfInput -> InputAction.Quit
                else -> InputAction.None
            }
        }
        val index = dialogFocusIndex.mod(focus.size)
        return when (event) {
            TerminalEvent.Escape -> InputAction.Cancel
            TerminalEvent.EndOfInput -> InputAction.Quit
            TerminalEvent.Tab, TerminalEvent.Arrow(Direction.DOWN), TerminalEvent.Arrow(Direction.RIGHT) ->
                moveFocus(focus, focus.next(index))
            TerminalEvent.BackTab, TerminalEvent.Arrow(Direction.UP), TerminalEvent.Arrow(Direction.LEFT) ->
                moveFocus(focus, focus.previous(index))
            TerminalEvent.Enter -> activateFocus(focus, index)
            is TerminalEvent.KeyPress ->
                if (focus.target(index) == DialogFocusTarget.TextField) InputAction.TextChar(event.char) else InputAction.None
            TerminalEvent.Backspace ->
                if (focus.target(index) == DialogFocusTarget.TextField) InputAction.EraseChar else InputAction.None
            else -> InputAction.None
        }
    }

    override fun decorateBoard(state: ScreenState): ScreenState {
        squareSide = state.squareSide
        arrowsEnabled = state.arrowsEnabled
        giveUpButton = state.giveUpButton
        onSolutionScreen = state.solution != null
        // Kept across new games, loads and solved spells (GH-101), only pulled inside a smaller
        // board - every repaint sees the current side, whichever mode changed it.
        selection.current = selection.current.clampedTo(squareSide)
        if (state.dialog == null) dialogFocusIndex = 0
        return state.copy(
            selection = if (arrowsEnabled) selection.current else null,
            showArrows = false,
            selectionHint = if (onSolutionScreen) SOLUTION_HINT else SELECTION_HINT,
            shiftHint = if (onSolutionScreen) emptyList() else SHIFT_HINT,
            controlsHint = CONTROLS_HINT,
            toolbarShortcuts = true,
            modeButtons = listOf(InputMode.MOUSE),
        )
    }

    override fun onDialogOpened() {
        dialogFocusIndex = 0
    }

    override fun decorateDialog(dialog: Dialog): Dialog {
        val focus = DialogFocus(dialog)
        if (focus.size == 0) return dialog
        val index = dialogFocusIndex.mod(focus.size)
        dialogFocusIndex = index
        return when (val target = focus.target(index)) {
            DialogFocusTarget.TextField -> dialog.copy(textFieldFocused = true)
            is DialogFocusTarget.Button -> dialog.copy(focusedButtonId = target.hitTarget.id)
            is DialogFocusTarget.ListRow, null -> dialog
        }
    }

    private fun moveFocus(focus: DialogFocus, newIndex: Int): InputAction {
        dialogFocusIndex = newIndex
        // A list row's focus doubles as its selection - the same HitTarget.DialogListRow a click
        // would produce. Anything else just moves the highlight; the dialog loop's own
        // per-iteration repaint redraws it, no InputAction.Redraw handling needed there.
        return when (val target = focus.target(newIndex)) {
            is DialogFocusTarget.ListRow -> InputAction.Activate(HitTarget.DialogListRow(target.index))
            else -> InputAction.Redraw
        }
    }

    private fun activateFocus(focus: DialogFocus, index: Int): InputAction = when (val target = focus.target(index)) {
        DialogFocusTarget.TextField -> InputAction.Submit
        is DialogFocusTarget.ListRow -> InputAction.Activate(HitTarget.DialogListRow(target.index))
        is DialogFocusTarget.Button -> InputAction.Activate(target.hitTarget)
        null -> InputAction.None
    }

    private fun functionKeyAction(n: Int): InputAction = when (n) {
        5 -> InputAction.Activate(HitTarget.ToolbarSave)
        6 -> InputAction.Activate(HitTarget.ToolbarLoad)
        7 -> InputAction.Activate(HitTarget.ToolbarMode(InputMode.MOUSE))
        8 -> when (giveUpButton) {
            GiveUpButton.GIVE_UP -> InputAction.Activate(HitTarget.GiveUp)
            GiveUpButton.MORDIES_SOLUTION -> InputAction.Activate(HitTarget.MordiesSolution)
            GiveUpButton.DIMMED, GiveUpButton.HIDDEN -> InputAction.None
        }
        9 -> InputAction.Activate(HitTarget.ToolbarNew)
        else -> InputAction.None
    }
}
