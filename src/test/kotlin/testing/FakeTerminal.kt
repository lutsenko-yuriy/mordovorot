package testing

import view.tui.Terminal
import view.tui.TerminalEvent
import view.tui.TerminalSize

/** A [Terminal] test double: replays a scripted [TerminalEvent] queue and records every
 *  written frame, so `view.tui` classes (WU3+) can be tested with zero real terminal I/O. */
class FakeTerminal(
    private val events: MutableList<TerminalEvent> = mutableListOf(),
    private val terminalSize: TerminalSize = TerminalSize(80, 24),
) : Terminal {

    val frames = mutableListOf<String>()

    var rawModeEntered = false
        private set

    var restored = false
        private set

    var mouseReportingEnabled = false
        private set

    override fun enterRawMode() {
        rawModeEntered = true
    }

    override fun restore() {
        restored = true
    }

    override fun enableMouseReporting() {
        mouseReportingEnabled = true
    }

    override fun write(frame: String) {
        frames += frame
    }

    override fun readEvent(): TerminalEvent =
        if (events.isEmpty()) TerminalEvent.EndOfInput else events.removeAt(0)

    override fun size(): TerminalSize = terminalSize
}
