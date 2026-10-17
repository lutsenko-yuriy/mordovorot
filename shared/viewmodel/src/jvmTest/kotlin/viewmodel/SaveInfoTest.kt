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
}
