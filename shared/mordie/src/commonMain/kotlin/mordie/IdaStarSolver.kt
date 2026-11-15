package mordie

/**
 * IDA* search for the shortest solution of [original] in the GH-62 move metric, guided by
 * [MoveMetricHeuristic]. The depth-first search runs on an explicit stack, so [step] can stop
 * after any number of nodes and a caller can spread the search over time; memory is O(depth).
 *
 * The board must be solvable - an unsolvable one is searched forever.
 *
 * Pruning: never two moves in a row on the same line (they merge into one), and consecutive
 * row moves - which commute - go in increasing index order, the same for columns.
 *
 * [ruledOut] (a saved N) skips every bound up to it, so a resumed search never redoes or
 * reports lengths below it. It must be a proven lower bound - one past the true minimum (a
 * hand-edited save) can yield a non-minimal length.
 */
class IdaStarSolver(original: IntArray, ruledOut: Int = -1) {

    private val n = MoveMetricHeuristic.sideOf(original)
    private val board = original.copyOf()
    private val scratch = IntArray(n)
    private val movesPerLine = n - 1

    /** Moves are numbered row moves first, then column moves; each axis has [movesPerAxis]. */
    private val movesPerAxis = n * movesPerLine

    // Tables instead of `/` and `%` in the node loop: Mordie may run where scripts aren't compiled (GH-282).
    private val homeRow = IntArray(n * n) { it / n }
    private val homeCol = IntArray(n * n) { it % n }
    private val moveLine = IntArray(2 * movesPerAxis) { (it % movesPerAxis) / movesPerLine }
    private val moveAmount = IntArray(2 * movesPerAxis) { it % movesPerLine + 1 }
    private val wrap = IntArray(2 * n) { it % n }
    private val lines = IntArray(n * n + 1) { (it + n - 1) / n }

    private var wrongRow = 0
    private var wrongCol = 0

    private var bound: Int
    private var nextBound = Int.MAX_VALUE
    private var depth = 0

    /** Next move to try at each depth. */
    private var cursor = IntArray(INITIAL_DEPTH)

    /** Move applied to get from each depth to the next. */
    private var applied = IntArray(INITIAL_DEPTH)

    /** The largest solution length proven impossible so far. */
    var ruledOut: Int
        private set

    /** The minimum number of moves, once found. */
    var solutionLength: Int? = null
        private set

    init {
        for (i in board.indices) {
            if (homeRow[board[i]] != i / n) wrongRow++
            if (homeCol[board[i]] != i % n) wrongCol++
        }
        val h0 = heuristic()
        bound = maxOf(h0, ruledOut + 1)
        this.ruledOut = bound - 1
        if (h0 == 0) solutionLength = 0
    }

    /** Expands up to [maxNodes] nodes; `true` once the solution is found. */
    fun step(maxNodes: Int): Boolean {
        repeat(maxNodes) {
            if (solutionLength != null) return true
            expand()
        }
        return solutionLength != null
    }

    /** The found solution, in order. */
    fun solution(): List<SolutionMove> {
        val length = checkNotNull(solutionLength) { "No solution found yet" }
        return (0 until length).map { applied[it] }.map { SolutionMove(isRow(it), lineOf(it), amountOf(it)) }
    }

    private fun expand() {
        val move = nextAllowedMove(cursor[depth])
        if (move == NO_MOVE) {
            backtrack()
            return
        }
        cursor[depth] = move + 1
        // The child's counts are read off the moved line before anything is rotated: most children are cut here.
        val delta = shiftDelta(move, amountOf(move))
        val h = if (isRow(move)) lines[wrongRow] + lines[wrongCol + delta] else lines[wrongRow + delta] + lines[wrongCol]
        val f = depth + 1 + h
        if (f > bound) {
            if (f < nextBound) nextBound = f
            return
        }
        apply(move, forward = true)
        applied[depth] = move
        if (h == 0) {
            solutionLength = depth + 1
            return
        }
        depth++
        if (depth == cursor.size) {
            cursor = cursor.copyOf(depth * 2)
            applied = applied.copyOf(depth * 2)
        }
        cursor[depth] = 0
    }

    private fun backtrack() {
        if (depth == 0) {
            // Every path within the bound failed: the next bound is the smallest f that exceeded it.
            bound = nextBound
            ruledOut = bound - 1
            nextBound = Int.MAX_VALUE
            cursor[0] = 0
            return
        }
        depth--
        apply(applied[depth], forward = false)
    }

    /** The first move from [from] on that the pruning allows, or [NO_MOVE]. After a move on
     *  line L of an axis, that axis continues at line L + 1. */
    private fun nextAllowedMove(from: Int): Int {
        if (from >= 2 * movesPerAxis) return NO_MOVE
        if (depth == 0) return from
        val previous = applied[depth - 1]
        val firstAllowed = (if (isRow(previous)) 0 else movesPerAxis) + (lineOf(previous) + 1) * movesPerLine
        val sameAxis = isRow(from) == isRow(previous)
        return when {
            !sameAxis || from >= firstAllowed -> from
            firstAllowed < (if (isRow(previous)) movesPerAxis else 2 * movesPerAxis) -> firstAllowed
            else -> if (isRow(previous)) movesPerAxis else NO_MOVE
        }
    }

    private fun isRow(move: Int) = move < movesPerAxis
    private fun lineOf(move: Int) = moveLine[move]
    private fun amountOf(move: Int) = moveAmount[move]

    /** How much a rotation of [move]'s line by [k] changes the wrong-column count (a row move) or the
     *  wrong-row count (a column move), without rotating: only the n tiles of the line move. */
    private fun shiftDelta(move: Int, k: Int): Int {
        val isRow = isRow(move)
        val first = if (isRow) lineOf(move) * n else lineOf(move)
        val stride = if (isRow) 1 else n
        var delta = 0
        for (j in 0 until n) {
            val tile = board[first + j * stride]
            val home = if (isRow) homeCol[tile] else homeRow[tile]
            if (home != j) delta--
            if (home != wrap[j + k]) delta++
        }
        return delta
    }

    /** Rotates in place, keeping the wrong-row/column counts current: a row rotation only
     *  changes the columns of its own tiles, a column rotation only their rows. */
    private fun apply(move: Int, forward: Boolean) {
        val isRow = isRow(move)
        val first = if (isRow) lineOf(move) * n else lineOf(move)
        val stride = if (isRow) 1 else n
        val k = if (forward) amountOf(move) else n - amountOf(move)
        val delta = shiftDelta(move, k)
        for (j in 0 until n) scratch[wrap[j + k]] = board[first + j * stride]
        for (j in 0 until n) board[first + j * stride] = scratch[j]
        if (isRow) wrongCol += delta else wrongRow += delta
    }

    private fun heuristic() = lines[wrongRow] + lines[wrongCol]

    private companion object {
        const val INITIAL_DEPTH = 32
        const val NO_MOVE = -1
    }
}
