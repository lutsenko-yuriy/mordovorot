package view.tui

/** The four directions an arrow key can decode to (GH-18's keyboard TUI). */
enum class Direction { UP, DOWN, LEFT, RIGHT }

/** One decoded terminal input event (GH-3's mouse TUI, GH-18's keyboard TUI), produced by
 *  [TerminalInputParser]. */
sealed class TerminalEvent {
    data class MouseClick(val x: Int, val y: Int) : TerminalEvent()
    /** One mouse-wheel notch (GH-126). */
    data class Wheel(val up: Boolean) : TerminalEvent()
    data class KeyPress(val char: Char) : TerminalEvent()
    data class Arrow(val direction: Direction) : TerminalEvent()
    object Tab : TerminalEvent()
    object BackTab : TerminalEvent()
    object Backspace : TerminalEvent()
    object Enter : TerminalEvent()
    object Escape : TerminalEvent()
    object Resize : TerminalEvent()
    object EndOfInput : TerminalEvent()
}
