package web.tutorial

import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import viewmodel.TileLabels
import web.drag.Axis

/** GH-288: Mordie's texts, for a computer and a phone, under each label scheme. */
class TutorialTextTest {
    private val tutorial = texts().tutorial


    private fun text(step: Int, computer: Boolean = true, labels: TileLabels = TileLabels.NUMBERS) = TutorialText.step(step, computer, labels)

    @Test
    fun `a computer gets a lead sentence, the ways follow on their own lines (GH-290)`() {
        assertEquals(tutorial.step1(computer = true), text(1))
        assertEquals(
            tutorial.step2("14", computer = true),
            text(2),
        )
        assertEquals(tutorial.step3(computer = true), text(3))
        assertEquals(tutorial.step4(computer = true), text(4))
    }

    @Test
    fun `a phone is told to drag only`() {
        assertEquals(tutorial.step1(computer = false), text(1, computer = false))
        assertEquals(
            tutorial.step2("14", computer = false),
            text(2, computer = false),
        )
        assertEquals(tutorial.step3(computer = false), text(3, computer = false))
        assertEquals(tutorial.step4(computer = false), text(4, computer = false))
        (1..4).forEach { step -> listOf("◀", "▶", "▲", "▼").forEach { assertFalse(it in text(step, computer = false)) } }
    }

    @Test
    fun `step 2 names the tile by the labels in use`() {
        assertEquals(tutorial.step2("N", computer = true), text(2, labels = TileLabels.LETTERS))
        assertEquals(tutorial.step2("42", computer = true), text(2, labels = TileLabels.PAIRS))
        assertEquals(tutorial.step2("13", computer = true), text(2, labels = TileLabels.NUMBERS_FROM_0))
    }

    @Test
    fun `step 5 challenges the player`() {
        assertEquals(tutorial.step5, text(5))
    }

    @Test
    fun `step 3 once more says the live move count`() {
        assertEquals(tutorial.again(3), TutorialText.again(3))
    }

    @Test
    fun `the blocked line names a row or a column`() {
        assertEquals(tutorial.blockedRow, TutorialText.blocked(Axis.ROW))
        assertEquals(tutorial.blockedColumn, TutorialText.blocked(Axis.COLUMN))
    }
}
