package view.tui

import InputMode

/** Interior character width of one board cell - wide enough for a 2-digit 1-based tile value
 *  plus a padding space on each side (` 12 `). Internal (not private) so [ScreenRenderer] can
 *  draw tile text at exactly the columns [BoardLayout] hit-tests against. */
internal const val CELL_WIDTH = 4

/**
 * Pure geometry for GH-3's board screen: given a terminal size and the board's square side,
 * computes where the shift arrows, the grid, and the toolbar buttons land, and resolves a click
 * coordinate to a [HitTarget]. No terminal or viewModel involved - [ScreenRenderer] draws to
 * these same coordinates so what's drawn is exactly what's clickable.
 *
 * Layout, top to bottom: title row, the moves row (GH-62), the Mordie rows (GH-83, [mordieRows]),
 * a blank row, the up-arrows row, the grid (box-drawing
 * borders around [squareSide] x [squareSide] cells, with left/right arrows flanking each row),
 * the down-arrows row, a blank row, the toolbar row, and (GH-30) the mode row directly below it
 * for the mode-switch button - kept on their own row, not appended to the toolbar
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

    private val primaryEntries = primaryButtons(toolbarShortcuts)
    private val modeEntries = modeButtons.map { HitTarget.ToolbarMode(it) to modeButtonLabel(it, toolbarShortcuts) }

    /** The Mordie line's wrap width (GH-83): about as wide as the board and toolbar, never the
     *  full terminal. */
    internal val mordieWrapWidth =
        minOf(maxOf(fullWidth, rowWidth(primaryEntries), rowWidth(modeEntries)), terminalSize.columns - 2).coerceAtLeast(1)

    /** Rows reserved for the Mordie line: the wrapped height of the longest line it can say. Fixed
     *  per layout, not per text, so the board doesn't jump as the line comes and goes. */
    internal val mordieRows = wordWrap(LONGEST_MORDIE_LINE, mordieWrapWidth).size

    private val topExtra = TOP_EXTRA + mordieRows
    private val fullHeight = topExtra + innerRows + BOTTOM_EXTRA

    private val originX = ((terminalSize.columns - fullWidth) / 2).coerceAtLeast(0)
    // Centered with the rows under the toolbar counted too, so the Mordie rows (GH-83) don't push
    // keyboard mode's controls hint off a 24-row terminal at 5x5.
    private val originY = ((terminalSize.rows - fullHeight - BELOW_TOOLBAR) / 2).coerceAtLeast(0)

    private val gridLeft = originX + LEFT_GUTTER
    private val gridTop = originY + topExtra

    val titleRow: Int get() = originY

    /** `Moves: N`, directly under the title (GH-62). */
    val movesRow: Int get() = originY + 1

    /** The first of the [mordieRows] Mordie rows, directly under [movesRow] (GH-83). */
    internal val mordieRow: Int get() = originY + 2
    private val upArrowRow = gridTop - 1
    private val downArrowRow = gridTop + innerRows
    internal val toolbarRow = downArrowRow + 2

    /** The mode-switch row, directly below [toolbarRow] (GH-30). */
    internal val modeRow = toolbarRow + 1

    /** The grid's top-left corner and size, for [ScreenRenderer] to draw the box-drawing frame
     *  and tile text at the same coordinates [hitTest] resolves clicks against. */
    fun gridOrigin(): Pair<Int, Int> = gridLeft to gridTop
    val gridWidth: Int get() = innerCols
    val gridHeight: Int get() = innerRows

    fun leftArrowPosition(row: Int): Pair<Int, Int> = originX to contentRowY(row)
    fun rightArrowPosition(row: Int): Pair<Int, Int> = (gridLeft + innerCols + 1) to contentRowY(row)
    fun upArrowPosition(col: Int): Pair<Int, Int> = cellCenterX(col) to upArrowRow

    /** Row of keyboard mode's [index]th side-hint line (GH-101), centred on the grid. */
    fun sideHintRow(index: Int): Int = gridTop + innerRows / 2 - 1 + index

    /** The left hints end just before this column, a gap left of the arrow gutter (GH-101). */
    val leftHintEndX: Int get() = originX - HINT_GAP

    /** The right hints start here, a gap right of the arrow gutter (GH-101). */
    val rightHintX: Int get() = originX + fullWidth + HINT_GAP
    fun downArrowPosition(col: Int): Pair<Int, Int> = cellCenterX(col) to downArrowRow

    fun saveButtonPosition(): Pair<Int, Int> = buttonPosition(HitTarget.ToolbarSave)
    fun loadButtonPosition(): Pair<Int, Int> = buttonPosition(HitTarget.ToolbarLoad)
    fun exitButtonPosition(): Pair<Int, Int> = buttonPosition(HitTarget.ToolbarExit)
    fun newButtonPosition(): Pair<Int, Int> = buttonPosition(HitTarget.ToolbarNew)

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

    /** [toolbarRow]'s New/Save/Load/Exit buttons plus [modeRow]'s mode-switch buttons, each row
     *  independently centered - shared by [hitTest] (via [ToolbarButton.range]/[ToolbarButton.y])
     *  and [ScreenRenderer] (via [ToolbarButton.text]). Internal, not private, so [ScreenRenderer]
     *  can draw exactly what's clickable. */
    internal fun toolbarButtons(): List<ToolbarButton> =
        layoutRow(primaryEntries, toolbarRow) + layoutRow(modeEntries, modeRow)

    private fun primaryButtons(shortcuts: Boolean): List<Pair<HitTarget, String>> =
        if (shortcuts) {
            listOf(
                HitTarget.ToolbarNew to "[New F9]",
                HitTarget.ToolbarSave to "[Save F5]",
                HitTarget.ToolbarLoad to "[Load F6]",
                HitTarget.ToolbarExit to "[Exit ESC]",
            )
        } else {
            listOf(
                HitTarget.ToolbarNew to "[ New ]",
                HitTarget.ToolbarSave to "[ Save ]",
                HitTarget.ToolbarLoad to "[ Load ]",
                HitTarget.ToolbarExit to "[ Exit ]",
            )
        }

    private fun rowWidth(entries: List<Pair<HitTarget, String>>) = entries.sumOf { it.second.length } + (entries.size - 1)

    private fun layoutRow(entries: List<Pair<HitTarget, String>>, row: Int): List<ToolbarButton> {
        var x = (originX + (fullWidth - rowWidth(entries)) / 2).coerceAtLeast(0)
        return entries.map { (target, text) ->
            val button = ToolbarButton(target, text, x, row, x until (x + text.length))
            x += text.length + 1
            button
        }
    }

    /** `[ Keyboard ]` (mouse mode) vs. `[Mouse F7]` (keyboard mode, WU4). Keyboard mode never offers switching to itself, so it has no assigned shortcut key -
     *  falls back to the plain label rather than drawing a broken `[Keyboard ]` (audit suggestion
     *  on PR #38). */
    private fun modeButtonLabel(mode: InputMode, shortcut: Boolean): String {
        val name = mode.name.lowercase().replaceFirstChar { it.uppercase() }
        val key = if (shortcut) {
            when (mode) {
                InputMode.MOUSE -> "F7"
                InputMode.KEYBOARD -> null
            }
        } else null
        return if (key != null) "[$name $key]" else "[ $name ]"
    }

    internal data class ToolbarButton(val target: HitTarget, val text: String, val x: Int, val y: Int, val range: IntRange)

    private companion object {
        /** Arrow char + one gap column, on each side of the grid. */
        const val LEFT_GUTTER = 2
        const val HINT_GAP = 2
        const val RIGHT_GUTTER = 2

        /** Title row, moves row, a blank row, the up-arrows row - plus [mordieRows]. */
        const val TOP_EXTRA = 4

        /** The longest line Mordie says, at a 2-digit move count (GH-83). */
        const val LONGEST_MORDIE_LINE = "Mordie: This one is hard! I have already made 99 moves and have not solved it yet!"

        /** The down-arrows row, a blank row, the toolbar row. */
        const val BOTTOM_EXTRA = 3

        /** The mode row, the status message and the controls hint (see [ScreenRenderer.render]). */
        const val BELOW_TOOLBAR = 3
    }
}
