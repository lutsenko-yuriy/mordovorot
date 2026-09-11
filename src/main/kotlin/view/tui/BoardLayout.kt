package view.tui

/** Interior character width of one board cell - wide enough for a 2-digit 1-based tile value
 *  plus a padding space on each side (` 12 `). */
private const val CELL_WIDTH = 4

/**
 * Pure geometry for GH-3's board screen: given a terminal size and the board's square side,
 * computes where the shift arrows, the grid, and the toolbar buttons land, and resolves a click
 * coordinate to a [HitTarget]. No terminal or presenter involved - [ScreenRenderer] draws to
 * these same coordinates so what's drawn is exactly what's clickable.
 *
 * Layout, top to bottom: title row, a blank row, the up-arrows row, the grid (box-drawing
 * borders around [squareSide] x [squareSide] cells, with left/right arrows flanking each row),
 * the down-arrows row, a blank row, the toolbar row. Centered in [terminalSize]; a terminal too
 * small to fit it still produces coordinates (some off-screen) rather than throwing - clicks
 * that land off-screen simply never reach [hitTest].
 */
class BoardLayout(
    private val terminalSize: TerminalSize,
    private val squareSide: Int,
    private val arrowsEnabled: Boolean = true,
) {
    private val innerCols = squareSide * CELL_WIDTH + (squareSide + 1)
    private val innerRows = squareSide * 2 + 1

    private val fullWidth = LEFT_GUTTER + innerCols + RIGHT_GUTTER
    private val fullHeight = TOP_EXTRA + innerRows + BOTTOM_EXTRA

    private val originX = ((terminalSize.columns - fullWidth) / 2).coerceAtLeast(0)
    private val originY = ((terminalSize.rows - fullHeight) / 2).coerceAtLeast(0)

    private val gridLeft = originX + LEFT_GUTTER
    private val gridTop = originY + 3

    val titleRow: Int get() = originY
    private val upArrowRow = originY + 2
    private val downArrowRow = gridTop + innerRows
    private val toolbarRow = downArrowRow + 2

    fun leftArrowPosition(row: Int): Pair<Int, Int> = originX to contentRowY(row)
    fun rightArrowPosition(row: Int): Pair<Int, Int> = (gridLeft + innerCols + 1) to contentRowY(row)
    fun upArrowPosition(col: Int): Pair<Int, Int> = cellCenterX(col) to upArrowRow
    fun downArrowPosition(col: Int): Pair<Int, Int> = cellCenterX(col) to downArrowRow

    fun saveButtonPosition(): Pair<Int, Int> = toolbarButtons()[0].second.first to toolbarRow
    fun loadButtonPosition(): Pair<Int, Int> = toolbarButtons()[1].second.first to toolbarRow
    fun exitButtonPosition(): Pair<Int, Int> = toolbarButtons()[2].second.first to toolbarRow

    fun hitTest(x: Int, y: Int): HitTarget {
        if (arrowsEnabled) {
            for (row in 0 until squareSide) {
                if (x to y == leftArrowPosition(row)) return HitTarget.ShiftLeft(row)
                if (x to y == rightArrowPosition(row)) return HitTarget.ShiftRight(row)
            }
            for (col in 0 until squareSide) {
                if (x to y == upArrowPosition(col)) return HitTarget.ShiftUp(col)
                if (x to y == downArrowPosition(col)) return HitTarget.ShiftDown(col)
            }
        }
        if (y == toolbarRow) {
            for ((target, range) in toolbarButtons()) if (x in range) return target
        }
        return HitTarget.Nothing
    }

    private fun contentRowY(row: Int) = gridTop + 1 + 2 * row
    private fun cellCenterX(col: Int) = gridLeft + 1 + col * (CELL_WIDTH + 1) + CELL_WIDTH / 2

    /** The toolbar's three buttons, in order, as (target, x-range) - all share [toolbarRow]. */
    private fun toolbarButtons(): List<Pair<HitTarget, IntRange>> {
        val labels = listOf(HitTarget.ToolbarSave to " Save ", HitTarget.ToolbarLoad to " Load ", HitTarget.ToolbarExit to " Exit ")
        val texts = labels.map { "[${it.second}]" }
        val totalWidth = texts.sumOf { it.length } + (texts.size - 1)
        var x = (originX + (fullWidth - totalWidth) / 2).coerceAtLeast(0)
        return labels.mapIndexed { i, (target, _) ->
            val text = texts[i]
            val range = x until (x + text.length)
            x += text.length + 1
            target to range
        }
    }

    private companion object {
        /** Arrow char + one gap column, on each side of the grid. */
        const val LEFT_GUTTER = 2
        const val RIGHT_GUTTER = 2

        /** Title row, a blank row, the up-arrows row. */
        const val TOP_EXTRA = 3

        /** The down-arrows row, a blank row, the toolbar row. */
        const val BOTTOM_EXTRA = 3
    }
}
