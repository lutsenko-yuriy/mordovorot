package web

import kotlinx.dom.clear
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.pointerevents.PointerEvent
import web.drag.Axis
import web.drag.LineOffset
import web.drag.WrapPositions

/** The board in the page (GH-110): its tiles and arrows, and in keyboard mode its selection, turning clicks and drags into [game] intents.
 *  Split from [DomRenderer] (GH-171). */
internal class DomBoard(private val root: HTMLElement, private val game: () -> WebGame) {

    /** The pointer dragging a line, if any. [root] holds its capture, as the tiles are rebuilt. */
    private var dragPointer: Int? = null

    private var tiles: Element? = null

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
        tiles = layer
        drawTiles(layer, state, offset)

        val classes = listOfNotNull("board", "solved".takeIf { state.solved }, "just-solved".takeIf { justSolved })
        return element("div", classes.joinToString(" ")).apply {
            append(
                element("span"), arrows(Arrow.UP, "▲", "row"), element("span"),
                arrows(Arrow.LEFT, "◀", "column"),
                layer,
                arrows(Arrow.RIGHT, "▶", "column"),
                element("span"), arrows(Arrow.DOWN, "▼", "row"), element("span"),
            )
            state.selection?.let { append(selectionOutlines(it)) }
        }
    }

    /** This frame's tiles again, for a slide's next animation frame. */
    fun redrawTiles(state: WebState, offset: LineOffset?) {
        tiles?.let { drawTiles(it, state, offset) }
    }

    /** Fills [layer] with the board's tiles, a row element per board row (GH-167), the line [offset]
     *  names moved by it: a tile pushed past one end shows at the other as well, and the layer clips both. */
    private fun drawTiles(layer: Element, state: WebState, offset: LineOffset?) {
        val side = state.side
        val wrapped = offset?.let { WrapPositions.of(side, it.tiles) }
        val texts = state.tileTexts
        val home = state.home
        val playing = state.arrowsEnabled
        layer.clear()
        repeat(side) { row ->
            val cells = gridRow(layer, playing)
            repeat(side) { col ->
                val value = state.tiles[row * side + col]
                val label = texts[row * side + col]
                val x = if (offset?.axis == Axis.ROW && offset.line == row) wrapped!![col] else col.toDouble()
                val y = if (offset?.axis == Axis.COLUMN && offset.line == col) wrapped!![row] else row.toDouble()
                val selected = state.selection?.takeIf { it.row == row && it.col == col } != null
                cells.append(asCell(tile(value, label, side, x, y), label, playing, playing && home[row * side + col], state.selection != null, selected).apply {
                    addEventListener("pointerdown", { startDrag(it as PointerEvent, layer, side, row, col) })
                })
                if (x > side - 1) cells.append(asCopy(tile(value, label, side, x - side, y)))
                if (y > side - 1) cells.append(asCopy(tile(value, label, side, x, y - side)))
            }
        }
    }

    /** Tile [value], showing [label], at column [x], row [y], in tiles; its colour comes from where it belongs. */
    private fun tile(value: Int, label: String, side: Int, x: Double, y: Double) = text("tile", label).apply {
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
