package view.tui

import viewmodel.BoardKey
import viewmodel.KeyScheme
import viewmodel.KeyboardLayout
import viewmodel.SolutionKey

/**
 * The terminal's side of the shared [KeyScheme]: it turns a [TerminalEvent] into its keys, and holds
 * the terminal's hint texts.
 */
internal object BoardKeys {

    /** Left of the board: the left hand's keys. */
    fun leftHint(layout: KeyboardLayout) = when (layout) {
        KeyboardLayout.RIGHT_HANDED -> listOf("W/A/S/D select")
        KeyboardLayout.LEFT_HANDED -> listOf("A/D row", "W/S column")
    }

    /** Right of the board: the right hand's keys. */
    fun rightHint(layout: KeyboardLayout) = when (layout) {
        KeyboardLayout.RIGHT_HANDED -> listOf("←/→ H/L row", "↑/↓ K/J column")
        KeyboardLayout.LEFT_HANDED -> listOf("←↑↓→ H/J/K/L select")
    }

    val SOLUTION_HINT = listOf("←/→ A/D H/L  step", "Space/Enter  next", "R  restart", "↑/↓ W/S K/J  scroll")

    /** Under the toolbar. Must fit 80 columns, the fallback terminal width: [Canvas] clips, it
     *  doesn't wrap. */
    const val CONTROLS_HINT = "1 New · 2 Save · 3 Load · 4 Labels · 5 Give Up · 6 Controls · Esc Exit"

    /** The Controls dialog's key list (GH-170), within its 44-column interior. */
    fun keyList(layout: KeyboardLayout) = KeyScheme.boardKeyLines(layout) + COMMON_KEYS

    private val COMMON_KEYS = listOf(
        "New 1 · Save 2 · Load 3 · Labels 4",
        "Give Up 5 · Controls 6 · Exit Esc",
    ) + KeyScheme.SOLUTION_KEY_LINES

    fun boardKey(event: TerminalEvent, layout: KeyboardLayout): BoardKey? =
        KeyScheme.boardKey(event.letter, event.arrow, layout)

    /** The solution screen's keys, as the terminal acts on them. */
    fun solutionAction(event: TerminalEvent): InputAction =
        when (KeyScheme.solutionKey(event.letter, event.arrow, enter = event == TerminalEvent.Enter)) {
            SolutionKey.PREV -> InputAction.Activate(HitTarget.ReplayPrevious)
            SolutionKey.NEXT -> InputAction.Activate(HitTarget.ReplayNext)
            SolutionKey.RESTART -> InputAction.Activate(HitTarget.ReplayRestart)
            SolutionKey.SCROLL_UP -> InputAction.Scroll(-1)
            SolutionKey.SCROLL_DOWN -> InputAction.Scroll(1)
            null -> InputAction.None
        }

    private val TerminalEvent.letter get() = (this as? TerminalEvent.KeyPress)?.char
    private val TerminalEvent.arrow get() = (this as? TerminalEvent.Arrow)?.direction
}
