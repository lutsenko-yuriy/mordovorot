package web.qr

import kotlin.test.Test
import kotlin.test.assertEquals

class QrSvgTest {

    private val code = QrCode.encode("hello")

    @Test
    fun `the view box adds a quiet zone of 4 modules on every side`() {
        assertEquals(21 + 8, QrSvg.extent(code))
        assertEquals("0 0 29 29", QrSvg.viewBox(code))
    }

    @Test
    fun `the path covers exactly the dark modules`() {
        val drawn = Array(code.size) { BooleanArray(code.size) }
        Regex("M(\\d+) (\\d+)h(\\d+)v1h-(\\d+)z").findAll(QrSvg.path(code)).forEach { match ->
            val (x, y, width, back) = match.destructured
            assertEquals(width, back)
            for (i in 0 until width.toInt()) {
                val column = x.toInt() - QrSvg.QUIET_ZONE + i
                val row = y.toInt() - QrSvg.QUIET_ZONE
                assertEquals(false, drawn[row][column], "module $column,$row drawn twice")
                drawn[row][column] = true
            }
        }
        for (y in 0 until code.size) for (x in 0 until code.size) assertEquals(code.isDark(x, y), drawn[y][x], "module $x,$y")
    }
}
