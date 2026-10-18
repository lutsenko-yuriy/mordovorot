package web.drag

import kotlin.test.Test
import kotlin.test.assertEquals

class WrapPositionsTest {

    @Test
    fun `no offset leaves every tile in place`() {
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0), WrapPositions.of(side = 4, offset = 0.0))
    }

    @Test
    fun `a positive offset moves the tiles right, and the last wraps to the start`() {
        assertEquals(listOf(1.0, 2.0, 3.0, 0.0), WrapPositions.of(side = 4, offset = 1.0))
    }

    @Test
    fun `a fractional offset leaves the last tile straddling the end`() {
        assertEquals(listOf(0.5, 1.5, 2.5, 3.5), WrapPositions.of(side = 4, offset = 0.5))
    }

    @Test
    fun `a negative offset wraps the first tile to the end`() {
        assertEquals(listOf(3.5, 0.5, 1.5, 2.5), WrapPositions.of(side = 4, offset = -0.5))
    }

    @Test
    fun `offsets of a loop or more wrap all the way round`() {
        assertEquals(WrapPositions.of(side = 3, offset = 0.25), WrapPositions.of(side = 3, offset = 6.25))
        assertEquals(WrapPositions.of(side = 3, offset = 0.25), WrapPositions.of(side = 3, offset = -2.75))
    }
}
