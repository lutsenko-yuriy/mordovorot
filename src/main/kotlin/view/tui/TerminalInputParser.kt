package view.tui

private const val ESC = 0x1B

/** A malformed/garbled escape sequence this long is given up on and resynced past, rather than
 *  buffering forever waiting for a terminator that will never arrive. SGR-1006 reports are well
 *  under this. */
private const val MAX_ESCAPE_SEQUENCE_LENGTH = 32

/**
 * Pure byte-stream -> [TerminalEvent] decoder for GH-3's mouse TUI: SGR-1006 mouse reports
 * (primary), the legacy X10 `ESC[M` fallback, and single-byte keys for the save-name field.
 * No terminal I/O here - [AnsiTerminal] feeds it real bytes and tests feed it directly. A
 * sequence split across two [feed] calls is buffered and completed on the next one.
 */
class TerminalInputParser {

    private var pending = ByteArray(0)

    fun feed(bytes: ByteArray): List<TerminalEvent> {
        val data = pending + bytes
        val events = mutableListOf<TerminalEvent>()
        var i = 0
        while (i < data.size) {
            val consumed = decodeOne(data, i, events)
            if (consumed == 0) break
            i += consumed
        }
        pending = data.copyOfRange(i, data.size)
        return events
    }

    fun endOfInput(): TerminalEvent = TerminalEvent.EndOfInput

    /** Decodes the event starting at [from], appending it to [events]. Returns bytes consumed,
     *  or 0 if [data] doesn't hold a complete sequence yet (more bytes needed). */
    private fun decodeOne(data: ByteArray, from: Int, events: MutableList<TerminalEvent>): Int {
        val b = data[from].toInt() and 0xFF
        if (b != ESC) {
            events += decodeSingleByte(b)
            return 1
        }

        // A lone ESC with nothing else in the buffer is treated as a standalone Escape key,
        // not a truncated sequence - real terminals send a full CSI sequence in one write.
        if (from + 1 >= data.size) {
            events += TerminalEvent.Escape
            return 1
        }
        if ((data[from + 1].toInt() and 0xFF) != '['.code) {
            events += TerminalEvent.Escape
            return 1
        }
        if (from + 2 >= data.size) return 0

        return when (data[from + 2].toInt() and 0xFF) {
            '<'.code -> decodeSgr(data, from, events)
            'M'.code -> decodeX10(data, from, events)
            else -> decodeUnknownCsi(data, from)
        }
    }

    /** `ESC [ < Pb ; Px ; Py (M|m)`. `M` is a press (-> [TerminalEvent.MouseClick]), `m` a
     *  release (no event). Returns 0 until the terminating `M`/`m` byte arrives. */
    private fun decodeSgr(data: ByteArray, from: Int, events: MutableList<TerminalEvent>): Int {
        val end = scanFor(data, from + 3, 'M', 'm') ?: return giveUpOrWait(data, from)

        val press = data[end].toInt().toChar() == 'M'
        val parts = String(data, from + 3, end - (from + 3), Charsets.US_ASCII).split(';')
        val px = parts.getOrNull(1)?.toIntOrNull()
        val py = parts.getOrNull(2)?.toIntOrNull()
        if (press && px != null && py != null) events += TerminalEvent.MouseClick(px - 1, py - 1)
        return end - from + 1
    }

    /** `ESC [ M Cb Cx Cy` - legacy X10, each of Cb/Cx/Cy a raw byte offset by 32. */
    private fun decodeX10(data: ByteArray, from: Int, events: MutableList<TerminalEvent>): Int {
        if (from + 5 >= data.size) return 0
        val column = (data[from + 4].toInt() and 0xFF) - 32
        val row = (data[from + 5].toInt() and 0xFF) - 32
        events += TerminalEvent.MouseClick(column - 1, row - 1)
        return 6
    }

    /** Any other CSI sequence (arrow keys, Home/End, ...) - discarded whole rather than left to
     *  leak its individual bytes out as bogus [TerminalEvent.KeyPress]es. */
    private fun decodeUnknownCsi(data: ByteArray, from: Int): Int {
        var i = from + 2 // the final byte can be immediately after '[' (e.g. arrow keys: ESC [ A)
        while (i < data.size) {
            if ((data[i].toInt() and 0xFF) in 0x40..0x7E) return i - from + 1 // CSI final byte
            i++
        }
        return giveUpOrWait(data, from)
    }

    /** 0 (wait for more) while [data] since [from] is still under [MAX_ESCAPE_SEQUENCE_LENGTH];
     *  past that it's treated as garbage and just the `ESC` is dropped, so a garbled/truncated
     *  sequence can't wedge the parser forever. */
    private fun giveUpOrWait(data: ByteArray, from: Int): Int = if (data.size - from > MAX_ESCAPE_SEQUENCE_LENGTH) 1 else 0

    private fun scanFor(data: ByteArray, from: Int, vararg terminators: Char): Int? {
        for (i in from until data.size) if (data[i].toInt().toChar() in terminators) return i
        return null
    }

    private fun decodeSingleByte(b: Int): TerminalEvent = when (b) {
        8, 127 -> TerminalEvent.Backspace
        10, 13 -> TerminalEvent.Enter
        else -> TerminalEvent.KeyPress(b.toChar())
    }
}
