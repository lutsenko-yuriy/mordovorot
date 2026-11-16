package viewmodel

import mordie.MordieLine
import mordie.MordieOutcome
import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals

class MordieTextTest {
    private val mordieTexts = texts().mordie

    @Test
    fun `the opening lines`() {
        assertEquals(mordieTexts.opening, MordieText.line(MordieLine.Opening))
        assertEquals(
            mordieTexts.resumed(8),
            MordieText.line(MordieLine.Resumed(8)),
        )
    }

    @Test
    fun `the progress line, plain and hard`() {
        assertEquals(
            mordieTexts.progress(6),
            MordieText.line(MordieLine.Progress(6, hard = false)),
        )
        assertEquals(
            mordieTexts.progressHard(9),
            MordieText.line(MordieLine.Progress(9, hard = true)),
        )
    }

    @Test
    fun `the found line`() {
        assertEquals(
            mordieTexts.found(11),
            MordieText.line(MordieLine.Found(11)),
        )
    }

    @Test
    fun `every line uses the singular for 1 move`() {
        assertEquals(
            mordieTexts.resumed(1),
            MordieText.line(MordieLine.Resumed(1)),
        )
        assertEquals(
            mordieTexts.progress(1),
            MordieText.line(MordieLine.Progress(1, hard = false)),
        )
        assertEquals(
            mordieTexts.found(1),
            MordieText.line(MordieLine.Found(1)),
        )
    }

    @Test
    fun `the four endings`() {
        assertEquals(mordieTexts.endingPlayerWon(4), MordieText.ending(MordieOutcome(4, found = false), 6))
        assertEquals(mordieTexts.endingMatched(3), MordieText.ending(MordieOutcome(3, found = true), 3))
        assertEquals(mordieTexts.endingMordieWon(3, 7), MordieText.ending(MordieOutcome(3, found = true), 7))
        assertEquals(
            mordieTexts.endingMachine(5, 2),
            MordieText.ending(MordieOutcome(5, found = true), 2),
        )
    }

    @Test
    fun `every ending uses the singular for 1 move`() {
        assertEquals(mordieTexts.endingPlayerWon(1), MordieText.ending(MordieOutcome(1, found = false), 3))
        assertEquals(mordieTexts.endingMatched(1), MordieText.ending(MordieOutcome(1, found = true), 1))
        assertEquals(mordieTexts.endingMordieWon(1, 3), MordieText.ending(MordieOutcome(1, found = true), 3))
        assertEquals(
            mordieTexts.endingMachine(5, 1),
            MordieText.ending(MordieOutcome(5, found = false), 1),
        )
    }

    @Test
    fun `the solution screen's line, GH-126`() {
        assertEquals(mordieTexts.solutionHere(7), MordieText.solution(7))
        assertEquals(mordieTexts.solutionHere(1), MordieText.solution(1))
        assertEquals(mordieTexts.solutionHere(7), MordieText.solution(7, null))
        assertEquals("${mordieTexts.solutionStopped(14)} ${mordieTexts.solutionHere(9)}", MordieText.solution(9, PlayerResult(gaveUp = true, moves = 14)))
        assertEquals("${mordieTexts.solutionSolved(14)} ${mordieTexts.solutionHere(9)}", MordieText.solution(9, PlayerResult(gaveUp = false, moves = 14)))
        assertEquals("${mordieTexts.solutionStopped(1)} ${mordieTexts.solutionHere(1)}", MordieText.solution(1, PlayerResult(gaveUp = true, moves = 1)))
        assertEquals("${mordieTexts.solutionSolved(1)} ${mordieTexts.solutionHere(2)}", MordieText.solution(2, PlayerResult(gaveUp = false, moves = 1)))
        assertEquals("${mordieTexts.solutionNotStarted} ${mordieTexts.solutionHere(2)}", MordieText.solution(2, PlayerResult(gaveUp = true, moves = 0)))
    }

    @Test
    fun `the lines for an answer that may not be the shortest (GH-223)`() {
        assertEquals(
            mordieTexts.foundUnproven(40),
            MordieText.line(MordieLine.FoundUnproven(40)),
        )
        assertEquals(
            mordieTexts.shorter(31),
            MordieText.line(MordieLine.Shorter(31)),
        )
        assertEquals(
            mordieTexts.foundUnproven(1),
            MordieText.line(MordieLine.FoundUnproven(1)),
        )
        assertEquals(
            mordieTexts.shorter(1),
            MordieText.line(MordieLine.Shorter(1)),
        )
    }

    @Test
    fun `beating an answer not proven shortest has its own ending (GH-223)`() {
        val unproven = MordieOutcome(40, found = true, provenShortest = false)

        assertEquals(
            mordieTexts.endingBeatUnproven(40, 31),
            MordieText.ending(unproven, 31),
        )
        assertEquals(
            mordieTexts.endingBeatUnproven(1, 0),
            MordieText.ending(MordieOutcome(1, found = true, provenShortest = false), 0),
        )
    }

    @Test
    fun `matching or losing to an unproven answer reads as before (GH-223)`() {
        val unproven = MordieOutcome(40, found = true, provenShortest = false)

        assertEquals(mordieTexts.endingMatched(40), MordieText.ending(unproven, 40))
        assertEquals(mordieTexts.endingMordieWon(40, 45), MordieText.ending(unproven, 45))
    }

    @Test
    fun `the friend lines (GH-272)`() {
        assertEquals(
            mordieTexts.friendSolving(12),
            MordieText.line(MordieLine.FriendSolving(12)),
        )
        assertEquals(mordieTexts.friendSolved(12), MordieText.line(MordieLine.FriendSolved(12)))
        assertEquals(
            mordieTexts.friendGaveUpAfter(12),
            MordieText.line(MordieLine.FriendGaveUp(12)),
        )
        assertEquals(
            mordieTexts.friendGaveUp,
            MordieText.line(MordieLine.FriendGaveUp(null)),
        )
    }

    @Test
    fun `the friend lines say 1 move`() {
        assertEquals(
            mordieTexts.friendSolving(1),
            MordieText.line(MordieLine.FriendSolving(1)),
        )
        assertEquals(mordieTexts.friendSolved(1), MordieText.line(MordieLine.FriendSolved(1)))
        assertEquals(
            mordieTexts.friendGaveUpAfter(1),
            MordieText.line(MordieLine.FriendGaveUp(1)),
        )
    }
}
