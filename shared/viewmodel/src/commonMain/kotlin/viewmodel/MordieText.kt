package viewmodel

import mordie.Mordie
import mordie.MordieLine
import mordie.MordieOutcome
import mordie.MordieResult

/** Mordie's words (GH-83). [line] leaves out the `Mordie:` prefix - the view colors it. */
object MordieText {

    fun line(line: MordieLine): String = when (line) {
        MordieLine.Opening -> "Alright! Let's sort this one out!"
        is MordieLine.SitsOut -> "${line.side}x${line.side} is too much for me, you're on your own!"
        is MordieLine.Resumed -> "Okay, so we stopped when I was at ${moves(line.moves)}! What was I doing?.."
        is MordieLine.Progress ->
            (if (line.hard) "This one is hard! " else "") +
                "I have already made ${moves(line.moves)} and have not solved it yet!"
        is MordieLine.Found -> "Found it! I had to make ${moves(line.moves)} to find the solution! Can you beat it?"
    }

    /** The only line Mordie says on a [side]x[side] board it sits out (GH-160), for the terminal
     *  to reserve rows for; `null` on a board it plays. */
    fun sitsOutLine(side: Int): String? = if (side > Mordie.MAX_SIDE) line(MordieLine.SitsOut(side)) else null

    /** The solved title, given the player's [playerMoves]. */
    fun ending(outcome: MordieOutcome, playerMoves: Int): String {
        val mine = moves(outcome.moves)
        return when (outcome.result(playerMoves)) {
            MordieResult.PLAYER_WON -> "You beat me! I was still at $mine."
            MordieResult.MATCHED -> "Cool, you matched me! $mine each!"
            MordieResult.MORDIE_WON -> "Sorry, but I win: $mine vs your $playerMoves."
            MordieResult.MACHINE -> "I made $mine and you made ${moves(playerMoves)}! Are you a machine?"
        }
    }

    /** The solution screen's line (GH-126), Mordie's solution being [length] moves long. */
    fun solution(length: Int): String = "Here's how I did it in ${moves(length)}!"

    private fun moves(count: Int) = if (count == 1) "1 move" else "$count moves"
}
