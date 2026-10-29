package view.tui

import viewmodel.KeyboardLayout
import viewmodel.ShiftKey

/** What a board key does in keyboard mode (GH-170): move the selected tile, or shift its row or
 *  column, tagged with the [ShiftKey] that made it - `null` for a left-handed W/A/S/D shift,
 *  which `board_solved.shift_keys` leaves out. */
internal sealed class BoardKey {
    data class Select(val direction: Direction) : BoardKey()
    data class Shift(val direction: Direction, val key: ShiftKey?) : BoardKey()
}

/**
 * Keyboard mode's key scheme (ADR-0003), per [KeyboardLayout]. Right-handed: W/A/S/D select,
 * H/L and ←/→ shift the row, K/J and ↑/↓ the column. Left-handed: H/J/K/L and the arrows select,
 * A/D shift the row, W/S the column. Letters in either case. The solution screen's keys and every
 * hint text live here too, so the keys and what the screen says about them can't drift apart.
 */
internal object BoardKeys {

    private val WASD = mapOf('w' to Direction.UP, 's' to Direction.DOWN, 'a' to Direction.LEFT, 'd' to Direction.RIGHT)
    private val HJKL = mapOf('h' to Direction.LEFT, 'j' to Direction.DOWN, 'k' to Direction.UP, 'l' to Direction.RIGHT)

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
    fun keyList(layout: KeyboardLayout) = when (layout) {
        KeyboardLayout.RIGHT_HANDED -> listOf("Select tile    W A S D", "Shift row      ← →  H L", "Shift column   ↑ ↓  K J")
        KeyboardLayout.LEFT_HANDED -> listOf("Select tile    ← ↑ ↓ →  H J K L", "Shift row      A D", "Shift column   W S")
    } + COMMON_KEYS

    private val COMMON_KEYS = listOf(
        "New 1 · Save 2 · Load 3 · Labels 4",
        "Give Up 5 · Controls 6 · Exit Esc",
        "Solution: ← → A D H L Prev/Next ·",
        "  Space/Enter Next · R Restart ·",
        "  ↑ ↓ W S K J scroll",
    )

    fun boardKey(event: TerminalEvent, layout: KeyboardLayout): BoardKey? {
        val arrow = (event as? TerminalEvent.Arrow)?.direction
        val letter = (event as? TerminalEvent.KeyPress)?.char?.lowercaseChar()
        return when (layout) {
            KeyboardLayout.RIGHT_HANDED -> arrow?.let { BoardKey.Shift(it, ShiftKey.ARROW) }
                ?: WASD[letter]?.let { BoardKey.Select(it) }
                ?: HJKL[letter]?.let { BoardKey.Shift(it, ShiftKey.HJKL) }
            KeyboardLayout.LEFT_HANDED -> (arrow ?: HJKL[letter])?.let { BoardKey.Select(it) }
                ?: WASD[letter]?.let { BoardKey.Shift(it, null) }
        }
    }

    /** The solution screen: ←/→, A/D and H/L Prev/Next, Space and Enter Next, R Restart,
     *  ↑/↓, W/S and K/J scroll the steps. */
    fun solutionAction(event: TerminalEvent): InputAction {
        val letter = (event as? TerminalEvent.KeyPress)?.char?.lowercaseChar()
        val direction = (event as? TerminalEvent.Arrow)?.direction ?: WASD[letter] ?: HJKL[letter]
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
