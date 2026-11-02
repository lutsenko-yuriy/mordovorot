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

    private var search: MordieSearch? = null
    private var opening: MordieLine = MordieLine.Opening
    private var checkedMoves = 0
    private var revealedMoves: Int? = null

    /** The line from the reveal on, fixed when set: it changes only at a drop (GH-223). */
    private var announced: MordieLine? = null

    /** Whether the solution has been revealed, whichever line says so. */
    val revealed: Boolean get() = revealedMoves != null

    /** Whether the revealed answer is proven shortest (GH-222); the checks go on until it is. */
    var provenShortest = false
        private set

    /** The largest length the exact search had ruled out when the answer was last read (GH-224). */
    private var ruledOut = 0

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

    /** A save made while unsolved: Mordie had ruled out [moves], or found a [solution] that long,
     *  [provenShortest] unless the save says it may not be (then [ruledOut] is what was ruled out,
     *  GH-224). An unproven answer shows at once and the exact search goes on above [ruledOut] from
     *  it, unless the save is a [solutionScreen]'s, which only shows it. */
    fun resume(
        original: IntArray,
        moves: Int,
        found: Boolean,
        solution: List<SolutionMove>? = null,
        provenShortest: Boolean = true,
        ruledOut: Int = 0,
        solutionScreen: Boolean = false,
    ) {
        require(moves >= 0) { "Mordie's moves must not be negative" }
        bind(original)
        this.solution = solution
        resumedFromSave = true
        checkedMoves = moves
        toldMoves = moves
        // An unproven answer with no path (a damaged save) can't be shown or searched from: it only
        // tells how far the exact search had got, so it is treated as still searching.
        if (found && (provenShortest || solution != null)) {
            revealedMoves = moves
            this.provenShortest = provenShortest
            this.ruledOut = ruledOut
            announced = if (provenShortest) MordieLine.Found(moves) else MordieLine.FoundUnproven(moves)
            if (!provenShortest && !solutionScreen) search = factory.start(this.original!!, ruledOut, solution)
        } else {
            val searched = if (found) ruledOut else moves
            opening = MordieLine.Resumed(searched)
            checkedMoves = searched
            toldMoves = searched
            search = factory.start(this.original!!, searched)
        }
    }

    /** A save made after solving: the result is already decided, so nothing searches. */
    fun restoreSolved(
        original: IntArray,
        moves: Int,
        found: Boolean,
        solution: List<SolutionMove>? = null,
        provenShortest: Boolean = true,
        ruledOut: Int = 0,
    ) {
        require(moves >= 0) { "Mordie's moves must not be negative" }
        bind(original)
        this.solution = solution
        this.provenShortest = found && provenShortest
        this.ruledOut = ruledOut
        outcome = MordieOutcome(moves, found, this.provenShortest)
    }

    /** A game Mordie sits out, e.g. a solved save with no Mordie data. */
    fun clear() {
        bind(null)
    }

    /** Counts a shift, the player having made [playerMoves] moves; returns what this shift's check
     *  found worth telling: the solution revealed, or a shorter one. Later checks keep the best
     *  answer until it is proven. */
    fun onShift(playerMoves: Int): MordieCheck? {
        if (original == null || outcome != null) return null
        shifts++
        if (shifts % CHECK_EVERY != 0) return null
        if (revealedMoves != null) {
            if (provenShortest) return null
            val previous = revealedMoves!!
            takeBest()
            val moves = revealedMoves!!
            if (moves >= previous) return null
            announced = MordieLine.Shorter(moves)
            return MordieCheck.Shortened(moves, previous, provenShortest)
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
        ruledOut = progress.ruledOut
        announced = if (provenShortest) MordieLine.Found(progress.moves) else MordieLine.FoundUnproven(progress.moves)
        shiftsAtReveal = shifts
        playerMovesAtReveal = playerMoves
        return MordieCheck.Revealed(progress)
    }

    fun line(): MordieLine? {
        if (original == null || outcome != null) return null
        if (revealedMoves != null) return announced
        return when {
            shifts < CHECK_EVERY -> opening
            shifts % PROGRESS_EVERY < CHECK_EVERY && progressWindowShown ->
                MordieLine.Progress(checkedMoves, hard = shifts >= HARD_FROM)
            else -> null
        }
    }

    /** Freezes Mordie's live state as the result; `null` when Mordie sits this game out. */
    fun onSolved(): MordieOutcome? {
        if (original == null) return null
        outcome?.let { return it }
        val progress = liveProgress()
        progress?.takeIf { it.found }?.let {
            solution = it.solution
            ruledOut = it.ruledOut
        }
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
        if (original != null && outcome == null) takeBest()
        stop()
    }

    /** The arrangement this game started from; `null` when Mordie sits it out (GH-126). */
    fun original(): IntArray? = original?.copyOf()

    /** What a save should store; `null` when Mordie sits this game out. */
    fun snapshot(): MordieSnapshot? {
        val original = original ?: return null
        outcome?.let { return snapshotOf(original, it, solution, ruledOut) }
        val progress = liveProgress()
        val state = live(progress)
        val best = progress?.takeIf { it.found }?.solution ?: solution
        return snapshotOf(original, state, best, maxOf(ruledOut, progress?.ruledOut ?: 0))
    }

    private fun snapshotOf(original: IntArray, state: MordieOutcome, solution: List<SolutionMove>?, ruledOut: Int): MordieSnapshot {
        // One move is always the shortest, and the save format has no R below 0 for it (GH-224).
        val unproven = state.found && !state.provenShortest && state.moves >= 2
        val kept = if (unproven) ruledOut.coerceIn(0, state.moves - 2) else 0
        return MordieSnapshot(original.copyOf(), state.moves, state.found, solution, provenShortest = !unproven, ruledOut = kept)
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
        announced = null
        toldMoves = 0
        progressWindowShown = false
        provenShortest = false
        ruledOut = 0
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
        ruledOut = progress.ruledOut
    }

    /** The search's current state - not just the last check's - from [progress] read by [liveProgress]. */
    private fun live(progress: SearchProgress?): MordieOutcome {
        if (progress?.found == true) return MordieOutcome(progress.moves, found = true, progress.provenShortest)
        revealedMoves?.let { return MordieOutcome(it, found = true, provenShortest) }
        progress ?: return MordieOutcome(checkedMoves, found = false)
        return MordieOutcome(progress.moves.coerceAtLeast(0), found = false)
    }

    companion object {
        private const val CHECK_EVERY = 5
        private const val PROGRESS_EVERY = 20
        private const val HARD_FROM = 60
    }
}
