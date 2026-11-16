package viewmodel

import mordie.Mordie
import mordie.MordieLine
import mordie.MordieOutcome
import mordie.MordieResult
import texts.texts

/** Mordie's words (GH-83), picked from `:shared:texts` (GH-274). [line] leaves out the `Mordie:` prefix - the view colors it. */
object MordieText {

    fun line(line: MordieLine): String = with(texts().mordie) {
        when (line) {
            MordieLine.Opening -> opening
            is MordieLine.Resumed -> resumed(line.moves)
            is MordieLine.FriendSolving -> friendSolving(line.moves)
            is MordieLine.FriendSolved -> friendSolved(line.moves)
            is MordieLine.FriendGaveUp -> line.moves.let { if (it == null) friendGaveUp else friendGaveUpAfter(it) }
            is MordieLine.Progress -> if (line.hard) progressHard(line.moves) else progress(line.moves)
            is MordieLine.Found -> found(line.moves)
            is MordieLine.FoundUnproven -> foundUnproven(line.moves)
            is MordieLine.Shorter -> shorter(line.moves)
        }
    }

    /** The solved title, given the player's [playerMoves]. */
    fun ending(outcome: MordieOutcome, playerMoves: Int): String = with(texts().mordie) {
        val mine = outcome.moves
        when (outcome.result(playerMoves)) {
            MordieResult.PLAYER_WON -> endingPlayerWon(mine)
            MordieResult.MATCHED -> endingMatched(mine)
            MordieResult.MORDIE_WON -> endingMordieWon(mine, playerMoves)
            MordieResult.BEAT_MORDIE -> endingBeatUnproven(mine, playerMoves)
            MordieResult.MACHINE -> endingMachine(mine, playerMoves)
        }
    }

    /** The solution screen's line (GH-126), Mordie's solution being [length] moves long, led by the player's [result] (GH-273). */
    fun solution(length: Int, result: PlayerResult? = null): String = with(texts().mordie) {
        val here = solutionHere(length)
        when {
            result == null -> here
            !result.gaveUp -> "${solutionSolved(result.moves)} $here"
            result.moves == 0 -> "$solutionNotStarted $here"
            else -> "${solutionStopped(result.moves)} $here"
        }
    }

    /** `1 move`, `2 moves`. */
    fun moves(count: Int) = texts().moves(count)
}
