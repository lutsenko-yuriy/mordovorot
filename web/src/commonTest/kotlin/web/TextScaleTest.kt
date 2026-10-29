package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** GH-168: the page's text scale from the browser's default font size and iOS's Dynamic Type body size. */
class TextScaleTest {

    @Test
    fun `the standard sizes are 1`() {
        assertEquals(1.0, textScaleOf(16.0, null))
        assertEquals(1.0, textScaleOf(16.0, 17.0))
    }

    @Test
    fun `a smaller default is 1, so text never shrinks`() {
        assertEquals(1.0, textScaleOf(12.0, null))
        assertEquals(1.0, textScaleOf(16.0, 14.0))
    }

    @Test
    fun `a larger default scales by its ratio to 16px`() {
        assertEquals(1.25, textScaleOf(20.0, null))
    }

    @Test
    fun `a larger Dynamic Type body scales by its ratio to 17px`() {
        assertEquals(23.0 / 17, textScaleOf(16.0, 23.0))
    }

    @Test
    fun `with both larger, the larger ratio wins`() {
        assertEquals(1.5, textScaleOf(24.0, 17.0 * 1.2))
        assertEquals(2.0, textScaleOf(20.0, 34.0))
    }

    @Test
    fun `missing or broken readings count as standard`() {
        for (bad in listOf(null, 0.0, -4.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals(1.0, textScaleOf(bad, bad), "$bad")
            assertEquals(1.25, textScaleOf(20.0, bad), "$bad")
        }
    }

    @Test
    fun `the root's font size is the standard 16px times the scale`() {
        assertEquals(23.0, rootFontPx(16.0, 23.0 / 16))
    }

    @Test
    fun `a smaller default is raised to 16px`() {
        assertEquals(16.0, rootFontPx(12.0, 1.0))
    }

    /** Set anyway, Firefox's text zoom, if the default carries it, would apply to it a second time. */
    @Test
    fun `a default that is already the size is left alone`() {
        assertNull(rootFontPx(16.0, 1.0))
        assertNull(rootFontPx(24.0, 1.5))
        assertNull(rootFontPx(24.004, 1.5))
    }

    @Test
    fun `a missing default is set`() {
        assertEquals(24.0, rootFontPx(null, 1.5))
    }
}
