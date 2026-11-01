package mordie

/**
 * A fast, non-minimal solver (GH-213): places the tiles 0, 1, 2, ... one at a time, each with a
 * 3-cycle built from a row move, a column move and their reverses. [step] places a few tiles
 * and returns, so a caller can spread the solve over time; the whole state is the board, the
 * next tile and the moves so far.
 *
 * The 3-cycle `[row r +a, col c +b, row r -a, col c -b]` moves the contents of the cells
 * P=(r, c-a) -> K=(r, c) -> Q=(r-b, c) -> P and leaves every other cell alone; its reverse runs
 * the moves backwards. Outside the last row, an unsolved cell always completes the L, so a tile
 * costs 4 moves. In the last row the three cells would lie on one line, so a setup (column down
 * by 1, then row 0) brings a cell from above next to the corner, and is undone afterwards: 8
 * moves. The last two tiles fall into place by themselves.
 *
 * Throws on an unsolvable board. Its solution has at most `(1 if n is even) + 4n(n-1) + 8(n-2)`
 * moves, in the GH-62 move metric (consecutive moves on one line are merged).
 */
class ThreeCycleSolver(original: IntArray) {

    private val n = MoveMetricHeuristic.sideOf(original)
    private val board = original.copyOf()
    private val moves = ArrayList<SolutionMove>()

    /** Tiles below this are home. */
    private var nextTile = 0

    /** The solution's length, once solved. */
    var solutionLength: Int? = null
        private set

    init {
        // Same rule as board_model.Solvability: on an odd side only even permutations solve.
        // This module keeps no project imports, so the parity is counted here.
        val even = isEvenPermutation()
        require(even || n % 2 == 0) { "Unsolvable board" }
        // A shift on an even side is an odd permutation: one up front makes the board even,
        // and then the last two tiles land by themselves.
        if (!even) move(isRow = true, index = 0, amount = 1)
        finishIfDone()
    }

    /** Places up to [maxTiles] tiles (tiles already home are free). True once solved. */
    fun step(maxTiles: Int): Boolean {
        var placed = 0
        while (solutionLength == null) {
            if (board[nextTile] != nextTile) {
                if (placed == maxTiles) return false
                place(nextTile)
                placed++
            }
            nextTile++
            finishIfDone()
        }
        return true
    }

    fun solution(): List<SolutionMove> {
        checkNotNull(solutionLength) { "Not solved yet" }
        return moves.toList()
    }

    private fun finishIfDone() {
        if (nextTile < n * n - 2) return
        check(board.indices.all { board[it] == it }) { "3-cycle solver left the board unsolved" }
        solutionLength = moves.size
    }

    private fun place(tile: Int) {
        val s = board.indexOf(tile)
        val rt = tile / n
        val ct = tile % n
        val rs = s / n
        val cs = s % n
        when {
            rt == n - 1 -> placeInLastRow(s, tile, cs)
            // Outside the last row the corner, one partner and the third cell X always sit in rows
            // below the solved ones.
            rs == rt -> cycle(rt, ct, mod(ct - cs), n - 1, s, tile)
            cs == ct -> cycle(rs, ct, n - 1, mod(rs - rt), s, tile)
            else -> cycle(rs, ct, mod(ct - cs), mod(rs - rt), s, tile)
        }
    }

    /** [s] and [tile] are cells of the last row, [tile] to the left; X is a third unsolved cell
     *  of the row, brought up to row 0 and then next to [tile]'s column. */
    private fun placeInLastRow(s: Int, tile: Int, cs: Int) {
        val ct = tile % n
        val cx = if (cs == ct + 1) ct + 2 else ct + 1
        val shift = mod(ct - cx)
        move(isRow = false, index = cx, amount = 1)
        move(isRow = true, index = 0, amount = shift)
        cycle(n - 1, ct, mod(ct - cs), n - 1, s, tile)
        move(isRow = true, index = 0, amount = n - shift)
        move(isRow = false, index = cx, amount = n - 1)
    }

    /** The 3-cycle on corner (r, c) with row partner a cells left and column partner b cells up,
     *  in whichever direction takes the tile at cell [from] to cell [to]. */
    private fun cycle(r: Int, c: Int, a: Int, b: Int, from: Int, to: Int) {
        val p = r * n + mod(c - a)
        val k = r * n + c
        val q = mod(r - b) * n + c
        val forward = (from == p && to == k) || (from == k && to == q) || (from == q && to == p)
        if (forward) {
            move(true, r, a); move(false, c, b); move(true, r, n - a); move(false, c, n - b)
        } else {
            move(false, c, b); move(true, r, a); move(false, c, n - b); move(true, r, n - a)
        }
    }

    /** Applies the move to the board and records it, merged into the previous move on the same line. */
    private fun move(isRow: Boolean, index: Int, amount: Int) {
        val old = board.copyOf()
        for (k in 0 until n) {
            if (isRow) board[index * n + (k + amount) % n] = old[index * n + k]
            else board[((k + amount) % n) * n + index] = old[k * n + index]
        }
        val last = moves.lastOrNull()
        if (last == null || last.isRow != isRow || last.index != index) {
            moves += SolutionMove(isRow, index, amount)
            return
        }
        moves.removeAt(moves.lastIndex)
        val merged = (last.amount + amount) % n
        if (merged != 0) moves += SolutionMove(isRow, index, merged)
    }

    private fun mod(x: Int) = ((x % n) + n) % n

    /** Even iff (size - number of cycles) is even. */
    private fun isEvenPermutation(): Boolean {
        val visited = BooleanArray(board.size)
        var cycles = 0
        for (start in board.indices) {
            if (visited[start]) continue
            cycles++
            var i = start
            while (!visited[i]) {
                visited[i] = true
                i = board[i]
            }
        }
        return (board.size - cycles) % 2 == 0
    }
}
