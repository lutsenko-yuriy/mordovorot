package mordie

/** What Mordie says under `Moves:` - the words live in `viewmodel.MordieText`. */
sealed interface MordieLine {
    /** Shifts 0-4 of a fresh game. */
    data object Opening : MordieLine

    /** Shifts 0-4 of a [side]x[side] game too big for Mordie's search (GH-160). */
    data class SitsOut(val side: Int) : MordieLine

    /** Shifts 0-4 after loading a save Mordie was still searching in, at [moves]. */
    data class Resumed(val moves: Int) : MordieLine

    /** Shifts 20-24, 40-44, ...: [moves] ruled out as of that window's check; [hard] from shift 60. */
    data class Progress(val moves: Int, val hard: Boolean) : MordieLine

    /** From the check that revealed a solution proven shortest until the board is solved. */
    data class Found(val moves: Int) : MordieLine

    /** From the check that revealed a solution not proven shortest (GH-223), until a shorter one. */
    data class FoundUnproven(val moves: Int) : MordieLine

    /** From the check that saw a shorter solution (GH-223), until the next drop or the solve. */
    data class Shorter(val moves: Int) : MordieLine
}

/** What one 5-shift check found worth telling (GH-223). */
sealed interface MordieCheck {
    /** The first solution, [progress] being what the search had. */
    data class Revealed(val progress: SearchProgress) : MordieCheck

    /** The exact search beat the answer told so far: N went from [previous] down to [moves]. */
    data class Shortened(val moves: Int, val previous: Int, val provenShortest: Boolean) : MordieCheck
}

/** Mordie's state when the board was solved: [moves] is its solution's length if [found],
 *  else the largest length it had ruled out. [provenShortest] says whether a found solution is
 *  the minimum - it may be the fast solver's longer one (GH-222). */
data class MordieOutcome(val moves: Int, val found: Boolean, val provenShortest: Boolean = found) {

    /** Fewer moves than a proven [moves] can only come from an inconsistent (e.g. hand-edited) save;
     *  fewer than an unproven one is a win over a longer answer (GH-223). */
    fun result(playerMoves: Int): MordieResult = when {
        playerMoves < moves && found && !provenShortest -> MordieResult.BEAT_MORDIE
        playerMoves < moves -> MordieResult.MACHINE
        !found -> MordieResult.PLAYER_WON
        playerMoves == moves -> MordieResult.MATCHED
        else -> MordieResult.MORDIE_WON
    }
}

enum class MordieResult { PLAYER_WON, MATCHED, MORDIE_WON, BEAT_MORDIE, MACHINE }

/** What a save stores about Mordie; [solution] is its path when known (GH-126). */
class MordieSnapshot(val original: IntArray, val moves: Int, val found: Boolean, val solution: List<SolutionMove>?)
