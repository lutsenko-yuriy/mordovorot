package web

import kotlin.test.Test
import kotlin.test.assertEquals

class LayoutTest {

    @Test
    fun `wider than tall is wide`() {
        assertEquals(Layout.WIDE, layoutOf(1200, 800))
    }

    @Test
    fun `taller than wide is tall`() {
        assertEquals(Layout.TALL, layoutOf(400, 800))
    }

    @Test
    fun `square is tall`() {
        assertEquals(Layout.TALL, layoutOf(800, 800))
    }
}
