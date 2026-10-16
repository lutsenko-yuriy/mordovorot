package mordie

/**
 * The opponent for one game: races the player to the shortest solution of the game's
 * original arrangement and says how it's going. Its progress is checked every
 * [CHECK_EVERY] shifts; the line it shows follows the shift count (GH-83).
 *
 * Not thread-safe - called only from the game's event loop. The search itself runs elsewhere
 * behind [MordieSearch].
 */
class Mordie(private val factory: MordieSearchFactory = BackgroundMordieSearch.Factory()) {

    /** Whether any game has been bound, even one [clear]ed of Mordie data. */
    private var bound = false
    private var original: IntArray? = null
    private var search: MordieSearch? = null
    private var opening: MordieLine = MordieLine.Opening
    private var checkedMoves = 0
    private var revealedMoves: Int? = null
    private var outcome: MordieOutcome? = null

    /** Shifts since the game started or was loaded. */
    var shifts = 0
        private set

    /** Whether the search resumed from a save rather than starting fresh. */
    var resumedFromSave = false
        private set

    fun startFresh(arrangement: IntArray) {
        bind(arrangement)
        search = factory.start(original!!, -1)
    }

    /** Starts fresh unless a game is already bound - e.g. after a mode-switch rebuild. */
    fun ensureStarted(arrangement: IntArray) {
        if (!bound) startFresh(arrangement)
    }

    /** A save made while unsolved: Mordie had ruled out [moves], or found a solution that long. */
    fun resume(original: IntArray, moves: Int, found: Boolean) {
        require(moves >= 0) { "Mordie's moves must not be negative" }
        bind(original)
        resumedFromSave = true
        checkedMoves = moves
        if (found) {
            revealedMoves = moves
        } else {
            opening = MordieLine.Resumed(moves)
            search = factory.start(this.original!!, moves)
        }
    }

    /** A save made after solving: the result is already decided, so nothing searches. */
    fun restoreSolved(original: IntArray, moves: Int, found: Boolean) {
        require(moves >= 0) { "Mordie's moves must not be negative" }
        bind(original)
        outcome = MordieOutcome(moves, found)
    }

    /** A game Mordie sits out, e.g. a solved save with no Mordie data. */
    fun clear() {
        bind(null)
    }

    /** Counts a shift; returns the progress when this shift's check reveals the solution. */
    fun onShift(): SearchProgress? {
        if (original == null || outcome != null) return null
        shifts++
        if (revealedMoves != null || shifts % CHECK_EVERY != 0) return null
        val progress = search!!.progress()
        checkedMoves = progress.moves.coerceAtLeast(0)
        if (!progress.found) return null
        revealedMoves = progress.moves
        return progress
    }

    fun line(): MordieLine? {
        if (original == null || outcome != null) return null
        revealedMoves?.let { return MordieLine.Found(it) }
        return when {
            shifts < CHECK_EVERY -> opening
            shifts % PROGRESS_EVERY < CHECK_EVERY -> MordieLine.Progress(checkedMoves, hard = shifts >= HARD_FROM)
            else -> null
        }
    }

    /** Freezes Mordie's live state as the result; `null` when Mordie sits this game out. */
    fun onSolved(): MordieOutcome? {
        if (original == null) return null
        outcome?.let { return it }
        return live().also {
            outcome = it
            stop()
        }
    }

    fun outcome(): MordieOutcome? = outcome

    /** What a save should store; `null` when Mordie sits this game out. */
    fun snapshot(): MordieSnapshot? {
        val original = original ?: return null
        val state = outcome ?: live()
        return MordieSnapshot(original.copyOf(), state.moves, state.found)
    }

    /** Cancels the search; everything else is kept. */
    fun stop() {
        search?.cancel()
    }

    private fun bind(arrangement: IntArray?) {
        stop()
        bound = true
        original = arrangement?.copyOf()
        search = null
        opening = MordieLine.Opening
        checkedMoves = 0
        revealedMoves = null
        outcome = null
        shifts = 0
        resumedFromSave = false
    }

    /** The search's current state - not just the last check's. */
    private fun live(): MordieOutcome {
        revealedMoves?.let { return MordieOutcome(it, found = true) }
        val progress = search?.progress() ?: return MordieOutcome(checkedMoves, found = false)
        return MordieOutcome(progress.moves.coerceAtLeast(0), progress.found)
    }

    private companion object {
        const val CHECK_EVERY = 5
        const val PROGRESS_EVERY = 20
        const val HARD_FROM = 60
    }
}
