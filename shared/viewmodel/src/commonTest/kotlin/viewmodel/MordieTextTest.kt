package viewmodel

import mordie.MordieLine
import mordie.MordieOutcome
import kotlin.test.Test
import kotlin.test.assertEquals

class MordieTextTest {

    @Test
    fun `the opening lines`() {
        assertEquals("Alright! Let's sort this one out!", MordieText.line(MordieLine.Opening))
        assertEquals(
            "Okay, so we stopped when I was at 8 moves! What was I doing?..",
            MordieText.line(MordieLine.Resumed(8)),
        )
    }

    @Test
    fun `the sit-out line (GH-160)`() {
        assertEquals("6x6 is too much for me, you're on your own!", MordieText.line(MordieLine.SitsOut(6)))
    }

    @Test
    fun `the line a layout reserves for a board Mordie sits out, and none below MAX_SIDE (GH-160)`() {
        assertEquals("6x6 is too much for me, you're on your own!", MordieText.sitsOutLine(6))
        assertEquals(null, MordieText.sitsOutLine(5))
    }

    @Test
    fun `the progress line, plain and hard`() {
        assertEquals(
            "I have already made 6 moves and have not solved it yet!",
            MordieText.line(MordieLine.Progress(6, hard = false)),
        )
        assertEquals(
            "This one is hard! I have already made 9 moves and have not solved it yet!",
            MordieText.line(MordieLine.Progress(9, hard = true)),
        )
    }

    @Test
    fun `the found line`() {
        assertEquals(
            "Found it! I had to make 11 moves to find the solution! Can you beat it?",
            MordieText.line(MordieLine.Found(11)),
        )
    }

    @Test
    fun `every line uses the singular for 1 move`() {
        assertEquals(
            "Okay, so we stopped when I was at 1 move! What was I doing?..",
            MordieText.line(MordieLine.Resumed(1)),
        )
        assertEquals(
            "I have already made 1 move and have not solved it yet!",
            MordieText.line(MordieLine.Progress(1, hard = false)),
        )
        assertEquals(
            "Found it! I had to make 1 move to find the solution! Can you beat it?",
            MordieText.line(MordieLine.Found(1)),
        )
    }

    @Test
    fun `the four endings`() {
        assertEquals("You beat me! I was still at 4 moves.", MordieText.ending(MordieOutcome(4, found = false), 6))
        assertEquals("Cool, you matched me! 3 moves each!", MordieText.ending(MordieOutcome(3, found = true), 3))
        assertEquals("Sorry, but I win: 3 moves vs your 7.", MordieText.ending(MordieOutcome(3, found = true), 7))
        assertEquals(
            "I made 5 moves and you made 2 moves! Are you a machine?",
            MordieText.ending(MordieOutcome(5, found = true), 2),
        )
    }

    @Test
    fun `every ending uses the singular for 1 move`() {
        assertEquals("You beat me! I was still at 1 move.", MordieText.ending(MordieOutcome(1, found = false), 3))
        assertEquals("Cool, you matched me! 1 move each!", MordieText.ending(MordieOutcome(1, found = true), 1))
        assertEquals("Sorry, but I win: 1 move vs your 3.", MordieText.ending(MordieOutcome(1, found = true), 3))
        assertEquals(
            "I made 5 moves and you made 1 move! Are you a machine?",
            MordieText.ending(MordieOutcome(5, found = false), 1),
        )
    }

    @Test
    fun `the solution screen's line, GH-126`() {
        assertEquals("Here's how I did it in 7 moves!", MordieText.solution(7))
        assertEquals("Here's how I did it in 1 move!", MordieText.solution(1))
    }
}
