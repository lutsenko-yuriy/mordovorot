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
    fun `an SGR wheel notch does not produce a MouseClick`() {
        val parser = TerminalInputParser()

        // Pb 64 (bit 0x40 set) - a scroll notch, not a button; wheel reports have no release.
        val events = parser.feed(csi("<64;5;3M"))

        assertEquals(emptyList(), events)
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
    fun `Tab byte decodes to Tab, and CSI Z decodes to BackTab`() {
        val parser = TerminalInputParser()

        assertEquals(listOf(TerminalEvent.Tab), parser.feed(byteArrayOf(9)))
        assertEquals(
            listOf(TerminalEvent.BackTab),
            parser.feed(byteArrayOf(ESC, '['.code.toByte(), 'Z'.code.toByte())),
        )
    }

    @Test
    fun `F5, F6, and F7 CSI sequences decode to their FunctionKey events`() {
        val parser = TerminalInputParser()

        assertEquals(listOf(TerminalEvent.FunctionKey(5)), parser.feed(csi("15~")))
        assertEquals(listOf(TerminalEvent.FunctionKey(6)), parser.feed(csi("17~")))
        assertEquals(listOf(TerminalEvent.FunctionKey(7)), parser.feed(csi("18~")))
    }

    @Test
    fun `an unrecognised function-key number is discarded whole, not leaked as KeyPresses`() {
        val parser = TerminalInputParser()

        // ESC[19~ - F8, which this parser has no meaning for.
        val events = parser.feed(csi("19~"))

        assertEquals(emptyList(), events)
    }
}
