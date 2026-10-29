package view.tui

/** SGR dim on/off - inert controls (dimmed arrows, buttons, dialog rows). */
internal const val DIM_ON = "\u001B[2m"
internal const val DIM_OFF = "\u001B[22m"

/** SGR reverse-video on/off - GH-18's dialog focus highlight (a focused button) and GH-170's
 *  selected tile. */
private const val REVERSE_ON = "\u001B[7m"
private const val REVERSE_OFF = "\u001B[27m"

/**
 * A fixed-size character grid, written to by column/row coordinate and rendered as one
 * newline-joined string. Writes fully or partially outside the canvas are clipped rather than
 * thrown - [BoardLayout] can hand back coordinates past a too-small terminal (see its own
 * KDoc), and drawing must degrade gracefully, not crash.
 *
 * Each cell holds one display-width unit: [put] writes plain text one character per column,
 * while [putGlyph] writes a whole escape-wrapped glyph (e.g. a colored arrow) into exactly one
 * cell, so the ANSI codes never shift surrounding columns. [putHighlighted]/[putColored] are the
 * multi-character equivalent (a focused dialog button, a keyboard-mode toolbar label): the SGR
 * codes fold into the first and last cell only, for the same reason.
 */
internal class Canvas(private val width: Int, private val height: Int) {
    private val rows = Array(height) { arrayOfNulls<String>(width) }

    fun put(x: Int, y: Int, text: String) {
        if (y !in 0 until height) return
        for (i in text.indices) {
            val col = x + i
            if (col in 0 until width) rows[y][col] = text[i].toString()
        }
    }

    fun putGlyph(x: Int, y: Int, content: String) {
        if (y in 0 until height && x in 0 until width) rows[y][x] = content
    }

    fun putHighlighted(x: Int, y: Int, text: String) = putStyled(x, y, text, REVERSE_ON, REVERSE_OFF)

    fun putColored(x: Int, y: Int, text: String, onCode: String, offCode: String) = putStyled(x, y, text, onCode, offCode)

    private fun putStyled(x: Int, y: Int, text: String, onCode: String, offCode: String) {
        if (text.isEmpty() || y !in 0 until height) return
        // on/offCode go on the first/last *visible* index, not text's own - otherwise a span
        // clipped by the canvas edge never writes its close code, leaking the style downstream.
        val visible = text.indices.filter { x + it in 0 until width }
        if (visible.isEmpty()) return
        val first = visible.first()
        val last = visible.last()
        for (i in visible) {
            val col = x + i
            val on = if (i == first) onCode else ""
            val off = if (i == last) offCode else ""
            rows[y][col] = "$on${text[i]}$off"
        }
    }

    fun render(): String = rows.joinToString("\r\n") { row -> row.joinToString("") { it ?: " " } }
}
