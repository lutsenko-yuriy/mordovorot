package view.tui

import viewmodel.ShiftKey

/** What a board key does in keyboard mode (GH-170): move the selected tile, or shift its row or
 *  column, tagged with the [ShiftKey] that made it. */
sealed class BoardKey {
    data class Select(val direction: Direction) : BoardKey()
    data class Shift(val direction: Direction, val key: ShiftKey) : BoardKey()
}

/**
 * Keyboard mode's key scheme (ADR-0003), right-handed: W/A/S/D select, H/L and ←/→ shift the
 * row, K/J and ↑/↓ the column; letters in either case. The solution screen's keys and every
 * hint text live here too, so the keys and what the screen says about them can't drift apart.
 */
internal object BoardKeys {

    private val SELECT = mapOf('w' to Direction.UP, 's' to Direction.DOWN, 'a' to Direction.LEFT, 'd' to Direction.RIGHT)
    private val HJKL = mapOf('h' to Direction.LEFT, 'j' to Direction.DOWN, 'k' to Direction.UP, 'l' to Direction.RIGHT)

    /** Left of the board. */
    val SELECTION_HINT = listOf("W/A/S/D select")

    /** Right of the board. */
    val SHIFT_HINT = listOf("←/→ H/L row", "↑/↓ K/J column")

    val SOLUTION_HINT = listOf("←/→ A/D H/L  step", "Space/Enter  next", "R  restart", "↑/↓ W/S K/J  scroll")

    /** Under the toolbar. Must fit 80 columns, the fallback terminal width: [Canvas] clips, it
     *  doesn't wrap. 6 is the mode switch until GH-170 WU2's Controls dialog. */
    const val CONTROLS_HINT = "1 New · 2 Save · 3 Load · 4 Labels · 5 Give Up · 6 Mouse · Esc Exit"

    fun boardKey(event: TerminalEvent): BoardKey? {
        if (event is TerminalEvent.Arrow) return BoardKey.Shift(event.direction, ShiftKey.ARROW)
        val letter = (event as? TerminalEvent.KeyPress)?.char?.lowercaseChar() ?: return null
        SELECT[letter]?.let { return BoardKey.Select(it) }
        return HJKL[letter]?.let { BoardKey.Shift(it, ShiftKey.HJKL) }
    }

    /** The solution screen: ←/→, A/D and H/L Prev/Next, Space and Enter Next, R Restart,
     *  ↑/↓, W/S and K/J scroll the steps. */
    fun solutionAction(event: TerminalEvent): InputAction {
        val letter = (event as? TerminalEvent.KeyPress)?.char?.lowercaseChar()
        val direction = (event as? TerminalEvent.Arrow)?.direction ?: SELECT[letter] ?: HJKL[letter]
        return when {
            direction == Direction.LEFT -> InputAction.Activate(HitTarget.ReplayPrevious)
            direction == Direction.RIGHT || letter == ' ' || event == TerminalEvent.Enter -> InputAction.Activate(HitTarget.ReplayNext)
            direction == Direction.UP -> InputAction.Scroll(-1)
            direction == Direction.DOWN -> InputAction.Scroll(1)
            letter == 'r' -> InputAction.Activate(HitTarget.ReplayRestart)
            else -> InputAction.None
        }
    }
}
