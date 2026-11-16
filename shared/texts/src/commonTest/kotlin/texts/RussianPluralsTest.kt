package texts

import kotlin.test.Test
import kotlin.test.assertEquals
import texts.ru.RussianTexts

/** Russian's one/few/many plurals and its genitive (GH-275). */
class RussianPluralsTest {

    private val texts = RussianTexts

    @Test
    fun `moves take one, few and many`() {
        val counts = listOf(0, 1, 2, 3, 4, 5, 11, 12, 14, 21, 22, 25, 101, 111, 112)
        assertEquals(
            listOf(
                "0 ходов", "1 ход", "2 хода", "3 хода", "4 хода", "5 ходов", "11 ходов", "12 ходов", "14 ходов",
                "21 ход", "22 хода", "25 ходов", "101 ход", "111 ходов", "112 ходов",
            ),
            counts.map(texts::moves),
        )
    }

    @Test
    fun `moves after a preposition that asks for the genitive`() {
        assertEquals(
            listOf("0 ходов", "1 хода", "2 ходов", "5 ходов", "21 хода", "11 ходов"),
            listOf(0, 1, 2, 5, 21, 11).map { texts.dialogs.shareProgressGaveUpAfter(it).substringAfter("после ").removeSuffix(")") },
        )
    }

    @Test
    fun `tiles in place take one, few and many`() {
        assertEquals(
            listOf("0 плиток на месте.", "1 плитка на месте.", "2 плитки на месте.", "5 плиток на месте.", "11 плиток на месте.", "21 плитка на месте."),
            listOf(0, 1, 2, 5, 11, 21).map(texts.reader::tilesInPlace),
        )
    }

    @Test
    fun `the tutorial counts two and three tiles in words and repeats in times`() {
        val tutorial = texts.tutorial
        assertEquals(
            listOf("Перетащи влево", "Перетащи на две плитки влево", "Перетащи на три плитки влево"),
            listOf(1, 2, 3).map(tutorial::dragLeft),
        )
        assertEquals(
            listOf("Нажми на ◀", "Нажми на ◀ дважды", "Нажми на ◀ три раза"),
            listOf(1, 2, 3).map { tutorial.click("◀", it) },
        )
    }

    @Test
    fun `the tutorial's counts beyond three take the plural too`() {
        val tutorial = texts.tutorial
        assertEquals(
            listOf("Перетащи на 5 плиток влево", "Перетащи на 21 плитку влево", "Перетащи на 22 плитки влево"),
            listOf(5, 21, 22).map(tutorial::dragLeft),
        )
        assertEquals(
            listOf("Нажми на ◀ 5 раз", "Нажми на ◀ 21 раз", "Нажми на ◀ 22 раза"),
            listOf(5, 21, 22).map { tutorial.click("◀", it) },
        )
    }

    @Test
    fun `a board size names both sides`() {
        assertEquals("4x4", texts.boardSize(4))
    }
}
