package view.tui

/** A terminal's viewport size in character cells, used by `BoardLayout` (WU3) to center and
 *  hit-test the board. */
data class TerminalSize(val columns: Int, val rows: Int)
