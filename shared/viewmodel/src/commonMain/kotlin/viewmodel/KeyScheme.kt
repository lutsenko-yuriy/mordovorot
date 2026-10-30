package viewmodel

/** What a board key does in keyboard mode (GH-170): move the selected tile, or shift its row or
 *  column, tagged with the [ShiftKey] that made it - `null` for a left-handed W/A/S/D shift,
 *  which `board_solved.shift_keys` leaves out. */
sealed class BoardKey {
    data class Select(val direction: Direction) : BoardKey()
    data class Shift(val direction: Direction, val key: ShiftKey?) : BoardKey()
}

/** What a key does on the solution screen (GH-126). */
enum class SolutionKey { PREV, NEXT, RESTART, SCROLL_UP, SCROLL_DOWN }

/**
 * Keyboard mode's key scheme (ADR-0003), per [KeyboardLayout], for every client (GH-171).
 * Right-handed: W/A/S/D select, H/L and ←/→ shift the row, K/J and ↑/↓ the column. Left-handed:
 * H/J/K/L and the arrows select, A/D shift the row, W/S the column. Letters in either case.
 */
object KeyScheme {

    private val WASD = mapOf('w' to Direction.UP, 's' to Direction.DOWN, 'a' to Direction.LEFT, 'd' to Direction.RIGHT)
    private val HJKL = mapOf('h' to Direction.LEFT, 'j' to Direction.DOWN, 'k' to Direction.UP, 'l' to Direction.RIGHT)

    fun boardKey(letter: Char?, arrow: Direction?, layout: KeyboardLayout): BoardKey? {
        val lower = letter?.lowercaseChar()
        return when (layout) {
            KeyboardLayout.RIGHT_HANDED -> arrow?.let { BoardKey.Shift(it, ShiftKey.ARROW) }
                ?: WASD[lower]?.let { BoardKey.Select(it) }
                ?: HJKL[lower]?.let { BoardKey.Shift(it, ShiftKey.HJKL) }
            KeyboardLayout.LEFT_HANDED -> (arrow ?: HJKL[lower])?.let { BoardKey.Select(it) }
                ?: WASD[lower]?.let { BoardKey.Shift(it, null) }
        }
    }

    /** ←/→, A/D and H/L Prev/Next, Space and Enter Next, R Restart, ↑/↓, W/S and K/J scroll the steps. */
    fun solutionKey(letter: Char?, arrow: Direction?, enter: Boolean): SolutionKey? {
        val lower = letter?.lowercaseChar()
        val direction = arrow ?: WASD[lower] ?: HJKL[lower]
        return when {
            direction == Direction.LEFT -> SolutionKey.PREV
            direction == Direction.RIGHT || lower == ' ' || enter -> SolutionKey.NEXT
            direction == Direction.UP -> SolutionKey.SCROLL_UP
            direction == Direction.DOWN -> SolutionKey.SCROLL_DOWN
            lower == 'r' -> SolutionKey.RESTART
            else -> null
        }
    }

    /** The solution screen's lines of the Controls dialog's key list. */
    val SOLUTION_KEY_LINES = listOf("Solution: ← → A D H L Prev/Next ·", "  Space/Enter Next · R Restart ·", "  ↑ ↓ W S K J scroll")

    /** The first lines of the Controls dialog's key list. */
    fun boardKeyLines(layout: KeyboardLayout) = when (layout) {
        KeyboardLayout.RIGHT_HANDED -> listOf("Select tile    W A S D", "Shift row      ← →  H L", "Shift column   ↑ ↓  K J")
        KeyboardLayout.LEFT_HANDED -> listOf("Select tile    ← ↑ ↓ →  H J K L", "Shift row      A D", "Shift column   W S")
    }
}
