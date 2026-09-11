package view.tui

import kotlin.test.Test

/**
 * Scenario stubs for GH-3's terminal input decoding: turning raw bytes read from the
 * terminal into [TerminalEvent]s. Pure byte-stream -> event decoding, no real terminal
 * involved. Filled in by `implement` during WU2, once `TerminalEvent`/`TerminalInputParser`
 * exist. See the plan comment on GH-3 for the full SGR-1006 format reference.
 */
class TerminalInputParserTest {

    @Test
    fun `SGR press event parses to a MouseClick with 0-based coordinates`() {
        // TODO: Feed the parser the bytes for `CSI <0;5;3M` (SGR press, button 0, col 5, row 3)
        // TODO: Verify it yields MouseClick(x=4, y=2) - 1-based wire coordinates become 0-based
    }

    @Test
    fun `SGR release event does not itself produce a click`() {
        // TODO: Feed a press event, consume it, then feed the matching `CSI <0;5;3m` release
        // TODO: Verify the release does not yield a second MouseClick
    }

    @Test
    fun `legacy X10 fallback event parses correctly`() {
        // TODO: Feed the older 3-byte `ESC [ M Cb Cx Cy` form for the same click
        // TODO: Verify it yields the equivalent MouseClick coordinates
    }

    @Test
    fun `an escape sequence split across two reads completes on the next read`() {
        // TODO: Feed only the first half of an SGR event's bytes
        // TODO: Verify no event is produced yet
        // TODO: Feed the remaining bytes
        // TODO: Verify the MouseClick event now fires, using the buffered prefix
    }

    @Test
    fun `a printable character produces KeyPress`() {
        // TODO: Feed a single printable ASCII byte (e.g. 'a')
        // TODO: Verify it yields KeyPress('a')
    }

    @Test
    fun `Backspace, Enter, and Escape bytes produce their sentinel events`() {
        // TODO: Feed the Backspace byte - verify TerminalEvent.Backspace
        // TODO: Feed the Enter byte (CR or LF) - verify TerminalEvent.Enter
        // TODO: Feed a bare Escape byte (not part of a longer sequence) - verify TerminalEvent.Escape
    }

    @Test
    fun `end of the underlying stream produces EndOfInput`() {
        // TODO: Simulate the input stream ending (read returns -1)
        // TODO: Verify the parser yields TerminalEvent.EndOfInput
    }
}
