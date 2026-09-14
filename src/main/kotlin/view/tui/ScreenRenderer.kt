package view.tui

/** GH-10's console dialect: tiles are stored 0-based, displayed 1-based. */
private const val DISPLAY_OFFSET = 1

private const val DIM_ON = "\u001B[2m"
private const val DIM_OFF = "\u001B[22m"

/** SGR reverse-video on/off - GH-18's keyboard-mode focus highlight (the cursor arrow, a
 *  focused dialog button). */
private const val REVERSE_ON = "\u001B[7m"
private const val REVERSE_OFF = "\u001B[27m"

/** Appended to the Save dialog's text field when keyboard focus (GH-18) is on it. Internal,
 *  not private - [DialogLayout.structuralContentWidth] reserves room for it unconditionally so
 *  the box doesn't resize (and truncate the caret) the moment focus lands on the field. */
internal const val TEXT_FIELD_FOCUS_MARKER = " ◀"

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
        val layout = BoardLayout(terminalSize, state.squareSide, state.arrowsEnabled, state.toolbarShortcuts)
        val canvas = Canvas(terminalSize.columns.coerceAtLeast(1), terminalSize.rows.coerceAtLeast(1))

        canvas.put(centeredX(state.title, terminalSize), layout.titleRow, state.title)
        drawGrid(canvas, layout, state)
        drawArrows(canvas, layout, state)
        drawToolbar(canvas, layout)
        state.message?.let { canvas.put(2, layout.toolbarRow + 2, it) }
        // Anchored to layout.toolbarRow (one row below the status message), not the raw
        // terminal's last row - the unconditional terminalSize.rows - 1 used to land on the
        // toolbar or message row on a short-but-wide terminal, overwriting them (audit finding
        // on GH-18 WU2 PR #31). Off-canvas is fine here, same as every other BoardLayout
        // coordinate on a too-small terminal (see that class's KDoc) - canvas.put no-ops.
        state.controlsHint?.let { canvas.put(0, layout.toolbarRow + 3, it) }
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
        // The keyboard-mode cursor (GH-18) always wins over dimming - state.cursor is only ever
        // non-null when arrowsEnabled is true (ScreenState's own KDoc), so the two never compete
        // for the same arrow.
        fun glyph(g: String, isCursor: Boolean) = when {
            isCursor -> "$REVERSE_ON$g$REVERSE_OFF"
            state.arrowsEnabled -> g
            else -> "$DIM_ON$g$DIM_OFF"
        }

        for (row in 0 until state.squareSide) {
            val (lx, ly) = layout.leftArrowPosition(row)
            canvas.putGlyph(lx, ly, glyph("◀", state.cursor == ArrowCursor(Edge.LEFT, row)))
            val (rx, ry) = layout.rightArrowPosition(row)
            canvas.putGlyph(rx, ry, glyph("▶", state.cursor == ArrowCursor(Edge.RIGHT, row)))
        }
        for (col in 0 until state.squareSide) {
            val (ux, uy) = layout.upArrowPosition(col)
            canvas.putGlyph(ux, uy, glyph("▲", state.cursor == ArrowCursor(Edge.TOP, col)))
            val (dx, dy) = layout.downArrowPosition(col)
            canvas.putGlyph(dx, dy, glyph("▼", state.cursor == ArrowCursor(Edge.BOTTOM, col)))
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
        // The message is already word-wrapped to fit maxLineWidth (DialogLayout.messageLines) -
        // drawn one line per row, not truncated, so a long multi-clause message (e.g. the exit
        // flow's invalid-name explanation) doesn't lose its actionable half (audit round 3 on
        // PR #24).
        layout.messageLines.forEachIndexed { index, line -> canvas.put(layout.left + 2, layout.messageRowPosition(index), line) }
        layout.textFieldRow?.let { row ->
            val focusMarker = if (dialog.textFieldFocused) TEXT_FIELD_FOCUS_MARKER else ""
            canvas.put(layout.left + 2, row, truncate("Name: ${dialog.textFieldValue}_$focusMarker", maxLineWidth))
        }
        for (index in dialog.listItems.indices) {
            val marker = if (index == dialog.selectedIndex) "> " else "  "
            canvas.put(layout.left + 2, layout.listRowPosition(index), truncate("$marker${dialog.listItems[index]}", maxLineWidth))
        }
        val focusedTarget = dialog.focusedButtonId?.let { HitTarget.DialogButton(it) }
        for (button in layout.buttons()) {
            if (button.target == focusedTarget) canvas.putHighlighted(button.x, layout.buttonsRow(), button.text)
            else canvas.put(button.x, layout.buttonsRow(), button.text)
        }
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
 * so wrapping it in ANSI codes never shifts surrounding columns. [putHighlighted] is the
 * multi-character equivalent (e.g. a focused dialog button's `[ Save ]`): the reverse-video
 * codes are folded into the first and last cell's content rather than spread across every
 * column, for the same reason.
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

    fun putHighlighted(x: Int, y: Int, text: String) {
        if (text.isEmpty() || y !in 0 until height) return
        // REVERSE_ON/REVERSE_OFF go on the first/last *visible* index, not the first/last index
        // of `text` - if the span is clipped by the canvas edge, closing on text's own last index
        // would never get written, leaking reverse video into every row/frame after this one
        // (audit finding on GH-18 WU2 PR #31: a narrow terminal clipping a dialog button did
        // exactly this).
        val visible = text.indices.filter { x + it in 0 until width }
        if (visible.isEmpty()) return
        val first = visible.first()
        val last = visible.last()
        for (i in visible) {
            val col = x + i
            val on = if (i == first) REVERSE_ON else ""
            val off = if (i == last) REVERSE_OFF else ""
            rows[y][col] = "$on${text[i]}$off"
        }
    }

    fun render(): String = rows.joinToString("\r\n") { row -> row.joinToString("") { it ?: " " } }
}
