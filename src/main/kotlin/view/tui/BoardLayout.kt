package view.tui

import InputMode

/** Interior character width of one board cell - wide enough for a 2-digit 1-based tile value
 *  plus a padding space on each side (` 12 `). Internal (not private) so [ScreenRenderer] can
 *  draw tile text at exactly the columns [BoardLayout] hit-tests against. */
internal const val CELL_WIDTH = 4

/**
 * Pure geometry for GH-3's board screen: given a terminal size and the board's square side,
 * computes where the shift arrows, the grid, and the toolbar buttons land, and resolves a click
 * coordinate to a [HitTarget]. No terminal or presenter involved - [ScreenRenderer] draws to
 * these same coordinates so what's drawn is exactly what's clickable.
 *
 * Layout, top to bottom: title row, a blank row, the up-arrows row, the grid (box-drawing
 * borders around [squareSide] x [squareSide] cells, with left/right arrows flanking each row),
 * the down-arrows row, a blank row, the toolbar row, and (GH-30) the mode row directly below it
 * for the Keyboard/Console switch buttons - kept on their own row, not appended to the toolbar
 * row, so a narrow terminal that fit the original three-button toolbar still fits it (audit
 * finding on PR #38: a single five-button row overflowed off-screen below 51 columns). Centered
 * in [terminalSize]; a terminal too small to fit it still produces coordinates (some off-screen)
 * rather than throwing - clicks that land off-screen simply never reach [hitTest].
 */
class BoardLayout(
    private val terminalSize: TerminalSize,
    private val squareSide: Int,
    private val arrowsEnabled: Boolean = true,
    /** Keyboard mode's toolbar labels (`[Save F5]`) vs. mouse mode's (`[ Save ]`) - see
     *  [toolbarButtons]. Must reach both [ScreenRenderer.render]'s and [TuiView]'s construction
     *  sites from the same [ScreenState.toolbarShortcuts] flag, or the drawn and hit-tested
     *  toolbars disagree (this class's own KDoc invariant). */
    private val toolbarShortcuts: Boolean = false,
    /** The other input modes offered on their own row below the toolbar (GH-30, [modeRow]). */
    private val modeButtons: List<InputMode> = emptyList(),
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
    internal val toolbarRow = downArrowRow + 2

    /** The Keyboard/Console mode-switch row, directly below [toolbarRow] (GH-30). */
    internal val modeRow = toolbarRow + 1

    /** The grid's top-left corner and size, for [ScreenRenderer] to draw the box-drawing frame
     *  and tile text at the same coordinates [hitTest] resolves clicks against. */
    fun gridOrigin(): Pair<Int, Int> = gridLeft to gridTop
    val gridWidth: Int get() = innerCols
    val gridHeight: Int get() = innerRows

    fun leftArrowPosition(row: Int): Pair<Int, Int> = originX to contentRowY(row)
    fun rightArrowPosition(row: Int): Pair<Int, Int> = (gridLeft + innerCols + 1) to contentRowY(row)
    fun upArrowPosition(col: Int): Pair<Int, Int> = cellCenterX(col) to upArrowRow
    fun downArrowPosition(col: Int): Pair<Int, Int> = cellCenterX(col) to downArrowRow

    fun saveButtonPosition(): Pair<Int, Int> = buttonPosition(HitTarget.ToolbarSave)
    fun loadButtonPosition(): Pair<Int, Int> = buttonPosition(HitTarget.ToolbarLoad)
    fun exitButtonPosition(): Pair<Int, Int> = buttonPosition(HitTarget.ToolbarExit)

    /** Looks a button up by target rather than a fixed index or row - defensive against either
     *  moving again (GH-30's mode buttons already moved once, off the toolbar row entirely). */
    private fun buttonPosition(target: HitTarget): Pair<Int, Int> =
        toolbarButtons().first { it.target == target }.let { it.range.first to it.y }

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
        if (y == toolbarRow || y == modeRow) {
            for (button in toolbarButtons()) if (y == button.y && x in button.range) return button.target
        }
        return HitTarget.Nothing
    }

    private fun contentRowY(row: Int) = gridTop + 1 + 2 * row
    private fun cellCenterX(col: Int) = gridLeft + 1 + col * (CELL_WIDTH + 1) + CELL_WIDTH / 2

    /** [toolbarRow]'s Save/Load/Exit buttons plus [modeRow]'s mode-switch buttons, each row
     *  independently centered - shared by [hitTest] (via [ToolbarButton.range]/[ToolbarButton.y])
     *  and [ScreenRenderer] (via [ToolbarButton.text]). Internal, not private, so [ScreenRenderer]
     *  can draw exactly what's clickable. */
    internal fun toolbarButtons(): List<ToolbarButton> {
        val primary = if (toolbarShortcuts) {
            listOf(HitTarget.ToolbarSave to "[Save F5]", HitTarget.ToolbarLoad to "[Load F6]", HitTarget.ToolbarExit to "[Exit ESC]")
        } else {
            listOf(HitTarget.ToolbarSave to "[ Save ]", HitTarget.ToolbarLoad to "[ Load ]", HitTarget.ToolbarExit to "[ Exit ]")
        }
        val modeEntries = modeButtons.map { HitTarget.ToolbarMode(it) to modeButtonLabel(it, toolbarShortcuts) }
        return layoutRow(primary, toolbarRow) + layoutRow(modeEntries, modeRow)
    }

    private fun layoutRow(entries: List<Pair<HitTarget, String>>, row: Int): List<ToolbarButton> {
        val totalWidth = entries.sumOf { it.second.length } + (entries.size - 1)
        var x = (originX + (fullWidth - totalWidth) / 2).coerceAtLeast(0)
        return entries.map { (target, text) ->
            val button = ToolbarButton(target, text, x, row, x until (x + text.length))
            x += text.length + 1
            button
        }
    }

    /** `[ Keyboard ]`/`[ Console ]` (mouse mode) vs. `[Mouse F7]`/`[Console F8]` (keyboard mode,
     *  WU4). Keyboard mode never offers switching to itself, so it has no assigned shortcut key -
     *  falls back to the plain label rather than drawing a broken `[Keyboard ]` (audit suggestion
     *  on PR #38). */
    private fun modeButtonLabel(mode: InputMode, shortcut: Boolean): String {
        val name = mode.name.lowercase().replaceFirstChar { it.uppercase() }
        val key = if (shortcut) {
            when (mode) {
                InputMode.MOUSE -> "F7"
                InputMode.CONSOLE -> "F8"
                InputMode.KEYBOARD -> null
            }
        } else null
        return if (key != null) "[$name $key]" else "[ $name ]"
    }

    internal data class ToolbarButton(val target: HitTarget, val text: String, val x: Int, val y: Int, val range: IntRange)

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
