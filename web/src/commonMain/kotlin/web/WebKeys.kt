package web

import viewmodel.BoardKey
import viewmodel.Direction
import viewmodel.KeyScheme
import viewmodel.KeyboardLayout

/** The toolbar's buttons and their digits (ADR-0003). */
enum class ToolbarButton(val digit: Int) { NEW(1), SAVE(2), LOAD(3), LABELS(4), GIVE_UP_ROW(5), CONTROLS(6) }

/** A key pressed outside a dialog. [modified] is with Ctrl, Alt or Meta, or part of an IME composition. */
data class KeyPress(val key: String, val modified: Boolean = false, val repeat: Boolean = false)

/** What a key does on the page (GH-171). */
sealed interface PageKey {
    data class Digit(val button: ToolbarButton) : PageKey
    data class Board(val key: BoardKey) : PageKey
}

internal object WebKeys {

    private val ARROWS = mapOf("ArrowUp" to Direction.UP, "ArrowDown" to Direction.DOWN, "ArrowLeft" to Direction.LEFT, "ArrowRight" to Direction.RIGHT)

    /** [onBoard]: the board is in play and has focus. A held digit is one press; a held board key repeats, as in the terminal. */
    fun pageKey(press: KeyPress, mode: PlayMode, controlsShown: Boolean, layout: KeyboardLayout = KeyboardLayout.DEFAULT, onBoard: Boolean = false): PageKey? {
        if (press.modified) return null
        if (onBoard && mode == PlayMode.KEYBOARD) {
            KeyScheme.boardKey(press.key.singleOrNull(), ARROWS[press.key], layout)?.let { return PageKey.Board(it) }
        }
        if (press.repeat) return null
        return ToolbarButton.entries.firstOrNull { it.digit.toString() == press.key && live(it, mode, controlsShown) }?.let { PageKey.Digit(it) }
    }

    /** Whether [button]'s digit works, and so shows: in keyboard mode all do, in mouse mode only Controls'. */
    fun live(button: ToolbarButton, mode: PlayMode, controlsShown: Boolean): Boolean =
        if (button == ToolbarButton.CONTROLS) controlsShown else mode == PlayMode.KEYBOARD
}
