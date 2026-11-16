package web

import kotlin.math.abs
import viewmodel.GameViewModel
import viewmodel.PointerShift
import web.a11y.Announcements
import web.drag.Axis
import web.drag.LineDrag
import web.drag.LineOffset
import web.drag.direction

/** [WebGame]'s dragging of a row or column (GH-110): a press on a tile, the moves, and the release
 *  as one shift. While one is under way, only the drag counts. */
internal class WebDragFlows(private val game: WebGame, private val viewModel: GameViewModel) {

    private var drag: LineDrag? = null

    val active get() = drag != null

    /** Where the dragged line is now, if it has left its place. */
    val offset: LineOffset? get() = drag?.offset

    /** A press on the tile at [row], [col], at ([x], [y]) on a board with tiles [pitch] apart, in pixels. */
    fun onStart(row: Int, col: Int, x: Double, y: Double, pitch: Double) {
        if (!game.playable) return
        drag = LineDrag(row, col, x, y, pitch, viewModel.squareSide())
    }

    fun onMove(x: Double, y: Double) {
        val drag = drag ?: return
        val before = drag.offset
        drag.move(x, y)
        if (drag.offset != before) game.render()
    }

    /** The release: the line snaps to the nearest tile, and the whole drag is one shift. */
    fun onEnd() {
        val released = drag?.release() ?: return onCancel()
        drag = null
        if (game.tutorial.take(released.axis, released.line, released.steps)) {
            game.shift({ Announcements.drag(released.direction, released.line, abs(released.steps), it) }) {
                when (released.axis) {
                    Axis.ROW -> viewModel.shiftRow(released.line, released.steps, PointerShift.DRAG)
                    Axis.COLUMN -> viewModel.shiftColumn(released.line, released.steps, PointerShift.DRAG)
                }
            }
            game.tutorial.afterShift()
        }
        game.share.openWaiting()
    }

    /** Drops the drag, leaving the board as it was. */
    fun onCancel() {
        val moved = drag?.offset != null
        drag = null
        if (moved) game.render()
        game.share.openWaiting()
    }
}
