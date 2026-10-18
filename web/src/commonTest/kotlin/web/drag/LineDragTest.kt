package web.drag

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** A press on the tile at row 1, column 2 of a 4x4 at (100, 100), with tiles 40 px apart. */
private fun drag() = LineDrag(row = 1, col = 2, x = 100.0, y = 100.0, pitch = 40.0, side = 4)

class LineDragTest {

    @Test
    fun `no axis until the pointer moves past the threshold`() {
        val drag = drag()

        drag.move(100.0 + LineDrag.THRESHOLD - 1, 102.0)

        assertNull(drag.offset)
        assertNull(drag.release())
    }

    @Test
    fun `a mostly sideways move locks the row`() {
        val drag = drag()

        drag.move(120.0, 108.0)

        assertEquals(LineOffset(Axis.ROW, line = 1, tiles = 0.5), drag.offset)
    }

    @Test
    fun `a mostly vertical move locks the column`() {
        val drag = drag()

        drag.move(92.0, 80.0)

        assertEquals(LineOffset(Axis.COLUMN, line = 2, tiles = -0.5), drag.offset)
    }

    @Test
    fun `once locked, the axis stays and the other direction is ignored`() {
        val drag = drag()
        drag.move(120.0, 100.0)

        drag.move(110.0, 220.0)

        assertEquals(LineOffset(Axis.ROW, line = 1, tiles = 0.25), drag.offset)
    }

    @Test
    fun `release rounds to the nearest whole tile`() {
        assertEquals(DragShift(Axis.ROW, line = 1, steps = 2), drag().apply { move(165.0, 100.0) }.release())
        assertEquals(DragShift(Axis.ROW, line = 1, steps = 1), drag().apply { move(155.0, 100.0) }.release())
        assertEquals(DragShift(Axis.COLUMN, line = 2, steps = -1), drag().apply { move(100.0, 50.0) }.release())
    }

    @Test
    fun `release under half a tile changes nothing`() {
        assertNull(drag().apply { move(119.0, 100.0) }.release())
    }

    @Test
    fun `release back at the start changes nothing`() {
        val drag = drag()
        drag.move(150.0, 100.0)

        drag.move(100.0, 100.0)

        assertEquals(LineOffset(Axis.ROW, line = 1, tiles = 0.0), drag.offset)
        assertNull(drag.release())
    }

    @Test
    fun `a drag of a loop or more is reduced modulo the side, keeping its direction`() {
        assertEquals(DragShift(Axis.ROW, line = 1, steps = 1), drag().apply { move(300.0, 100.0) }.release())
        assertEquals(DragShift(Axis.ROW, line = 1, steps = -1), drag().apply { move(-100.0, 100.0) }.release())
        assertNull(drag().apply { move(260.0, 100.0) }.release())
    }
}
