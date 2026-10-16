package viewmodel

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
    }

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

    private fun moves(count: Int) = if (count == 1) "1 move" else "$count moves"
}
