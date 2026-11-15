package mordie

/**
 * A pattern database for [IdaStarSolver] (GH-282): the exact move-metric distance of row 0 plus the first
 * tile of row 1 ([k] = n + 1 tiles), ignoring every other tile. A move of the full board is a move of the
 * pattern, so that distance never exceeds the board's. The torus translations and the transpose map the
 * moves onto themselves, so reading the table through each of those `2 * n * n` copies gives more bounds,
 * and their maximum is one too.
 *
 * The table is filled by a breadth-first search from the solved pattern, in slices ([step]), so a caller
 * can build it between slices of another search. A state is its tiles' cells, [bits] bits each.
 */
class PatternDatabase(private val n: Int) {

    private val cells = n * n
    private val k = n + 1
    private val bits = 32 - (cells - 1).countLeadingZeroBits()

    init {
        require(bits * k <= MAX_INDEX_BITS) { "A ${n}x$n pattern table would be too big" }
    }

    private val total = (0 until k).fold(1) { product, i -> product * (cells - i) }

    // Allocated by the first step: a search that ends early never pays for the table.
    private var distance = ByteArray(0)
    private var queue = IntArray(0)
    private var head = 0
    private var tail = 0

    /** True once every state of the pattern has its distance. */
    val done: Boolean get() = tail > 0 && head == tail

    /** The number of states reached so far: all of them, once [done]. */
    internal val seen: Int get() = tail

    /** The largest distance in the table, once [done]. */
    internal val farthest: Int get() = distance.maxOf { it.toInt() }

    private val copies = 2 * cells

    /** Copy c's tile i is `copyTiles[c * k + i]`; `weight[(c * k + i) * cells + cell]` is where that tile standing on
     *  [cell] lands in the table's index, once the copy is mapped back onto the pattern. */
    private val copyTiles = IntArray(copies * k)
    private val weight = IntArray(copies * k * cells)

    // For the build: each line (rows, then columns) as the index along it of each cell (-1 off the line), and back.
    private val indexOnLine = IntArray(2 * n * cells) { -1 }
    private val cellOnLine = IntArray(2 * n * n)
    private val wrap = IntArray(2 * n) { it % n }

    init {
        for (line in 0 until 2 * n) for (j in 0 until n) {
            val cell = if (line < n) line * n + j else j * n + line - n
            indexOnLine[line * cells + cell] = j
            cellOnLine[line * n + j] = cell
        }
        var copy = 0
        for (transpose in listOf(false, true)) for (dr in 0 until n) for (dc in 0 until n) {
            val image = IntArray(cells) { cell ->
                val r = (cell / n + dr) % n
                val c = (cell % n + dc) % n
                if (transpose) c * n + r else r * n + c
            }
            val inverse = IntArray(cells)
            for (cell in 0 until cells) inverse[image[cell]] = cell
            for (i in 0 until k) {
                copyTiles[copy * k + i] = image[i]
                for (cell in 0 until cells) weight[(copy * k + i) * cells + cell] = inverse[cell] shl (bits * i)
            }
            copy++
        }
    }

    /** Builds the table for up to [maxStates] more states. */
    fun step(maxStates: Int) {
        if (queue.isEmpty()) {
            distance = ByteArray(1 shl (bits * k)) { UNSEEN }
            queue = IntArray(total)
            var goal = 0
            for (i in 0 until k) goal = goal or (i shl (bits * i))
            distance[goal] = 0
            queue[tail++] = goal
        }
        val mask = (1 shl bits) - 1
        val at = IntArray(k)
        repeat(maxStates) {
            if (head == tail) return
            val state = queue[head++]
            val next = (distance[state] + 1).toByte()
            for (i in 0 until k) at[i] = (state ushr (bits * i)) and mask
            for (line in 0 until 2 * n) {
                val base = line * cells
                var rest = state
                var moving = 0
                for (i in 0 until k) if (indexOnLine[base + at[i]] >= 0) {
                    moving = moving or (1 shl i)
                    rest = rest and (mask shl (bits * i)).inv()
                }
                if (moving == 0) continue
                for (amount in 1 until n) {
                    var rotated = rest
                    for (i in 0 until k) if (moving and (1 shl i) != 0) {
                        rotated = rotated or (cellOnLine[line * n + wrap[indexOnLine[base + at[i]] + amount]] shl (bits * i))
                    }
                    if (distance[rotated] == UNSEEN) {
                        distance[rotated] = next
                        queue[tail++] = rotated
                    }
                }
            }
        }
        // Not needed any more; non-empty, so that a later step doesn't start over.
        if (head == tail) queue = IntArray(1)
    }

    /** The bound for [state] (the tile on each cell): the best of every copy. Needs [done]. */
    fun lowerBound(state: IntArray): Int {
        val pos = IntArray(cells)
        for (cell in state.indices) pos[state[cell]] = cell
        return lowerBound(pos, 0, Int.MAX_VALUE)
    }

    /**
     * The larger of [floor] and the copies' values for the board with tile t on cell `pos[t]`, stopping at
     * the first value over [limit]: a caller that only needs to know the child is cut gets that answer sooner.
     * Needs [done].
     */
    internal fun lowerBound(pos: IntArray, floor: Int, limit: Int): Int {
        var best = floor
        var s = 0
        repeat(copies) {
            var index = 0
            repeat(k) {
                index += weight[s * cells + pos[copyTiles[s]]]
                s++
            }
            val value = distance[index].toInt()
            if (value > best) {
                best = value
                if (best > limit) return best
            }
        }
        return best
    }

    private companion object {
        const val UNSEEN: Byte = -1

        /** The 4x4's: 1 MiB of table. */
        const val MAX_INDEX_BITS = 20
    }
}
