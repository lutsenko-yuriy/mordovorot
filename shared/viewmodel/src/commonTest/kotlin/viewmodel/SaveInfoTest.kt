package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class SaveInfoTest {

    @Test
    fun `display shows the name and size`() {
        assertEquals("foo (5x5)", SaveInfo("foo", 5).display())
    }

    @Test
    fun `display shows the name alone when the size is unknown`() {
        assertEquals("foo", SaveInfo("foo", null).display())
    }

    @Test
    fun `display marks an unsolvable save`() {
        assertEquals("foo (3x3) unsolvable", SaveInfo("foo", 3, solvable = false).display())
    }

    @Test
    fun `displayWithSize leaves the unsolvable mark out`() {
        assertEquals("foo (3x3)", SaveInfo("foo", 3, solvable = false).displayWithSize())
    }

    @Test
    fun `analyticsState is ok for a readable, solvable save, GH-251`() {
        assertEquals("ok", SaveInfo("foo", 4).analyticsState())
    }

    @Test
    fun `analyticsState is unsolvable for a save no shifts can solve, GH-251`() {
        assertEquals("unsolvable", SaveInfo("foo", 3, solvable = false).analyticsState())
    }

    @Test
    fun `analyticsState is corrupted for a save with no size, GH-251`() {
        assertEquals("corrupted", SaveInfo("foo", null).analyticsState())
    }
}
