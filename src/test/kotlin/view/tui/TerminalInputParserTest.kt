package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals

private const val ESC: Byte = 0x1B

/**
 * Covers GH-3's terminal input decoding: turning raw bytes read from the terminal into
 * [TerminalEvent]s. Pure byte-stream -> event decoding, no real terminal involved - see the
 * plan comment on GH-3 for the full SGR-1006 format reference.
 */
class TerminalInputParserTest {

    /** `ESC [ < body`, e.g. csi("<0;5;3M") for the SGR press used throughout this file. */
    private fun csi(body: String) = byteArrayOf(ESC, '['.code.toByte()) + body.toByteArray(Charsets.US_ASCII)

    @Test
    fun `SGR press event parses to a MouseClick with 0-based coordinates`() {
        val parser = TerminalInputParser()

        val events = parser.feed(csi("<0;5;3M"))

        assertEquals(listOf(TerminalEvent.MouseClick(4, 2)), events)
    }

    @Test
    fun `SGR release event does not itself produce a click`() {
        val parser = TerminalInputParser()
        parser.feed(csi("<0;5;3M"))

        val events = parser.feed(csi("<0;5;3m"))

        assertEquals(emptyList(), events)
    }

    @Test
    fun `an SGR wheel notch decodes to a Wheel, not a MouseClick (GH-126)`() {
        val parser = TerminalInputParser()

        // Pb 64/65 (bit 0x40 set) - a scroll notch up/down, not a button; no release report.
        assertEquals(listOf(TerminalEvent.Wheel(up = true)), parser.feed(csi("<64;5;3M")))
        assertEquals(listOf(TerminalEvent.Wheel(up = false)), parser.feed(csi("<65;5;3M")))
    }

    @Test
    fun `a legacy X10 wheel notch decodes to a Wheel (GH-126)`() {
        val parser = TerminalInputParser()

        // Cb 96/97 is 64/65 offset by 32.
        assertEquals(listOf(TerminalEvent.Wheel(up = true)), parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'M'.code.toByte(), 96, 37, 35)))
        assertEquals(listOf(TerminalEvent.Wheel(up = false)), parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'M'.code.toByte(), 97, 37, 35)))
    }

    @Test
    fun `legacy X10 fallback event parses correctly`() {
        val parser = TerminalInputParser()

        // ESC [ M Cb Cx Cy, each byte offset by 32; column 5, row 3 -> bytes 37, 35.
        val events = parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'M'.code.toByte(), 32, 37, 35))

        assertEquals(listOf(TerminalEvent.MouseClick(4, 2)), events)
    }

    @Test
    fun `a legacy X10 release does not itself produce a second MouseClick`() {
        val parser = TerminalInputParser()
        parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'M'.code.toByte(), 32, 37, 35))

        // Cb 35 -> (35-32) & 0x3 == 3, the X10 release code, regardless of which button.
        val events = parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'M'.code.toByte(), 35, 37, 35))

        assertEquals(emptyList(), events)
    }

    @Test
    fun `an escape sequence split across two reads completes on the next read`() {
        val parser = TerminalInputParser()

        val firstHalf = parser.feed(csi("<0;5;"))
        assertEquals(emptyList(), firstHalf)

        val secondHalf = parser.feed("3M".toByteArray(Charsets.US_ASCII))
        assertEquals(listOf(TerminalEvent.MouseClick(4, 2)), secondHalf)
    }

    @Test
    fun `a printable character produces KeyPress`() {
        val parser = TerminalInputParser()

        val events = parser.feed("a".toByteArray(Charsets.US_ASCII))

        assertEquals(listOf(TerminalEvent.KeyPress('a')), events)
    }

    @Test
    fun `Backspace, Enter, and Escape bytes produce their sentinel events`() {
        val parser = TerminalInputParser()

        assertEquals(listOf(TerminalEvent.Backspace), parser.feed(byteArrayOf(127)))
        assertEquals(listOf(TerminalEvent.Enter), parser.feed(byteArrayOf(13)))
        assertEquals(listOf(TerminalEvent.Escape), parser.feed(byteArrayOf(ESC)))
    }

    @Test
    fun `end of the underlying stream produces EndOfInput`() {
        val parser = TerminalInputParser()

        assertEquals(TerminalEvent.EndOfInput, parser.endOfInput())
    }

    @Test
    fun `an unrecognised CSI final byte is discarded whole, not leaked as individual KeyPresses`() {
        val parser = TerminalInputParser()

        // ESC [ H - Home, a CSI form this parser has no meaning for.
        val events = parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'H'.code.toByte()))

        assertEquals(emptyList(), events)
    }

    @Test
    fun `a truncated escape sequence past the length cap is discarded whole, not leaked byte-by-byte`() {
        val parser = TerminalInputParser()

        // "ESC[<" followed by garbage that never reaches an M/m terminator.
        val garbage = parser.feed(csi("<") + ByteArray(40) { '9'.code.toByte() })
        assertEquals(emptyList(), garbage)

        val events = parser.feed("a".toByteArray(Charsets.US_ASCII))
        assertEquals(listOf(TerminalEvent.KeyPress('a')), events)
    }

    @Test
    fun `CSI arrow keys decode to Arrow events with the matching direction`() {
        val parser = TerminalInputParser()

        assertEquals(
            listOf(TerminalEvent.Arrow(Direction.UP)),
            parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'A'.code.toByte())),
        )
        assertEquals(
            listOf(TerminalEvent.Arrow(Direction.DOWN)),
            parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'B'.code.toByte())),
        )
        assertEquals(
            listOf(TerminalEvent.Arrow(Direction.RIGHT)),
            parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'C'.code.toByte())),
        )
        assertEquals(
            listOf(TerminalEvent.Arrow(Direction.LEFT)),
            parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'D'.code.toByte())),
        )
    }

    @Test
    fun `SS3 arrow keys decode to the same Arrow events as their CSI form`() {
        val parser = TerminalInputParser()

        // ESC O A - the SS3 arrow-key form used under DECCKM (tmux/screen, some terminals).
        val events = parser.feed(byteArrayOf(ESC, 'O'.code.toByte(), 'A'.code.toByte()))

        assertEquals(listOf(TerminalEvent.Arrow(Direction.UP)), events)
    }

    @Test
    fun `an unrecognised SS3 final byte is discarded whole, not leaked as a KeyPress`() {
        val parser = TerminalInputParser()

        val events = parser.feed(byteArrayOf(ESC, 'O'.code.toByte(), 'P'.code.toByte()))

        assertEquals(emptyList(), events)
    }

    @Test
    fun `a modifier-prefixed arrow is discarded whole, not decoded as a plain arrow`() {
        val parser = TerminalInputParser()

        // ESC[1;2A - Shift+Up. Terminals/multiplexers that pass this through are not asking
        // for plain cursor movement, so this must not fall through to a bare Arrow(UP).
        val events = parser.feed(csi("1;2A"))

        assertEquals(emptyList(), events)
    }

    @Test
    fun `Tab byte decodes to Tab, and CSI Z decodes to BackTab`() {
        val parser = TerminalInputParser()

        assertEquals(listOf(TerminalEvent.Tab), parser.feed(byteArrayOf(9)))
        assertEquals(
            listOf(TerminalEvent.BackTab),
            parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'Z'.code.toByte())),
        )
    }

    @Test
    fun `function keys are swallowed whole, CSI and SS3 alike (GH-170)`() {
        val parser = TerminalInputParser()

        for (params in listOf("14~", "15~", "17~", "18~", "19~", "20~")) assertEquals(emptyList(), parser.feed(csi(params)), params)
        // ESC O P..S - F1-F4 as xterm, macOS Terminal and iTerm2 send them.
        for (final in "PQRS") assertEquals(emptyList(), parser.feed(byteArrayOf(ESC, 'O'.code.toByte(), final.code.toByte())), "SS3 $final")
        assertEquals(listOf(TerminalEvent.KeyPress('x')), parser.feed("x".toByteArray()))
    }

    @Test
    fun `an unrecognised function-key number is discarded whole, not leaked as KeyPresses`() {
        val parser = TerminalInputParser()

        // ESC[21~ - F10, which this parser has no meaning for.
        val events = parser.feed(csi("21~"))

        assertEquals(emptyList(), events)
    }

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun `a Cyrillic letter's UTF-8 bytes are one KeyPress (GH-200)`() {
        val parser = TerminalInputParser()

        assertEquals(listOf(TerminalEvent.KeyPress('ц')), parser.feed(bytes(0xD1, 0x86)))
        assertEquals(listOf(TerminalEvent.KeyPress('Ц')), parser.feed("Ц".toByteArray()))
        assertEquals(listOf(TerminalEvent.KeyPress('і'), TerminalEvent.KeyPress('ы')), parser.feed("іы".toByteArray()))
    }

    @Test
    fun `a UTF-8 letter split across two feeds waits for its second byte`() {
        val parser = TerminalInputParser()

        assertEquals(emptyList(), parser.feed(bytes(0xD1)))
        assertEquals(listOf(TerminalEvent.KeyPress('ц')), parser.feed(bytes(0x86)))
    }

    @Test
    fun `a stray byte is dropped alone and the next key kept`() {
        val parser = TerminalInputParser()

        assertEquals(listOf(TerminalEvent.KeyPress('x')), parser.feed(bytes(0x86, 'x'.code)))
        assertEquals(listOf(TerminalEvent.KeyPress('x')), parser.feed(bytes(0xD1, 'x'.code)))
        assertEquals(listOf(TerminalEvent.KeyPress('x')), parser.feed(bytes(0xFF, 'x'.code)))
    }

    @Test
    fun `a lead byte followed by an escape sequence keeps the sequence`() {
        val parser = TerminalInputParser()

        assertEquals(listOf(TerminalEvent.Arrow(Direction.UP)), parser.feed(bytes(0xD1, 0x1B, '['.code, 'A'.code)))
    }

    @Test
    fun `other UTF-8 characters are swallowed whole`() {
        val parser = TerminalInputParser()

        for (other in listOf("ω", "ש", "ü", "€", "😀")) assertEquals(emptyList(), parser.feed(other.toByteArray()), other)
        assertEquals(listOf(TerminalEvent.KeyPress('x')), parser.feed("x".toByteArray()))
    }

    @Test
    fun `an X10 report with a coordinate byte over 0x7F is still one click`() {
        val parser = TerminalInputParser()

        val events = parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'M'.code.toByte()) + bytes(32, 0xD1, 33))

        assertEquals(listOf(TerminalEvent.MouseClick(0xD1 - 33, 0)), events)
    }
}
