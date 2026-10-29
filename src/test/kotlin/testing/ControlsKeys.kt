package testing

import view.tui.Direction
import view.tui.TerminalEvent

/** 6, →, Enter: the Controls dialog marks the other mode and OKs it (GH-170) - a mode switch from
 *  either mode. */
val switchModeKeys: List<TerminalEvent> =
    listOf(TerminalEvent.KeyPress('6'), TerminalEvent.Arrow(Direction.RIGHT), TerminalEvent.Enter)
