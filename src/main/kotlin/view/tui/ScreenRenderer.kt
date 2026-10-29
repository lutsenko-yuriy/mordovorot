package view.tui

import viewmodel.GiveUpButton
import viewmodel.SolutionState

/** The rest of the selected tile's row (pale yellow) and column (light grey), black text
 *  (GH-170): 256-colour backgrounds. Provisional until GH-170 WU4 picks the final palette. */
private const val SELECTED_ROW_ON = "\u001B[30;48;5;230m"
private const val SELECTED_COLUMN_ON = "\u001B[30;48;5;252m"
private const val SELECTED_LINE_OFF = "\u001B[39;49m"

/** SGR bright-cyan foreground on/off - GH-18's keyboard-mode toolbar shortcut labels. */
private const val SHORTCUT_COLOR_ON = "\u001B[96m"
private const val SHORTCUT_COLOR_OFF = "\u001B[39m"

/** SGR magenta foreground on/off - GH-83's `Mordie:` prefix. */
private const val MORDIE_COLOR_ON = "\u001B[35m"
private const val MORDIE_COLOR_OFF = "\u001B[39m"
private const val MORDIE_PREFIX = "Mordie:"

/** Clears the screen and homes the cursor - every frame is a full repaint. */
private const val CLEAR_AND_HOME = "\u001B[2J\u001B[H"

/**
 * Pure [ScreenState] -> frame `String` renderer for GH-3's board screen: box-drawing borders,
 * tile labels ([ScreenState.tileLabels], GH-139), arrow glyphs (dimmed when disabled), and the toolbar. Builds a character
 * canvas from the same [BoardLayout] coordinates [BoardLayout.hitTest] resolves clicks against,
 * so what's drawn is exactly what's clickable. No terminal involved - [TuiView] writes the
 * returned string via [Terminal.write].
 */
class ScreenRenderer {

    fun render(state: ScreenState, terminalSize: TerminalSize): String {
        val layout = state.layout(terminalSize)
        val canvas = Canvas(terminalSize.columns.coerceAtLeast(1), terminalSize.rows.coerceAtLeast(1))

        canvas.put(centeredX(state.title, terminalSize), layout.titleRow, state.title)
        if (state.solution == null) {
            val moves = "Moves: ${state.moveCount}"
            canvas.put(centeredX(moves, terminalSize), layout.movesRow, moves)
        }
        state.mordieLine?.let { drawMordieLine(canvas, layout, it, terminalSize) }
        drawGrid(canvas, layout, state)
        if (state.showArrows) drawArrows(canvas, layout, state)
        drawSideHints(canvas, layout, state)
        drawToolbar(canvas, layout, state.toolbarShortcuts)
        drawGiveUp(canvas, layout, state)
        state.solution?.let { drawSolution(canvas, layout, it, state.stepOffset) }
        state.message?.let { canvas.put(2, layout.toolbarRow + 2, it) }
        // Anchored to layout.toolbarRow (one row below the status message), not the raw
        // terminal's last row - the unconditional terminalSize.rows - 1 used to land on the
        // toolbar or message row on a short-but-wide terminal, overwriting them (audit finding
        // on GH-18 WU2 PR #31). Off-canvas is fine here, same as every other BoardLayout
        // coordinate on a too-small terminal (see that class's KDoc) - canvas.put no-ops.
        state.controlsHint?.let { canvas.put(0, layout.toolbarRow + 3, it) }
        state.dialog?.let { DialogRenderer.draw(canvas, it, terminalSize) }

        return CLEAR_AND_HOME + canvas.render()
    }

    private fun centeredX(text: String, terminalSize: TerminalSize) =
        ((terminalSize.columns - text.length) / 2).coerceAtLeast(0)

    /** Wrapped and centered row by row; the prefix is always on the first row, so only that row
     *  is colored. */
    private fun drawMordieLine(canvas: Canvas, layout: BoardLayout, text: String, terminalSize: TerminalSize) {
        wordWrap("$MORDIE_PREFIX $text", layout.mordieWrapWidth).take(layout.mordieRows).forEachIndexed { index, line ->
            val x = centeredX(line, terminalSize)
            val y = layout.mordieRow + index
            canvas.put(x, y, line)
            if (index == 0) canvas.putColored(x, y, MORDIE_PREFIX, MORDIE_COLOR_ON, MORDIE_COLOR_OFF)
        }
    }

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
                val label = state.tileLabels.label(state.board[row * state.squareSide + col], state.squareSide)
                val text = label.padStart(CELL_WIDTH - 1).padEnd(CELL_WIDTH)
                val x = gridLeft + 1 + col * (CELL_WIDTH + 1)
                val y = gridTop + 1 + 2 * row
                val selection = state.selection
                when {
                    selection == null -> canvas.put(x, y, text)
                    row == selection.row && col == selection.col -> canvas.putHighlighted(x, y, text)
                    row == selection.row -> canvas.putColored(x, y, text, SELECTED_ROW_ON, SELECTED_LINE_OFF)
                    col == selection.col -> canvas.putColored(x, y, text, SELECTED_COLUMN_ON, SELECTED_LINE_OFF)
                    else -> canvas.put(x, y, text)
                }
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

    /** Keyboard mode's hints (GH-101): selection keys right-aligned left of the board, shift
     *  keys right of it. A too-narrow terminal clips them, like every other off-canvas text. */
    private fun drawSideHints(canvas: Canvas, layout: BoardLayout, state: ScreenState) {
        state.selectionHint.forEachIndexed { i, line -> canvas.put(layout.leftHintEndX - line.length, layout.sideHintRow(i), line) }
        state.shiftHint.forEachIndexed { i, line -> canvas.put(layout.rightHintX, layout.sideHintRow(i), line) }
    }

    private fun drawToolbar(canvas: Canvas, layout: BoardLayout, toolbarShortcuts: Boolean) {
        for (button in layout.toolbarButtons()) {
            if (toolbarShortcuts) canvas.putColored(button.x, button.y, button.text, SHORTCUT_COLOR_ON, SHORTCUT_COLOR_OFF)
            else canvas.put(button.x, button.y, button.text)
        }
    }

    /** Dimmed until Mordie's solution is there to show (GH-126). */
    private fun drawGiveUp(canvas: Canvas, layout: BoardLayout, state: ScreenState) {
        val button = layout.giveUpButton() ?: return
        when {
            state.giveUpButton == GiveUpButton.DIMMED -> canvas.putColored(button.x, button.y, button.text, DIM_ON, DIM_OFF)
            state.toolbarShortcuts -> canvas.putColored(button.x, button.y, button.text, SHORTCUT_COLOR_ON, SHORTCUT_COLOR_OFF)
            else -> canvas.put(button.x, button.y, button.text)
        }
    }

    /** The visible window of the step list, the current step reverse video, and Prev / Next /
     *  Restart, dimmed where inert (GH-126). */
    private fun drawSolution(canvas: Canvas, layout: BoardLayout, solution: SolutionState, offset: Int) {
        for (row in 0 until layout.stepRows) {
            val index = offset + row
            val step = solution.steps.getOrNull(index) ?: break
            val y = layout.stepListTop + row
            if (index == solution.current) canvas.putHighlighted(layout.stepListX, y, step) else canvas.put(layout.stepListX, y, step)
        }
        for (button in layout.replayButtons()) {
            if (button.target == HitTarget.Nothing) canvas.putColored(button.x, button.y, button.text, DIM_ON, DIM_OFF)
            else canvas.put(button.x, button.y, button.text)
        }
    }
}
