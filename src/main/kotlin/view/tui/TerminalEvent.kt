package view.tui

/** The shared [viewmodel.Direction], under the name the terminal's call sites use (GH-171). */
typealias Direction = viewmodel.Direction

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
