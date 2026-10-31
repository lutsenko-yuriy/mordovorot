package view.tui

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [AnsiTerminal] is otherwise the plan's one deliberately untested seam (real `stty`/terminal
 * I/O, covered by WU5's human smoke tests instead), but [AnsiTerminal.readEvent] draining a
 * real [java.io.InputStream] one byte at a time used to break mouse decoding entirely - a lone
 * leading `ESC` was handed to the parser alone and decoded as a standalone Escape key before
 * the rest of the sequence ever arrived. This covers that fix without needing a real terminal.
 */
class AnsiTerminalReadEventTest {

    private fun terminalReading(bytes: ByteArray) =
        AnsiTerminal(ByteArrayInputStream(bytes), PrintStream(ByteArrayOutputStream()))

    @Test
    fun `a full mouse sequence delivered in one input burst decodes to a single MouseClick`() {
        val terminal = terminalReading(byteArrayOf(0x1B, '['.code.toByte(), '<'.code.toByte()) + "0;5;3M".toByteArray())

        assertEquals(TerminalEvent.MouseClick(4, 2), terminal.readEvent())
    }

    @Test
    fun `a lone Escape keypress with nothing following still resolves to Escape`() {
        val terminal = terminalReading(byteArrayOf(0x1B))

        assertEquals(TerminalEvent.Escape, terminal.readEvent())
    }

    @Test
    fun `an extra event in the same burst is queued, not dropped, for the next readEvent call`() {
        val terminal = terminalReading(byteArrayOf(0x1B, '['.code.toByte(), '<'.code.toByte()) + "0;5;3M".toByteArray() + "a".toByteArray())

        assertEquals(TerminalEvent.MouseClick(4, 2), terminal.readEvent())
        assertEquals(TerminalEvent.KeyPress('a'), terminal.readEvent())
    }

    @Test
    fun `a Cyrillic letter's bytes are one key, in one read or two (GH-200)`() {
        assertEquals(TerminalEvent.KeyPress('ц'), terminalReading("ц".toByteArray()).readEvent())

        // available() is 0, so the second byte only comes with the next read.
        val oneByteAtATime = object : ByteArrayInputStream("ц".toByteArray()) {
            override fun available() = 0
        }
        val terminal = AnsiTerminal(oneByteAtATime, PrintStream(ByteArrayOutputStream()))
        assertEquals(TerminalEvent.KeyPress('ц'), terminal.readEvent())
    }
}
