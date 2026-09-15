package view.tui

/** The four directions an arrow key can decode to (GH-18's keyboard TUI). */
enum class Direction { UP, DOWN, LEFT, RIGHT }

/** One decoded terminal input event (GH-3's mouse TUI, GH-18's keyboard TUI), produced by
 *  [TerminalInputParser]. */
sealed class TerminalEvent {
    data class MouseClick(val x: Int, val y: Int) : TerminalEvent()
    data class KeyPress(val char: Char) : TerminalEvent()
    data class Arrow(val direction: Direction) : TerminalEvent()
    /** F5/F6/F7/F8, [n] holding the function-key number (5/6/7/8) rather than a separate case
     *  each, since keyboard mode only ever needs these four. */
    data class FunctionKey(val n: Int) : TerminalEvent()
    object Tab : TerminalEvent()
    object BackTab : TerminalEvent()
    object Backspace : TerminalEvent()
    object Enter : TerminalEvent()
    object Escape : TerminalEvent()
    object Resize : TerminalEvent()
    object EndOfInput : TerminalEvent()
}
