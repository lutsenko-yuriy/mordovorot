package web

import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.pointerevents.PointerEvent
import web.a11y.Announcements
import web.a11y.Focus
import web.a11y.focusKey
import web.drag.Axis
import web.drag.LineOffset
import web.drag.WrapPositions

/** The board in the page (GH-110): its tiles and arrows, and in keyboard mode its selection, turning clicks and drags into [game] intents.
 *  Split from [DomRenderer] (GH-171). */
internal class DomBoard(private val root: HTMLElement, private val game: () -> WebGame) {

    /** The pointer dragging a line, if any. [root] holds its capture, as the tiles are rebuilt. */
    private var dragPointer: Int? = null

    private var tiles: Element? = null
    private var outlines: Element? = null
    /** This board's row elements and cells, kept across a shift and a slide's frames so a reader's
     *  active cell stays the same element; the [copies] of tiles pushed past its edge come and go. */
    private var rows: List<Element> = emptyList()
    private var cells: List<Element> = emptyList()
    private val copies = mutableListOf<Element>()

    init {
        root.addEventListener("pointermove", { event ->
            val pointer = event as PointerEvent
            if (pointer.pointerId == dragPointer) game().onDragMove(pointer.clientX.toDouble(), pointer.clientY.toDouble())
        })
        root.addEventListener("pointerup", { endDrag(it as PointerEvent) { game().onDragEnd() } })
        root.addEventListener("pointercancel", { endDrag(it as PointerEvent) { game().onDragCancel() } })
        // After a pointerup the drag has already ended, so this only catches a capture lost mid-drag.
        root.addEventListener("lostpointercapture", { endDrag(it as PointerEvent) { game().onDragCancel() } })
    }

    /** The tiles, the line [offset] names moved by it, framed by the arrows: ▲ above and ▼ below each
     *  column, ◀ and ▶ at each end of a row. */
    fun draw(state: WebState, justSolved: Boolean, offset: LineOffset?): Element {
        val side = state.side
        val disabled = !state.arrowsEnabled || state.dialog != null
        fun arrow(label: String, arrow: Arrow, line: Int) =
            button(label, "arrow") { game().onArrow(arrow, line) }.focusKey(Focus.ArrowButton(arrow, line)).also {
                it.disabled = disabled
                it.setAttribute("aria-label", Announcements.line(arrow, line))
            }
        fun arrows(arrow: Arrow, label: String, axis: String) =
            element("div", "arrows $axis").apply { repeat(side) { append(arrow(label, arrow, it)) } }

        val layer = asTable(element("div", "tiles"), state.arrowsEnabled, state.selection)
        state.boardName?.let { asSolutionBoard(layer, it) }
        tiles = layer
        buildTiles(layer, state)
        redrawTiles(state, offset)

        val classes = listOfNotNull("board", "solved".takeIf { state.solved }, "just-solved".takeIf { justSolved })
        return element("div", classes.joinToString(" ")).apply {
            append(
                element("span"), arrows(Arrow.UP, "▲", "row"), element("span"),
                arrows(Arrow.LEFT, "◀", "column"),
                layer,
                arrows(Arrow.RIGHT, "▶", "column"),
                element("span"), arrows(Arrow.DOWN, "▼", "row"), element("span"),
            )
            outlines = state.selection?.let { selectionOutlines(it).also(::append) }
        }
    }

    /** The kept board brought up to [state], for a frame that doesn't rebuild it (GH-171). */
    fun update(state: WebState, offset: LineOffset?) {
        redrawTiles(state, offset)
        state.selection?.let { selection ->
            tiles?.setAttribute("aria-activedescendant", cellId(selection.row, selection.col))
            outlines?.let { placeOutlines(it, selection) }
        }
        state.boardName?.let { tiles?.setAttribute("aria-label", it) }
    }

    /** Fills [layer] with the board's cells, a row element per board row (GH-167). */
    private fun buildTiles(layer: Element, state: WebState) {
        val side = state.side
        val playing = state.arrowsEnabled
        copies.clear()
        rows = List(side) { gridRow(layer, playing) }
        cells = List(side * side) { i ->
            val row = i / side
            val col = i % side
            asCell(element("div", "tile"), playing, state.selection?.let { cellId(row, col) }).apply {
                addEventListener("pointerdown", { startDrag(it as PointerEvent, layer, side, row, col) })
                rows[row].append(this)
            }
        }
    }

    /** Puts [state]'s tiles in the cells, also for a slide's next animation frame, the line [offset]
     *  names moved by it: a tile pushed past one end shows at the other as well, and the layer clips both. */
    fun redrawTiles(state: WebState, offset: LineOffset?) {
        val side = state.side
        val wrapped = offset?.let { WrapPositions.of(side, it.tiles) }
        val texts = state.tileTexts
        val home = state.home
        val playing = state.arrowsEnabled
        copies.forEach { it.remove() }
        copies.clear()
        state.tiles.forEachIndexed { i, value ->
            val row = i / side
            val col = i % side
            val label = texts[i]
            val x = if (offset?.axis == Axis.ROW && offset.line == row) wrapped!![col] else col.toDouble()
            val y = if (offset?.axis == Axis.COLUMN && offset.line == col) wrapped!![row] else row.toDouble()
            place(cells[i], value, side, x, y)
            labelCell(cells[i], label, playing, playing && home[i])
            if (x > side - 1) copies += copy(rows[row], value, label, side, x - side, y)
            if (y > side - 1) copies += copy(rows[row], value, label, side, x, y - side)
        }
    }

    private fun copy(row: Element, value: Int, label: String, side: Int, x: Double, y: Double): Element =
        asCopy(place(text("tile", label), value, side, x, y)).also(row::append)

    /** Tile [value] at column [x], row [y], in tiles; its colour comes from where it belongs. */
    private fun place(tile: Element, value: Int, side: Int, x: Double, y: Double) = tile.apply {
        setAttribute(
            "style",
            "--x: $x; --y: $y; --row: ${(value - 1) / side}; --col: ${(value - 1) % side};",
        )
    }

    private fun startDrag(pointer: PointerEvent, tiles: Element, side: Int, row: Int, col: Int) {
        if (dragPointer != null || pointer.button.toInt() != 0) return
        pointer.preventDefault()
        dragPointer = pointer.pointerId
        root.setPointerCapture(pointer.pointerId)
        // Measured, so the drag follows the pointer at any tile size or zoom.
        val pitch = tiles.getBoundingClientRect().width / side
        // First: it redraws the tiles, and a drag under way would stop it.
        game().onTilePressed(row, col)
        game().onDragStart(row, col, pointer.clientX.toDouble(), pointer.clientY.toDouble(), pitch)
    }

    private fun endDrag(pointer: PointerEvent, end: () -> Unit) {
        if (pointer.pointerId != dragPointer) return
        dragPointer = null
        end()
    }
}
