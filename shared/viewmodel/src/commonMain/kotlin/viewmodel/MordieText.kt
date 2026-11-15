package viewmodel

import mordie.Mordie
import mordie.MordieLine
import mordie.MordieOutcome
import mordie.MordieResult

/** Mordie's words (GH-83). [line] leaves out the `Mordie:` prefix - the view colors it. */
object MordieText {

    fun line(line: MordieLine): String = when (line) {
        MordieLine.Opening -> "Alright! Let's sort this one out!"
        is MordieLine.Resumed -> "Okay, so we stopped when I was at ${moves(line.moves)}! What was I doing?.."
        is MordieLine.Progress ->
            (if (line.hard) "This one is hard! " else "") +
                "I have already made ${moves(line.moves)} and have not solved it yet!"
        is MordieLine.Found -> "Found it! I had to make ${moves(line.moves)} to find the solution! Can you beat it?"
        is MordieLine.FoundUnproven -> "Found one in ${moves(line.moves)}, but maybe there's a shorter one! Can you beat it?"
        is MordieLine.Shorter -> "Wait, I found a shorter one: ${moves(line.moves)} now! Can you beat it?"
    }

    /** The solved title, given the player's [playerMoves]. */
    fun ending(outcome: MordieOutcome, playerMoves: Int): String {
        val mine = moves(outcome.moves)
        return when (outcome.result(playerMoves)) {
            MordieResult.PLAYER_WON -> "You beat me! I was still at $mine."
            MordieResult.MATCHED -> "Cool, you matched me! $mine each!"
            MordieResult.MORDIE_WON -> "Sorry, but I win: $mine vs your $playerMoves."
            MordieResult.BEAT_MORDIE ->
                "You beat me! $mine vs your $playerMoves. Mine may not have been the shortest."
            MordieResult.MACHINE -> "I made $mine and you made ${moves(playerMoves)}! Are you a machine?"
        }
    }

    /** The solution screen's line (GH-126), Mordie's solution being [length] moves long, led by the player's [result] (GH-273). */
    fun solution(length: Int, result: PlayerResult? = null): String = "${resultSentence(result)}Here's how I did it in ${moves(length)}!"

    private fun resultSentence(result: PlayerResult?) = when {
        result == null -> ""
        !result.gaveUp -> "You solved it in ${moves(result.moves)}. "
        result.moves == 0 -> "You did not even start this one. "
        else -> "You gave up after ${moves(result.moves)}. "
    }

    private fun moves(count: Int) = if (count == 1) "1 move" else "$count moves"
}
