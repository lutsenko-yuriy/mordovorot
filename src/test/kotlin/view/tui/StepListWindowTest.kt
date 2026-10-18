package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-126: the scroll window over the solution screen's step list. */
class StepListWindowTest {

    private val window = StepListWindow()

    @Test
    fun `scrolling is clamped to the list`() {
        window.update(total = 20, visible = 9, current = null)

        window.scroll(-1)
        assertEquals(0, window.offset)
        window.scroll(5)
        assertEquals(5, window.offset)
        window.scroll(100)
        assertEquals(11, window.offset)
    }

    @Test
    fun `a list that fits never scrolls`() {
        window.update(total = 7, visible = 9, current = null)

        window.scroll(3)

        assertEquals(0, window.offset)
    }

    @Test
    fun `a new current step is brought into view, at the bottom going down and the top going up`() {
        window.update(total = 20, visible = 9, current = 8)
        assertEquals(0, window.offset)

        window.update(total = 20, visible = 9, current = 9)
        assertEquals(1, window.offset)

        window.update(total = 20, visible = 9, current = 0)
        assertEquals(0, window.offset)
    }

    @Test
    fun `scrolling away from the current step holds until the step changes`() {
        window.update(total = 20, visible = 9, current = 3)
        window.scroll(10)

        window.update(total = 20, visible = 9, current = 3)
        assertEquals(10, window.offset)

        window.update(total = 20, visible = 9, current = 4)
        assertEquals(4, window.offset)
    }

    @Test
    fun `a shorter list or a taller window pulls the offset back in`() {
        window.update(total = 20, visible = 9, current = null)
        window.scroll(11)

        window.update(total = 12, visible = 9, current = null)

        assertEquals(3, window.offset)
    }

    @Test
    fun `reset goes back to the top`() {
        window.update(total = 20, visible = 9, current = 15)

        window.reset()

        assertEquals(0, window.offset)
    }
}
