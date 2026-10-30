package web

import viewmodel.BoardKey
import viewmodel.Direction
import viewmodel.KeyScheme
import viewmodel.KeyboardLayout
import viewmodel.SolutionKey

/** The toolbar's buttons and their digits (ADR-0003). */
enum class ToolbarButton(val digit: Int) { NEW(1), SAVE(2), LOAD(3), LABELS(4), GIVE_UP_ROW(5), CONTROLS(6) }

/** A key pressed outside a dialog. [modified] is with Ctrl, Alt or Meta, or part of an IME composition. */
data class KeyPress(val key: String, val modified: Boolean = false, val repeat: Boolean = false)

/** What a key does on the page (GH-171). */
sealed interface PageKey {
    data class Digit(val button: ToolbarButton) : PageKey
    data class Board(val key: BoardKey) : PageKey
    data class Solution(val key: SolutionKey) : PageKey
    data object ReadBoard : PageKey
}

internal object WebKeys {

    private val ARROWS = mapOf("ArrowUp" to Direction.UP, "ArrowDown" to Direction.DOWN, "ArrowLeft" to Direction.LEFT, "ArrowRight" to Direction.RIGHT)

    /** [onBoard]: the board is in play and has focus; [onSolution]: the solution screen's has. A held
     *  digit, Space or Enter is one press; a held board or solution direction repeats, as in the terminal. */
    fun pageKey(press: KeyPress, mode: PlayMode, controlsShown: Boolean, layout: KeyboardLayout = KeyboardLayout.DEFAULT, onBoard: Boolean = false, onSolution: Boolean = false): PageKey? {
        if (press.modified) return null
        val keyboard = mode == PlayMode.KEYBOARD
        if (onBoard && keyboard) {
            KeyScheme.boardKey(press.key.singleOrNull(), ARROWS[press.key], layout)?.let { return PageKey.Board(it) }
        }
        val enter = press.key == "Enter"
        if (onSolution && keyboard && !(press.repeat && (enter || press.key == " "))) {
            KeyScheme.solutionKey(press.key.singleOrNull(), ARROWS[press.key], enter)?.let { return PageKey.Solution(it) }
        }
        if (press.repeat) return null
        if (press.key == "0" && keyboard) return PageKey.ReadBoard
        return ToolbarButton.entries.firstOrNull { it.digit.toString() == press.key && live(it, mode, controlsShown) }?.let { PageKey.Digit(it) }
    }

    /** Whether [button]'s digit works, and so shows: in keyboard mode all do, in mouse mode only Controls'. */
    fun live(button: ToolbarButton, mode: PlayMode, controlsShown: Boolean): Boolean =
        if (button == ToolbarButton.CONTROLS) controlsShown else mode == PlayMode.KEYBOARD
}
