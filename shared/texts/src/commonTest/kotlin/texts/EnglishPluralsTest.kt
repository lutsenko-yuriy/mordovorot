package texts

import kotlin.test.Test
import kotlin.test.assertEquals

/** English's one/other plurals (GH-274): what a language with more forms replaces. */
class EnglishPluralsTest {

    private val texts = texts()

    @Test
    fun `moves have a singular only at one`() {
        assertEquals(listOf("0 moves", "1 move", "2 moves", "5 moves"), listOf(0, 1, 2, 5).map(texts::moves))
    }

    @Test
    fun `tiles in place have a singular only at one`() {
        assertEquals(
            listOf("0 tiles in place.", "1 tile in place.", "2 tiles in place."),
            listOf(0, 1, 2).map(texts.reader::tilesInPlace),
        )
    }

    @Test
    fun `the tutorial counts one to three in words and repeats in times`() {
        val tutorial = texts.tutorial
        assertEquals(
            listOf("Drag it left", "Drag it two tiles left", "Drag it three tiles left"),
            listOf(1, 2, 3).map(tutorial::dragLeft),
        )
        assertEquals(
            listOf("Click its ◀", "Click its ◀ twice", "Click its ◀ three times"),
            listOf(1, 2, 3).map { tutorial.click("◀", it) },
        )
    }

    @Test
    fun `a board size names both sides`() {
        assertEquals("4x4", texts.boardSize(4))
    }
}
