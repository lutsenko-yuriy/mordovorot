package web.tutorial

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import viewmodel.TileLabels
import web.drag.Axis

/** GH-288: Mordie's texts, for a computer and a phone, under each label scheme. */
class TutorialTextTest {

    private fun text(step: Int, computer: Boolean = true, labels: TileLabels = TileLabels.NUMBERS) = TutorialText.step(step, computer, labels)

    @Test
    fun `a computer is offered the arrows too`() {
        assertEquals("Hi there! I'm Mordie. Let's play a short game. Drag the top row one tile left, or click its ◀.", text(1))
        assertEquals(
            "Lines go round: see tile 14 at the top of the second column? Shift that column one tile up, by dragging or with ▲, and 14 comes back in at the bottom.",
            text(2),
        )
        assertEquals("A move is one row or column shifted, however far. Drag the third row two tiles right, or click its ▶ twice: still 1 move.", text(3))
        assertEquals("Last one: drag the fourth column one tile down, or click its ▼. That solves it!", text(4))
    }

    @Test
    fun `a phone is told to drag only`() {
        assertEquals("Hi there! I'm Mordie. Let's play a short game. Drag the top row one tile left.", text(1, computer = false))
        assertEquals(
            "Lines go round: see tile 14 at the top of the second column? Drag that column one tile up, and 14 comes back in at the bottom.",
            text(2, computer = false),
        )
        assertEquals("A move is one row or column shifted, however far. Drag the third row two tiles right: still 1 move.", text(3, computer = false))
        assertEquals("Last one: drag the fourth column one tile down. That solves it!", text(4, computer = false))
        (1..4).forEach { step -> listOf("◀", "▶", "▲", "▼").forEach { assertFalse(it in text(step, computer = false)) } }
    }

    @Test
    fun `step 2 names the tile by the labels in use`() {
        assertTrue("see tile N at" in text(2, labels = TileLabels.LETTERS))
        assertTrue("N comes back" in text(2, labels = TileLabels.LETTERS))
        assertTrue("see tile 42 at" in text(2, labels = TileLabels.PAIRS))
        assertTrue("see tile 13 at" in text(2, labels = TileLabels.NUMBERS_FROM_0))
    }

    @Test
    fun `step 5 challenges the player`() {
        assertEquals("Can you beat my results?", text(5))
    }

    @Test
    fun `step 3 once more says the live move count`() {
        assertEquals("Once more! It's the same row, so Moves stays at 3.", TutorialText.again(3))
    }

    @Test
    fun `the blocked line names a row or a column`() {
        assertEquals("You can do that later. For now shift that row please!", TutorialText.blocked(Axis.ROW))
        assertEquals("You can do that later. For now shift that column please!", TutorialText.blocked(Axis.COLUMN))
    }
}
