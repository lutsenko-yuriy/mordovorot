package texts

/** Mordie's words (GH-83), after the `Mordie:` prefix the view adds. Counts are move counts. */
interface MordieTexts {
    val opening: String
    fun resumed(moves: Int): String
    fun friendSolving(moves: Int): String
    fun friendSolved(moves: Int): String
    val friendGaveUp: String
    fun friendGaveUpAfter(moves: Int): String
    fun progress(moves: Int): String
    fun progressHard(moves: Int): String
    fun found(moves: Int): String
    fun foundUnproven(moves: Int): String
    fun shorter(moves: Int): String

    /** The solved title's endings, [mine] being Mordie's moves and [yours] the player's. */
    fun endingPlayerWon(mine: Int): String
    fun endingMatched(mine: Int): String
    fun endingMordieWon(mine: Int, yours: Int): String
    fun endingBeatUnproven(mine: Int, yours: Int): String
    fun endingMachine(mine: Int, yours: Int): String

    /** The solution screen's line: the player's result sentence, then [solutionHere]. */
    fun solutionSolved(moves: Int): String
    val solutionNotStarted: String
    fun solutionStopped(moves: Int): String
    fun solutionHere(length: Int): String
}
