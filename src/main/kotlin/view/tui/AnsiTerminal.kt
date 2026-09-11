package view.tui

import java.io.InputStream
import java.io.PrintStream

private const val ENTER_ALT_SCREEN = "[?1049h"
private const val EXIT_ALT_SCREEN = "[?1049l"
private const val ENABLE_MOUSE = "[?1000h[?1006h"
private const val DISABLE_MOUSE = "[?1006l[?1000l"
private const val HIDE_CURSOR = "[?25l"
private const val SHOW_CURSOR = "[?25h"

/**
 * The one class in `view.tui` that touches a real terminal: `stty` for cbreak mode, ANSI
 * escapes for the alternate screen buffer and SGR-1006 mouse reporting, and a shutdown hook so
 * all of that is undone even on an abnormal exit (Ctrl+C, an uncaught exception). Everything
 * else in `view.tui` is pure or tested against [FakeTerminal] instead - this class is covered
 * by the human smoke tests in the GH-3 plan, not by unit tests.
 */
class AnsiTerminal(
    private val input: InputStream = System.`in`,
    private val output: PrintStream = System.out,
) : Terminal {

    private val parser = TerminalInputParser()
    private val pendingEvents = ArrayDeque<TerminalEvent>()

    // Read by the shutdown-hook thread, written by whichever thread calls enterRawMode/restore.
    @Volatile
    private var rawModeEntered = false
    private var shutdownHookRegistered = false

    override fun enterRawMode() {
        if (rawModeEntered) return
        // -icanon -echo (cbreak), not `stty raw` - raw also disables ISIG, so Ctrl+C would stop
        // generating SIGINT and never reach the shutdown hook below.
        ProcessBuilder("stty", "-icanon", "-echo").inheritIO().start().waitFor()
        rawModeEntered = true
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(Thread { restore() })
            shutdownHookRegistered = true
        }
        output.print(ENTER_ALT_SCREEN + HIDE_CURSOR)
        output.flush()
    }

    override fun restore() {
        if (!rawModeEntered) return
        // Cleared only once the work below actually succeeds - if it throws, rawModeEntered
        // stays true, so a later call (the shutdown hook included) retries instead of no-op'ing
        // over a terminal that's still raw.
        output.print(DISABLE_MOUSE + SHOW_CURSOR + EXIT_ALT_SCREEN)
        output.flush()
        ProcessBuilder("stty", "sane").inheritIO().start().waitFor()
        rawModeEntered = false
    }

    override fun enableMouseReporting() {
        output.print(ENABLE_MOUSE)
        output.flush()
    }

    override fun write(frame: String) {
        output.print(frame)
        output.flush()
    }

    /** Reads one full chunk per [InputStream.read] burst (not one byte at a time) before handing
     *  it to [parser] - a real escape sequence normally arrives in a single burst, so this is
     *  what lets [TerminalInputParser] tell a genuine standalone Escape key apart from the start
     *  of one. Queues any extra decoded events instead of dropping them (a click plus a fast
     *  keystroke can share a burst).
     *
     *  Caveat: a sequence fragmented across two OS-level reads (a slow pipe, a laggy SSH hop)
     *  still degrades to a stray Escape plus leftover keystrokes, same as the case this fixed -
     *  the underlying ambiguity (is a lone ESC the whole input, or is more still coming?) needs
     *  a read timeout to resolve properly, which this stdlib-only terminal layer doesn't have.
     */
    override fun readEvent(): TerminalEvent {
        while (pendingEvents.isEmpty()) {
            val first = input.read()
            if (first == -1) return parser.endOfInput()
            pendingEvents.addAll(parser.feed(byteArrayOf(first.toByte()) + readAvailable()))
        }
        return pendingEvents.removeFirst()
    }

    private fun readAvailable(): ByteArray {
        val available = input.available()
        if (available <= 0) return ByteArray(0)
        val chunk = ByteArray(available)
        var read = 0
        while (read < available) {
            val n = input.read(chunk, read, available - read)
            if (n == -1) break
            read += n
        }
        return if (read == available) chunk else chunk.copyOf(read)
    }

    override fun size(): TerminalSize =
        try {
            val process = ProcessBuilder("stty", "size").redirectInput(ProcessBuilder.Redirect.INHERIT).start()
            val line = process.inputStream.bufferedReader().use { it.readLine() }
            process.waitFor()
            val (rows, columns) = line.trim().split(" ").map { it.toInt() }
            TerminalSize(columns, rows)
        } catch (e: Exception) {
            TerminalSize(80, 24)
        }
}
