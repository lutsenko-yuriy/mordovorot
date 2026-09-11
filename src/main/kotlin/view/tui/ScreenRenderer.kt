package view.tui

/** GH-10's console dialect: tiles are stored 0-based, displayed 1-based. */
private const val DISPLAY_OFFSET = 1

private const val DIM_ON = "\u001B[2m"
private const val DIM_OFF = "\u001B[22m"

/** Clears the screen and homes the cursor - every frame is a full repaint. */
private const val CLEAR_AND_HOME = "\u001B[2J\u001B[H"

/**
 * Pure [ScreenState] -> frame `String` renderer for GH-3's board screen: box-drawing borders,
 * 1-based tile values, arrow glyphs (dimmed when disabled), and the toolbar. Builds a character
 * canvas from the same [BoardLayout] coordinates [BoardLayout.hitTest] resolves clicks against,
 * so what's drawn is exactly what's clickable. No terminal involved - [TuiView] writes the
 * returned string via [Terminal.write].
 */
class ScreenRenderer {

    fun render(state: ScreenState, terminalSize: TerminalSize): String {
        val layout = BoardLayout(terminalSize, state.squareSide, state.arrowsEnabled)
        val canvas = Canvas(terminalSize.columns.coerceAtLeast(1), terminalSize.rows.coerceAtLeast(1))

        canvas.put(centeredX(state.title, terminalSize), layout.titleRow, state.title)
        drawGrid(canvas, layout, state)
        drawArrows(canvas, layout, state)
        drawToolbar(canvas, layout)
        state.message?.let { canvas.put(2, layout.toolbarRow + 2, it) }
        state.dialog?.let { drawDialog(canvas, it, terminalSize) }

        return CLEAR_AND_HOME + canvas.render()
    }

    private fun centeredX(text: String, terminalSize: TerminalSize) =
        ((terminalSize.columns - text.length) / 2).coerceAtLeast(0)

    private fun drawGrid(canvas: Canvas, layout: BoardLayout, state: ScreenState) {
        val (gridLeft, gridTop) = layout.gridOrigin()
        val bottomRow = layout.gridHeight - 1

        for (rowIndex in 0..bottomRow) {
            val line = when {
                rowIndex == 0 -> borderLine(state.squareSide, "┌", "┬", "┐", "─")
                rowIndex == bottomRow -> borderLine(state.squareSide, "└", "┴", "┘", "─")
                rowIndex % 2 == 0 -> borderLine(state.squareSide, "├", "┼", "┤", "─")
                else -> borderLine(state.squareSide, "│", "│", "│", " ")
            }
            canvas.put(gridLeft, gridTop + rowIndex, line)
        }

        for (row in 0 until state.squareSide) {
            for (col in 0 until state.squareSide) {
                val value = state.board[row * state.squareSide + col] + DISPLAY_OFFSET
                val text = value.toString().padStart(CELL_WIDTH - 1).padEnd(CELL_WIDTH)
                canvas.put(gridLeft + 1 + col * (CELL_WIDTH + 1), gridTop + 1 + 2 * row, text)
            }
        }
    }

    private fun borderLine(squareSide: Int, left: String, mid: String, right: String, fill: String): String {
        val builder = StringBuilder(left)
        for (col in 0 until squareSide) {
            builder.append(fill.repeat(CELL_WIDTH))
            builder.append(if (col == squareSide - 1) right else mid)
        }
        return builder.toString()
    }

    private fun drawArrows(canvas: Canvas, layout: BoardLayout, state: ScreenState) {
        fun glyph(g: String) = if (state.arrowsEnabled) g else "$DIM_ON$g$DIM_OFF"

        for (row in 0 until state.squareSide) {
            val (lx, ly) = layout.leftArrowPosition(row)
            canvas.putGlyph(lx, ly, glyph("◀"))
            val (rx, ry) = layout.rightArrowPosition(row)
            canvas.putGlyph(rx, ry, glyph("▶"))
        }
        for (col in 0 until state.squareSide) {
            val (ux, uy) = layout.upArrowPosition(col)
            canvas.putGlyph(ux, uy, glyph("▲"))
            val (dx, dy) = layout.downArrowPosition(col)
            canvas.putGlyph(dx, dy, glyph("▼"))
        }
    }

    private fun drawToolbar(canvas: Canvas, layout: BoardLayout) {
        for (button in layout.toolbarButtons()) canvas.put(button.x, layout.toolbarRow, button.text)
    }

    private fun drawDialog(canvas: Canvas, dialog: Dialog, terminalSize: TerminalSize) {
        val layout = DialogLayout(dialog, terminalSize)
        // DialogLayout.width can be capped below what the content actually needs (a terminal
        // too narrow to fit it in full) - truncating here is what keeps that content from
        // overwriting the box's own right border (audit round 2 on PR #24).
        val maxLineWidth = (layout.width - 4).coerceAtLeast(1)
        drawDialogBox(canvas, layout)
        canvas.put(layout.left + 2, layout.titleRow(), truncate(dialog.title, maxLineWidth))
        dialog.message?.let { canvas.put(layout.left + 2, layout.messageRow!!, truncate(it, maxLineWidth)) }
        layout.textFieldRow?.let { row -> canvas.put(layout.left + 2, row, truncate("Name: ${dialog.textFieldValue}_", maxLineWidth)) }
        for (index in dialog.listItems.indices) {
            val marker = if (index == dialog.selectedIndex) "> " else "  "
            canvas.put(layout.left + 2, layout.listRowPosition(index), truncate("$marker${dialog.listItems[index]}", maxLineWidth))
        }
        for (button in layout.buttons()) canvas.put(button.x, layout.buttonsRow(), button.text)
    }

    private fun truncate(text: String, maxWidth: Int): String =
        if (text.length <= maxWidth) text else text.take((maxWidth - 1).coerceAtLeast(0)) + "…"

    private fun drawDialogBox(canvas: Canvas, layout: DialogLayout) {
        val top = layout.titleRow() - 1
        val bottom = layout.bottomRow() + 1
        val left = layout.left - 1
        val boxWidth = layout.width + 2
        canvas.put(left, top, "┌" + "─".repeat(boxWidth - 2) + "┐")
        for (row in (top + 1) until bottom) canvas.put(left, row, "│" + " ".repeat(boxWidth - 2) + "│")
        canvas.put(left, bottom, "└" + "─".repeat(boxWidth - 2) + "┘")
    }
}

/**
 * A fixed-size character grid, written to by column/row coordinate and rendered as one
 * newline-joined string. Writes fully or partially outside the canvas are clipped rather than
 * thrown - [BoardLayout] can hand back coordinates past a too-small terminal (see its own
 * KDoc), and drawing must degrade gracefully, not crash.
 *
 * Each cell holds one display-width unit: [put] writes plain text one character per column
 * (grid lines, tile text, the title, toolbar labels - none of which contain escapes), while
 * [putGlyph] writes a whole escape-wrapped glyph (e.g. a dimmed arrow) into exactly one cell,
 * so wrapping it in ANSI codes never shifts surrounding columns.
 */
private class Canvas(private val width: Int, private val height: Int) {
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

    fun render(): String = rows.joinToString("\r\n") { row -> row.joinToString("") { it ?: " " } }
}
