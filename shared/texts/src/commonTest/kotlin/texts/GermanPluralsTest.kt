package texts

import kotlin.test.Test
import kotlin.test.assertEquals
import texts.de.GermanTexts

/** German's Zug/Züge plural and its dative after «in» and «nach» (GH-275). */
class GermanPluralsTest {

    private val texts = GermanTexts

    @Test
    fun `moves take Zug and Züge`() {
        assertEquals(
            listOf("0 Züge", "1 Zug", "2 Züge", "5 Züge", "11 Züge", "21 Züge"),
            listOf(0, 1, 2, 5, 11, 21).map(texts::moves),
        )
    }

    @Test
    fun `moves after in and nach take the dative`() {
        assertEquals(
            listOf("0 Zügen", "1 Zug", "2 Zügen", "21 Zügen"),
            listOf(0, 1, 2, 21).map { texts.dialogs.shareProgressGaveUpAfter(it).substringAfter("nach ").removeSuffix(")") },
        )
        assertEquals("Rätsel gelöst in 1 Zug.", texts.mordie.solutionSolved(1))
        assertEquals("Rätsel gelöst in 3 Zügen.", texts.mordie.solutionSolved(3))
    }

    @Test
    fun `moves as an object stay in the plain form`() {
        assertEquals("Ich habe schon 3 Züge gemacht und es noch nicht gelöst!", texts.mordie.progress(3))
    }

    @Test
    fun `tiles in place take Kachel and Kacheln`() {
        assertEquals(
            listOf("0 Kacheln an ihrem Platz.", "1 Kachel an ihrem Platz.", "2 Kacheln an ihrem Platz."),
            listOf(0, 1, 2).map(texts.reader::tilesInPlace),
        )
    }

    @Test
    fun `the tutorial counts two and three tiles in words and repeats in times`() {
        val tutorial = texts.tutorial
        assertEquals(
            listOf("Ziehe sie nach links", "Ziehe sie um zwei Kacheln nach links", "Ziehe sie um drei Kacheln nach links", "Ziehe sie um 5 Kacheln nach links"),
            listOf(1, 2, 3, 5).map(tutorial::dragLeft),
        )
        assertEquals(
            listOf("Klicke auf ◀", "Klicke auf ◀ zweimal", "Klicke auf ◀ dreimal", "Klicke auf ◀ 5-mal"),
            listOf(1, 2, 3, 5).map { tutorial.click("◀", it) },
        )
    }

    @Test
    fun `a board size names both sides`() {
        assertEquals("4x4", texts.boardSize(4))
    }
}
