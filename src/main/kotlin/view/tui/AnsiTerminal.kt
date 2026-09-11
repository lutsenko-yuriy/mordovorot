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
 * The one class in `view.tui` that touches a real terminal: `stty` for raw/cbreak mode, ANSI
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
    private var rawModeEntered = false

    override fun enterRawMode() {
        if (rawModeEntered) return
        ProcessBuilder("stty", "raw", "-echo").inheritIO().start().waitFor()
        rawModeEntered = true
        Runtime.getRuntime().addShutdownHook(Thread { restore() })
        output.print(ENTER_ALT_SCREEN + HIDE_CURSOR)
        output.flush()
    }

    override fun restore() {
        if (!rawModeEntered) return
        rawModeEntered = false
        output.print(DISABLE_MOUSE + SHOW_CURSOR + EXIT_ALT_SCREEN)
        output.flush()
        ProcessBuilder("stty", "sane").inheritIO().start().waitFor()
    }

    override fun enableMouseReporting() {
        output.print(ENABLE_MOUSE)
        output.flush()
    }

    override fun write(frame: String) {
        output.print(frame)
        output.flush()
    }

    override fun readEvent(): TerminalEvent {
        while (true) {
            val b = input.read()
            val event = if (b == -1) parser.endOfInput() else parser.feed(byteArrayOf(b.toByte())).firstOrNull()
            if (event != null) return event
        }
    }

    override fun size(): TerminalSize {
        val columns = System.getenv("COLUMNS")?.toIntOrNull() ?: 80
        val rows = System.getenv("LINES")?.toIntOrNull() ?: 24
        return TerminalSize(columns, rows)
    }
}
