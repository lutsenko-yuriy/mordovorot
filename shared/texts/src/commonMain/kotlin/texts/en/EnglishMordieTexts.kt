package texts.en

import texts.MordieTexts

object EnglishMordieTexts : MordieTexts {
    override val opening = "Alright! Let's sort this one out!"
    override fun resumed(moves: Int) = "Okay, so we stopped when I was at ${englishMoves(moves)}! Where was I?"
    override fun friendSolving(moves: Int) =
        "Okay, so your friend made ${englishMoves(moves)} and hasn't solved it yet. Can we help?"
    override fun friendSolved(moves: Int) = "Okay, so your friend solved this one in ${englishMoves(moves)}. Can you beat it?"
    override val friendGaveUp = "Okay, so your friend gave up before finding the solution. Can we find one?"
    override fun friendGaveUpAfter(moves: Int) =
        "Okay, so your friend gave up after ${englishMoves(moves)}, before finding the solution. Can we find one?"
    override fun progress(moves: Int) = "I've already made ${englishMoves(moves)} and haven't solved it yet!"
    override fun progressHard(moves: Int) = "This one is hard! " + progress(moves)
    override fun found(moves: Int) = "Got it! I found it in ${englishMoves(moves)}! Can you beat it?"
    override fun foundUnproven(moves: Int) = "Found one in ${englishMoves(moves)}, but maybe there's a shorter one! Can you beat it?"
    override fun shorter(moves: Int) = "Wait, I found a shorter one: ${englishMoves(moves)} now! Can you beat it?"

    override fun endingPlayerWon(mine: Int) = "You beat me! I was still at ${englishMoves(mine)}."
    override fun endingMatched(mine: Int) = "Cool, you matched me! ${englishMoves(mine)} each!"
    override fun endingMordieWon(mine: Int, yours: Int) = "Sorry, I beat you this time: ${englishMoves(mine)} vs your ${englishMoves(yours)}."
    override fun endingBeatUnproven(mine: Int, yours: Int) =
        "Wow, you found a shorter one: your ${englishMoves(yours)} vs my ${englishMoves(mine)}! Mine may not have been the shortest."
    override fun endingMachine(mine: Int, yours: Int) =
        "I made ${englishMoves(mine)} and you made ${englishMoves(yours)}! Are you a machine?"

    override fun solutionSolved(moves: Int) = "You solved it in ${englishMoves(moves)}."
    override val solutionNotStarted = "You didn't try this one yet."
    override fun solutionStopped(moves: Int) = "You stopped after ${englishMoves(moves)}."
    override fun solutionHere(length: Int) = "Here's how I did it in ${englishMoves(length)}!"
}
