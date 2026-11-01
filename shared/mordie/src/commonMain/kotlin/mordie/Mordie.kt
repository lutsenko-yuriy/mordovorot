package mordie

/**
 * The opponent for one game: races the player to the shortest solution of the game's
 * original arrangement and says how it's going. Its progress is checked every
 * [CHECK_EVERY] shifts; the line it shows follows the shift count (GH-83).
 *
 * Not thread-safe - called only from the game's event loop. The search itself runs elsewhere
 * behind [MordieSearch].
 */
class Mordie(private val factory: MordieSearchFactory) {

    /** Whether any game has been bound, even one [clear]ed of Mordie data. */
    private var bound = false
    private var original: IntArray? = null

    /** A game too big to search, until it's solved: only shifts are counted, for the
     *  [MordieLine.SitsOut] line. */
    private var sitsOut = false

    /** The side of the game being sat out, for its line. */
    private var satOutSide = 0
    private var search: MordieSearch? = null
    private var opening: MordieLine = MordieLine.Opening
    private var checkedMoves = 0
    private var revealedMoves: Int? = null

    /** Whether the revealed answer is proven shortest (GH-222); the checks go on until it is. */
    var provenShortest = false
        private set

    /** Set by [giveUp]: the solution screen shows the answer as found, proven or not. */
    private var givenUp = false

    /** The last N Mordie told the player - its opening's, or its last progress line's. */
    private var toldMoves = 0

    /** Whether this progress window has news; set at its first check. */
    private var progressWindowShown = false
    private var outcome: MordieOutcome? = null

    /** Shifts since the game started or was loaded. */
    var shifts = 0
        private set

    /** Mordie's solution, once revealed or solved past - `null` until then, or when the search
     *  or the save had no path (GH-126). */
    var solution: List<SolutionMove>? = null
        private set

    /** [shifts] and the player's moves at the check that revealed the solution; `null` before
     *  it, and after loading a save that was already found. */
    var shiftsAtReveal: Int? = null
        private set
    var playerMovesAtReveal: Int? = null
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

    /** [sitOut] unless a game is already bound - e.g. after a mode-switch rebuild. */
    fun ensureSatOut(side: Int) {
        if (!bound) sitOut(side)
    }

    /** A save made while unsolved: Mordie had ruled out [moves], or found a [solution] that long. */
    fun resume(original: IntArray, moves: Int, found: Boolean, solution: List<SolutionMove>? = null) {
        require(moves >= 0) { "Mordie's moves must not be negative" }
        bind(original)
        this.solution = solution
        resumedFromSave = true
        checkedMoves = moves
        toldMoves = moves
        if (found) {
            revealedMoves = moves
            provenShortest = true
        } else {
            opening = MordieLine.Resumed(moves)
            search = factory.start(this.original!!, moves)
        }
    }

    /** A save made after solving: the result is already decided, so nothing searches. */
    fun restoreSolved(original: IntArray, moves: Int, found: Boolean, solution: List<SolutionMove>? = null) {
        require(moves >= 0) { "Mordie's moves must not be negative" }
        bind(original)
        this.solution = solution
        outcome = MordieOutcome(moves, found)
        provenShortest = found
    }

    /** A game Mordie sits out, e.g. a solved save with no Mordie data. */
    fun clear() {
        bind(null)
    }

    /** A game too big for the search (side above [MAX_SIDE], GH-160): no search or snapshot, only
     *  the [MordieLine.SitsOut] line for its first shifts. */
    fun sitOut(side: Int) {
        bind(null)
        sitsOut = true
        satOutSide = side
    }

    /** Counts a shift, the player having made [playerMoves] moves; returns the progress when this
     *  shift's check reveals the solution. Later checks keep the best answer until it is proven. */
    fun onShift(playerMoves: Int): SearchProgress? {
        if (sitsOut) shifts++
        if (original == null || outcome != null) return null
        shifts++
        if (shifts % CHECK_EVERY != 0) return null
        if (revealedMoves != null) {
            if (!provenShortest) takeBest()
            return null
        }
        val progress = search!!.progress()
        checkedMoves = progress.moves.coerceAtLeast(0)
        if (shifts % PROGRESS_EVERY == 0) {
            progressWindowShown = checkedMoves != toldMoves
            toldMoves = checkedMoves
        }
        if (!progress.found) return null
        revealedMoves = progress.moves
        solution = progress.solution
        provenShortest = progress.provenShortest
        shiftsAtReveal = shifts
        playerMovesAtReveal = playerMoves
        return progress
    }

    fun line(): MordieLine? {
        if (sitsOut) return MordieLine.SitsOut(satOutSide).takeIf { shifts < CHECK_EVERY }
        if (original == null || outcome != null) return null
        revealedMoves?.let { return MordieLine.Found(it) }
        return when {
            shifts < CHECK_EVERY -> opening
            shifts % PROGRESS_EVERY < CHECK_EVERY && progressWindowShown ->
                MordieLine.Progress(checkedMoves, hard = shifts >= HARD_FROM)
            else -> null
        }
    }

    /** Freezes Mordie's live state as the result; `null` when Mordie sits this game out. */
    fun onSolved(): MordieOutcome? {
        sitsOut = false
        if (original == null) return null
        outcome?.let { return it }
        val progress = liveProgress()
        progress?.takeIf { it.found }?.let { solution = it.solution }
        return live(progress).also {
            provenShortest = it.provenShortest
            outcome = it
            stop()
        }
    }

    fun outcome(): MordieOutcome? = outcome

    /** The player gives up: takes the live best answer and stops the search, so the solution
     *  screen, its save and the analytics agree (GH-222). */
    fun giveUp() {
        givenUp = true
        if (original != null && outcome == null) takeBest()
        stop()
    }

    /** The arrangement this game started from; `null` when Mordie sits it out (GH-126). */
    fun original(): IntArray? = original?.copyOf()

    /** What a save should store; `null` when Mordie sits this game out. */
    fun snapshot(): MordieSnapshot? {
        val original = original ?: return null
        outcome?.let { return MordieSnapshot(original.copyOf(), it.moves, it.found, solution) }
        val progress = liveProgress()
        val state = live(progress)
        // An answer not proven shortest is saved as searching (until saves keep it, GH-224), so
        // the loaded game searches again; a solution screen's save keeps what it shows.
        if (state.found && !state.provenShortest && !givenUp) {
            return MordieSnapshot(original.copyOf(), progress!!.ruledOut.coerceAtLeast(0), found = false, solution = null)
        }
        val best = progress?.takeIf { it.found }?.solution ?: solution
        return MordieSnapshot(original.copyOf(), state.moves, state.found, best)
    }

    /** Cancels the search; everything else is kept. */
    fun stop() {
        search?.cancel()
    }

    private fun bind(arrangement: IntArray?) {
        stop()
        bound = true
        sitsOut = false
        original = arrangement?.copyOf()
        search = null
        opening = MordieLine.Opening
        checkedMoves = 0
        revealedMoves = null
        toldMoves = 0
        progressWindowShown = false
        provenShortest = false
        givenUp = false
        outcome = null
        solution = null
        shiftsAtReveal = null
        playerMovesAtReveal = null
        shifts = 0
        resumedFromSave = false
    }

    /** The search's progress right now, unless the revealed answer is final or nothing searches. */
    private fun liveProgress(): SearchProgress? = if (revealedMoves != null && provenShortest) null else search?.progress()

    /** Keeps the search's best answer as the revealed one, once it is found. */
    private fun takeBest() {
        val progress = search?.progress()?.takeIf { it.found } ?: return
        revealedMoves = progress.moves
        solution = progress.solution
        provenShortest = progress.provenShortest
    }

    /** The search's current state - not just the last check's - from [progress] read by [liveProgress]. */
    private fun live(progress: SearchProgress?): MordieOutcome {
        if (progress?.found == true) return MordieOutcome(progress.moves, found = true, progress.provenShortest)
        revealedMoves?.let { return MordieOutcome(it, found = true, provenShortest) }
        progress ?: return MordieOutcome(checkedMoves, found = false)
        return MordieOutcome(progress.moves.coerceAtLeast(0), found = false)
    }

    companion object {
        /** The largest board side Mordie searches; bigger boards it sits out (GH-160, until GH-116). */
        const val MAX_SIDE = 5

        private const val CHECK_EVERY = 5
        private const val PROGRESS_EVERY = 20
        private const val HARD_FROM = 60
    }
}
