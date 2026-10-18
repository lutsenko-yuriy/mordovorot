package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import web.drag.Axis
import web.drag.LineOffset

class SlidesTest {

    @Test
    fun `after an arrow shift the line starts one tile back, where it showed before`() {
        assertEquals(LineOffset(Axis.ROW, line = 2, tiles = 1.0), Slides.ofArrow(Arrow.LEFT, 2))
        assertEquals(LineOffset(Axis.ROW, line = 0, tiles = -1.0), Slides.ofArrow(Arrow.RIGHT, 0))
        assertEquals(LineOffset(Axis.COLUMN, line = 1, tiles = 1.0), Slides.ofArrow(Arrow.UP, 1))
        assertEquals(LineOffset(Axis.COLUMN, line = 3, tiles = -1.0), Slides.ofArrow(Arrow.DOWN, 3))
    }

    @Test
    fun `a released drag snaps the rest of the way to its nearest tile`() {
        assertEquals(
            LineOffset(Axis.ROW, line = 1, tiles = 0.25),
            Slides.afterDrag(LineOffset(Axis.ROW, line = 1, tiles = 2.25), moved = true, side = 4),
        )
        assertEquals(
            LineOffset(Axis.COLUMN, line = 0, tiles = -0.5),
            Slides.afterDrag(LineOffset(Axis.COLUMN, line = 0, tiles = 1.5), moved = true, side = 4),
        )
    }

    @Test
    fun `a drag that changed nothing slides back from where it was let go`() {
        assertEquals(
            LineOffset(Axis.ROW, line = 1, tiles = 1.75),
            Slides.afterDrag(LineOffset(Axis.ROW, line = 1, tiles = 1.75), moved = false, side = 4),
        )
        assertEquals(
            LineOffset(Axis.ROW, line = 1, tiles = 0.25),
            Slides.afterDrag(LineOffset(Axis.ROW, line = 1, tiles = 4.25), moved = false, side = 4),
        )
    }

    @Test
    fun `a drag let go on a whole tile needs no slide`() {
        assertNull(Slides.afterDrag(LineOffset(Axis.ROW, line = 1, tiles = 2.0), moved = true, side = 4))
        assertNull(Slides.afterDrag(LineOffset(Axis.ROW, line = 1, tiles = 0.0), moved = false, side = 4))
    }

    @Test
    fun `a slide eases out from its start to nothing`() {
        val start = LineOffset(Axis.ROW, line = 1, tiles = -1.0)

        assertEquals(start, Slides.at(start, progress = 0.0))
        assertEquals(LineOffset(Axis.ROW, line = 1, tiles = -0.125), Slides.at(start, progress = 0.5))
        assertNull(Slides.at(start, progress = 1.0))
        assertNull(Slides.at(start, progress = 1.5))
    }

    @Test
    fun `a frame stamped just before the slide began shows its start, not past it`() {
        val start = LineOffset(Axis.ROW, line = 1, tiles = -1.0)

        assertEquals(start, Slides.at(start, progress = -0.05))
    }
}
