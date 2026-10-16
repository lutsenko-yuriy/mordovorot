package mordie

/** One move of a solution: rotate a row right (or a column down) by [amount] cells. */
internal data class SolutionMove(val isRow: Boolean, val index: Int, val amount: Int)

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
            if (board[i] / n != i / n) wrongRow++
            if (board[i] % n != i % n) wrongCol++
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

    /** The found solution, in order - for tests only, nothing shows it to the player. */
    internal fun solution(): List<SolutionMove> {
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
        apply(move, forward = true)
        val h = heuristic()
        val f = depth + 1 + h
        if (f > bound) {
            if (f < nextBound) nextBound = f
            apply(move, forward = false)
            return
        }
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
    private fun lineOf(move: Int) = (move % movesPerAxis) / movesPerLine
    private fun amountOf(move: Int) = move % movesPerLine + 1

    /** Rotates in place, keeping the wrong-row/column counts current: a row rotation only
     *  changes the columns of its own tiles, a column rotation only their rows. */
    private fun apply(move: Int, forward: Boolean) {
        val isRow = isRow(move)
        val index = lineOf(move)
        val k = if (forward) amountOf(move) else n - amountOf(move)
        for (j in 0 until n) {
            val at = if (isRow) index * n + j else j * n + index
            if (isRow && board[at] % n != j) wrongCol--
            if (!isRow && board[at] / n != j) wrongRow--
            scratch[(j + k) % n] = board[at]
        }
        for (j in 0 until n) {
            val at = if (isRow) index * n + j else j * n + index
            board[at] = scratch[j]
            if (isRow && board[at] % n != j) wrongCol++
            if (!isRow && board[at] / n != j) wrongRow++
        }
    }

    private fun heuristic() = MoveMetricHeuristic.of(n, wrongRow, wrongCol)

    private companion object {
        const val INITIAL_DEPTH = 32
        const val NO_MOVE = -1
    }
}
