package testing

import view.tui.Terminal
import view.tui.TerminalEvent
import view.tui.TerminalSize

/** A [Terminal] test double: replays a scripted [TerminalEvent] queue and records every
 *  written frame, so `view.tui` classes (WU3+) can be tested with zero real terminal I/O. */
class FakeTerminal(
    private val events: MutableList<TerminalEvent> = mutableListOf(),
    private val terminalSize: TerminalSize = TerminalSize(80, 24),
    /** Run just before the event at that (0-based) index is read - lets a scenario change the
     *  world between events, e.g. script a search's progress. */
    private val beforeEvent: Map<Int, () -> Unit> = emptyMap(),
) : Terminal {

    private var eventsRead = 0

    val frames = mutableListOf<String>()

    /** Each read's timeout, `null` for a blocking read (GH-126). */
    val timeouts = mutableListOf<Long?>()

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

    /** Scripted [TerminalEvent.Tick]s stand in for timeouts passing, so tests take no real time. */
    override fun readEvent(timeoutMs: Long?): TerminalEvent {
        timeouts += timeoutMs
        beforeEvent[eventsRead++]?.invoke()
        return if (events.isEmpty()) TerminalEvent.EndOfInput else events.removeAt(0)
    }

    override fun size(): TerminalSize = terminalSize
}
