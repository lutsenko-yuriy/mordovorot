package mordie

/** What Mordie says under `Moves:` - the words live in `viewmodel.MordieText`. */
sealed interface MordieLine {
    /** Shifts 0-4 of a fresh game. */
    data object Opening : MordieLine

    /** Shifts 0-4 after loading a save Mordie was still searching in, at [moves]. */
    data class Resumed(val moves: Int) : MordieLine

    /** Shifts 20-24, 40-44, ...: [moves] ruled out as of that window's check; [hard] from shift 60. */
    data class Progress(val moves: Int, val hard: Boolean) : MordieLine

    /** From the check that revealed Mordie's solution until the board is solved. */
    data class Found(val moves: Int) : MordieLine
}

/** Mordie's state when the board was solved: [moves] is its solution's length if [found],
 *  else the largest length it had ruled out. */
data class MordieOutcome(val moves: Int, val found: Boolean) {

    /** Fewer moves than [moves] can only come from an inconsistent (e.g. hand-edited) save. */
    fun result(playerMoves: Int): MordieResult = when {
        playerMoves < moves -> MordieResult.MACHINE
        !found -> MordieResult.PLAYER_WON
        playerMoves == moves -> MordieResult.MATCHED
        else -> MordieResult.MORDIE_WON
    }
}

enum class MordieResult { PLAYER_WON, MATCHED, MORDIE_WON, MACHINE }

/** What a save stores about Mordie. */
class MordieSnapshot(val original: IntArray, val moves: Int, val found: Boolean)
