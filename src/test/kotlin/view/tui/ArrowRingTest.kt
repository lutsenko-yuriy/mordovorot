package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers GH-18's keyboard board cursor: [ArrowRing.move] stepping an [ArrowCursor] around the
 * perimeter of shift arrows (corners wrap to the nearest arrow on the adjacent edge rather than
 * dead-ending), and [ArrowRing.toHitTarget] mapping a cursor to the [HitTarget.ShiftLeft]/
 * [HitTarget.ShiftRight]/[HitTarget.ShiftUp]/[HitTarget.ShiftDown] a click on that arrow would
 * produce. Pure geometry, no terminal or presenter involved. Exhaustive over every
 * (edge, index, direction) combination for squareSide = 4, plus squareSide = 2 where the corner
 * cases collapse onto each other (row 0 == last row).
 */
class ArrowRingTest {

    private fun assertMoves(
        ring: ArrowRing,
        cursor: ArrowCursor,
        up: ArrowCursor,
        down: ArrowCursor,
        left: ArrowCursor,
        right: ArrowCursor,
    ) {
        assertEquals(up, ring.move(cursor, Direction.UP), "$cursor + UP")
        assertEquals(down, ring.move(cursor, Direction.DOWN), "$cursor + DOWN")
        assertEquals(left, ring.move(cursor, Direction.LEFT), "$cursor + LEFT")
        assertEquals(right, ring.move(cursor, Direction.RIGHT), "$cursor + RIGHT")
    }

    @Test
    fun `every LEFT-edge cursor moves per the table, for squareSide 4`() {
        val ring = ArrowRing(squareSide = 4)

        assertMoves(
            ring, ArrowCursor(Edge.LEFT, 0),
            up = ArrowCursor(Edge.TOP, 0), down = ArrowCursor(Edge.LEFT, 1),
            left = ArrowCursor(Edge.RIGHT, 0), right = ArrowCursor(Edge.RIGHT, 0),
        )
        assertMoves(
            ring, ArrowCursor(Edge.LEFT, 1),
            up = ArrowCursor(Edge.LEFT, 0), down = ArrowCursor(Edge.LEFT, 2),
            left = ArrowCursor(Edge.RIGHT, 1), right = ArrowCursor(Edge.RIGHT, 1),
        )
        assertMoves(
            ring, ArrowCursor(Edge.LEFT, 2),
            up = ArrowCursor(Edge.LEFT, 1), down = ArrowCursor(Edge.LEFT, 3),
            left = ArrowCursor(Edge.RIGHT, 2), right = ArrowCursor(Edge.RIGHT, 2),
        )
        assertMoves(
            ring, ArrowCursor(Edge.LEFT, 3),
            up = ArrowCursor(Edge.LEFT, 2), down = ArrowCursor(Edge.BOTTOM, 0),
            left = ArrowCursor(Edge.RIGHT, 3), right = ArrowCursor(Edge.RIGHT, 3),
        )
    }

    @Test
    fun `every RIGHT-edge cursor moves per the table, for squareSide 4`() {
        val ring = ArrowRing(squareSide = 4)

        assertMoves(
            ring, ArrowCursor(Edge.RIGHT, 0),
            up = ArrowCursor(Edge.TOP, 3), down = ArrowCursor(Edge.RIGHT, 1),
            left = ArrowCursor(Edge.LEFT, 0), right = ArrowCursor(Edge.LEFT, 0),
        )
        assertMoves(
            ring, ArrowCursor(Edge.RIGHT, 1),
            up = ArrowCursor(Edge.RIGHT, 0), down = ArrowCursor(Edge.RIGHT, 2),
            left = ArrowCursor(Edge.LEFT, 1), right = ArrowCursor(Edge.LEFT, 1),
        )
        assertMoves(
            ring, ArrowCursor(Edge.RIGHT, 2),
            up = ArrowCursor(Edge.RIGHT, 1), down = ArrowCursor(Edge.RIGHT, 3),
            left = ArrowCursor(Edge.LEFT, 2), right = ArrowCursor(Edge.LEFT, 2),
        )
        assertMoves(
            ring, ArrowCursor(Edge.RIGHT, 3),
            up = ArrowCursor(Edge.RIGHT, 2), down = ArrowCursor(Edge.BOTTOM, 3),
            left = ArrowCursor(Edge.LEFT, 3), right = ArrowCursor(Edge.LEFT, 3),
        )
    }

    @Test
    fun `every TOP-edge cursor moves per the table, for squareSide 4`() {
        val ring = ArrowRing(squareSide = 4)

        assertMoves(
            ring, ArrowCursor(Edge.TOP, 0),
            up = ArrowCursor(Edge.BOTTOM, 0), down = ArrowCursor(Edge.BOTTOM, 0),
            left = ArrowCursor(Edge.LEFT, 0), right = ArrowCursor(Edge.TOP, 1),
        )
        assertMoves(
            ring, ArrowCursor(Edge.TOP, 1),
            up = ArrowCursor(Edge.BOTTOM, 1), down = ArrowCursor(Edge.BOTTOM, 1),
            left = ArrowCursor(Edge.TOP, 0), right = ArrowCursor(Edge.TOP, 2),
        )
        assertMoves(
            ring, ArrowCursor(Edge.TOP, 2),
            up = ArrowCursor(Edge.BOTTOM, 2), down = ArrowCursor(Edge.BOTTOM, 2),
            left = ArrowCursor(Edge.TOP, 1), right = ArrowCursor(Edge.TOP, 3),
        )
        assertMoves(
            ring, ArrowCursor(Edge.TOP, 3),
            up = ArrowCursor(Edge.BOTTOM, 3), down = ArrowCursor(Edge.BOTTOM, 3),
            left = ArrowCursor(Edge.TOP, 2), right = ArrowCursor(Edge.RIGHT, 0),
        )
    }

    @Test
    fun `every BOTTOM-edge cursor moves per the table, for squareSide 4`() {
        val ring = ArrowRing(squareSide = 4)

        assertMoves(
            ring, ArrowCursor(Edge.BOTTOM, 0),
            up = ArrowCursor(Edge.TOP, 0), down = ArrowCursor(Edge.TOP, 0),
            left = ArrowCursor(Edge.LEFT, 3), right = ArrowCursor(Edge.BOTTOM, 1),
        )
        assertMoves(
            ring, ArrowCursor(Edge.BOTTOM, 1),
            up = ArrowCursor(Edge.TOP, 1), down = ArrowCursor(Edge.TOP, 1),
            left = ArrowCursor(Edge.BOTTOM, 0), right = ArrowCursor(Edge.BOTTOM, 2),
        )
        assertMoves(
            ring, ArrowCursor(Edge.BOTTOM, 2),
            up = ArrowCursor(Edge.TOP, 2), down = ArrowCursor(Edge.TOP, 2),
            left = ArrowCursor(Edge.BOTTOM, 1), right = ArrowCursor(Edge.BOTTOM, 3),
        )
        assertMoves(
            ring, ArrowCursor(Edge.BOTTOM, 3),
            up = ArrowCursor(Edge.TOP, 3), down = ArrowCursor(Edge.TOP, 3),
            left = ArrowCursor(Edge.BOTTOM, 2), right = ArrowCursor(Edge.RIGHT, 3),
        )
    }

    @Test
    fun `squareSide 2 collapses the corner cases onto each other`() {
        val ring = ArrowRing(squareSide = 2)

        assertMoves(
            ring, ArrowCursor(Edge.LEFT, 0),
            up = ArrowCursor(Edge.TOP, 0), down = ArrowCursor(Edge.LEFT, 1),
            left = ArrowCursor(Edge.RIGHT, 0), right = ArrowCursor(Edge.RIGHT, 0),
        )
        assertMoves(
            ring, ArrowCursor(Edge.LEFT, 1),
            up = ArrowCursor(Edge.LEFT, 0), down = ArrowCursor(Edge.BOTTOM, 0),
            left = ArrowCursor(Edge.RIGHT, 1), right = ArrowCursor(Edge.RIGHT, 1),
        )
        assertMoves(
            ring, ArrowCursor(Edge.RIGHT, 0),
            up = ArrowCursor(Edge.TOP, 1), down = ArrowCursor(Edge.RIGHT, 1),
            left = ArrowCursor(Edge.LEFT, 0), right = ArrowCursor(Edge.LEFT, 0),
        )
        assertMoves(
            ring, ArrowCursor(Edge.RIGHT, 1),
            up = ArrowCursor(Edge.RIGHT, 0), down = ArrowCursor(Edge.BOTTOM, 1),
            left = ArrowCursor(Edge.LEFT, 1), right = ArrowCursor(Edge.LEFT, 1),
        )
        assertMoves(
            ring, ArrowCursor(Edge.TOP, 0),
            up = ArrowCursor(Edge.BOTTOM, 0), down = ArrowCursor(Edge.BOTTOM, 0),
            left = ArrowCursor(Edge.LEFT, 0), right = ArrowCursor(Edge.TOP, 1),
        )
        assertMoves(
            ring, ArrowCursor(Edge.TOP, 1),
            up = ArrowCursor(Edge.BOTTOM, 1), down = ArrowCursor(Edge.BOTTOM, 1),
            left = ArrowCursor(Edge.TOP, 0), right = ArrowCursor(Edge.RIGHT, 0),
        )
        assertMoves(
            ring, ArrowCursor(Edge.BOTTOM, 0),
            up = ArrowCursor(Edge.TOP, 0), down = ArrowCursor(Edge.TOP, 0),
            left = ArrowCursor(Edge.LEFT, 1), right = ArrowCursor(Edge.BOTTOM, 1),
        )
        assertMoves(
            ring, ArrowCursor(Edge.BOTTOM, 1),
            up = ArrowCursor(Edge.TOP, 1), down = ArrowCursor(Edge.TOP, 1),
            left = ArrowCursor(Edge.BOTTOM, 0), right = ArrowCursor(Edge.RIGHT, 1),
        )
    }

    @Test
    fun `toHitTarget maps each edge to its matching ShiftLeft-Right-Up-Down with the right index`() {
        val ring = ArrowRing(squareSide = 4)

        assertEquals(HitTarget.ShiftLeft(2), ring.toHitTarget(ArrowCursor(Edge.LEFT, 2)))
        assertEquals(HitTarget.ShiftRight(2), ring.toHitTarget(ArrowCursor(Edge.RIGHT, 2)))
        assertEquals(HitTarget.ShiftUp(2), ring.toHitTarget(ArrowCursor(Edge.TOP, 2)))
        assertEquals(HitTarget.ShiftDown(2), ring.toHitTarget(ArrowCursor(Edge.BOTTOM, 2)))
    }
}
