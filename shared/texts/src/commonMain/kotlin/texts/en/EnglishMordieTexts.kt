package texts.en

import texts.MordieTexts

object EnglishMordieTexts : MordieTexts {
    override val opening = "Alright! Let's sort this one out!"
    override fun resumed(moves: Int) = "Okay, so we stopped when I was at ${englishMoves(moves)}! What was I doing?.."
    override fun friendSolving(moves: Int) =
        "Okay, so your friend made ${englishMoves(moves)} and did not solve the puzzle yet. Maybe we could help?"
    override fun friendSolved(moves: Int) = "Okay, so your friend solved this one in ${englishMoves(moves)}. Wanna beat this?"
    override val friendGaveUp = "So your friend gave up before finding the solution. Can we find one?"
    override fun friendGaveUpAfter(moves: Int) =
        "So after making ${englishMoves(moves)} your friend gave up before finding the solution. Can we find one?"
    override fun progress(moves: Int) = "I have already made ${englishMoves(moves)} and have not solved it yet!"
    override fun progressHard(moves: Int) = "This one is hard! " + progress(moves)
    override fun found(moves: Int) = "Found it! I had to make ${englishMoves(moves)} to find the solution! Can you beat it?"
    override fun foundUnproven(moves: Int) = "Found one in ${englishMoves(moves)}, but maybe there's a shorter one! Can you beat it?"
    override fun shorter(moves: Int) = "Wait, I found a shorter one: ${englishMoves(moves)} now! Can you beat it?"

    override fun endingPlayerWon(mine: Int) = "You beat me! I was still at ${englishMoves(mine)}."
    override fun endingMatched(mine: Int) = "Cool, you matched me! ${englishMoves(mine)} each!"
    override fun endingMordieWon(mine: Int, yours: Int) = "Sorry, but I win: ${englishMoves(mine)} vs your $yours."
    override fun endingBeatUnproven(mine: Int, yours: Int) =
        "You beat me! ${englishMoves(mine)} vs your $yours. Mine may not have been the shortest."
    override fun endingMachine(mine: Int, yours: Int) =
        "I made ${englishMoves(mine)} and you made ${englishMoves(yours)}! Are you a machine?"

    override fun solutionSolved(moves: Int) = "You solved it in ${englishMoves(moves)}."
    override val solutionNotStarted = "You did not even start this one."
    override fun solutionStopped(moves: Int) = "You gave up after ${englishMoves(moves)}."
    override fun solutionHere(length: Int) = "Here's how I did it in ${englishMoves(length)}!"
}
