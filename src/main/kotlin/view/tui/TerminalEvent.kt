package view.tui

/** One decoded terminal input event (GH-3's mouse TUI), produced by [TerminalInputParser]. */
sealed class TerminalEvent {
    data class MouseClick(val x: Int, val y: Int) : TerminalEvent()
    data class KeyPress(val char: Char) : TerminalEvent()
    object Backspace : TerminalEvent()
    object Enter : TerminalEvent()
    object Escape : TerminalEvent()
    object Resize : TerminalEvent()
    object EndOfInput : TerminalEvent()
}
