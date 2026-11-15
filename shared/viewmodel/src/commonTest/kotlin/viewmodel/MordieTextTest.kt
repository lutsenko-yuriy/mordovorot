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
        assertEquals("Here's how I did it in 7 moves!", MordieText.solution(7, null))
        assertEquals("You gave up after 14 moves. Here's how I did it in 9 moves!", MordieText.solution(9, PlayerResult(gaveUp = true, moves = 14)))
        assertEquals("You solved it in 14 moves. Here's how I did it in 9 moves!", MordieText.solution(9, PlayerResult(gaveUp = false, moves = 14)))
        assertEquals("You gave up after 1 move. Here's how I did it in 1 move!", MordieText.solution(1, PlayerResult(gaveUp = true, moves = 1)))
        assertEquals("You solved it in 1 move. Here's how I did it in 2 moves!", MordieText.solution(2, PlayerResult(gaveUp = false, moves = 1)))
        assertEquals("You did not even start this one. Here's how I did it in 2 moves!", MordieText.solution(2, PlayerResult(gaveUp = true, moves = 0)))
    }

    @Test
    fun `the lines for an answer that may not be the shortest (GH-223)`() {
        assertEquals(
            "Found one in 40 moves, but maybe there's a shorter one! Can you beat it?",
            MordieText.line(MordieLine.FoundUnproven(40)),
        )
        assertEquals(
            "Wait, I found a shorter one: 31 moves now! Can you beat it?",
            MordieText.line(MordieLine.Shorter(31)),
        )
        assertEquals(
            "Found one in 1 move, but maybe there's a shorter one! Can you beat it?",
            MordieText.line(MordieLine.FoundUnproven(1)),
        )
        assertEquals(
            "Wait, I found a shorter one: 1 move now! Can you beat it?",
            MordieText.line(MordieLine.Shorter(1)),
        )
    }

    @Test
    fun `beating an answer not proven shortest has its own ending (GH-223)`() {
        val unproven = MordieOutcome(40, found = true, provenShortest = false)

        assertEquals(
            "You beat me! 40 moves vs your 31. Mine may not have been the shortest.",
            MordieText.ending(unproven, 31),
        )
        assertEquals(
            "You beat me! 1 move vs your 0. Mine may not have been the shortest.",
            MordieText.ending(MordieOutcome(1, found = true, provenShortest = false), 0),
        )
    }

    @Test
    fun `matching or losing to an unproven answer reads as before (GH-223)`() {
        val unproven = MordieOutcome(40, found = true, provenShortest = false)

        assertEquals("Cool, you matched me! 40 moves each!", MordieText.ending(unproven, 40))
        assertEquals("Sorry, but I win: 40 moves vs your 45.", MordieText.ending(unproven, 45))
    }

    @Test
    fun `the friend lines (GH-272)`() {
        assertEquals(
            "Okay, so your friend made 12 moves and did not solve the puzzle yet. Maybe we could help?",
            MordieText.line(MordieLine.FriendSolving(12)),
        )
        assertEquals("Okay, so your friend solved this one in 12 moves. Wanna beat this?", MordieText.line(MordieLine.FriendSolved(12)))
        assertEquals(
            "So after making 12 moves your friend gave up before finding the solution. Can we find one?",
            MordieText.line(MordieLine.FriendGaveUp(12)),
        )
        assertEquals(
            "So your friend gave up before finding the solution. Can we find one?",
            MordieText.line(MordieLine.FriendGaveUp(null)),
        )
    }

    @Test
    fun `the friend lines say 1 move`() {
        assertEquals(
            "Okay, so your friend made 1 move and did not solve the puzzle yet. Maybe we could help?",
            MordieText.line(MordieLine.FriendSolving(1)),
        )
        assertEquals("Okay, so your friend solved this one in 1 move. Wanna beat this?", MordieText.line(MordieLine.FriendSolved(1)))
        assertEquals(
            "So after making 1 move your friend gave up before finding the solution. Can we find one?",
            MordieText.line(MordieLine.FriendGaveUp(1)),
        )
    }
}
