package view.tui

/** Everything GH-3's mouse TUI needs from a real terminal, kept behind an interface so
 *  `TuiView` (WU3) can run against `FakeTerminal` with zero real terminal I/O. [AnsiTerminal]
 *  is the only implementation that touches an actual terminal. */
interface Terminal {
    fun enterRawMode()
    fun restore()
    fun enableMouseReporting()
    fun write(frame: String)
    /** Blocks for the next event; with [timeoutMs], returns [TerminalEvent.Tick] once that
     *  passes with no input (GH-126's replay clock). */
    fun readEvent(timeoutMs: Long? = null): TerminalEvent
    fun size(): TerminalSize
}
