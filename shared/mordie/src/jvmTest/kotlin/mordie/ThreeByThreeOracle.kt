package mordie

/** Exact move-metric distances for every solvable 3x3 board (181,440), by BFS from the solved
 *  board - the test oracle for [MoveMetricHeuristic] and [IdaStarSolver]. A move rotates one
 *  row or column by 1 or 2, so every move is invertible and distance-to-solved equals
 *  distance-from-solved. States are indexed by permutation rank (Lehmer code). */
object ThreeByThreeOracle {

    private const val N = 3
    private const val SIZE = N * N
    private const val UNSEEN: Byte = -1

    private val distances: ByteArray by lazy { bfs() }

    /** Distance of [state], or -1 for an unsolvable one. */
    fun distance(state: IntArray): Int = distances[rank(state)].toInt()

    /** Every solvable state, in rank order. */
    fun solvableStates(): Sequence<IntArray> =
        distances.indices.asSequence().filter { distances[it] != UNSEEN }.map(::unrank)

    private fun bfs(): ByteArray {
        val dist = ByteArray(FACTORIALS[SIZE]) { UNSEEN }
        val queue = IntArray(dist.size)
        var head = 0
        var tail = 0
        val solved = rank(IntArray(SIZE) { it })
        dist[solved] = 0
        queue[tail++] = solved
        while (head < tail) {
            val current = queue[head++]
            val state = unrank(current)
            for (next in neighbours(state)) {
                val r = rank(next)
                if (dist[r] == UNSEEN) {
                    dist[r] = (dist[current] + 1).toByte()
                    queue[tail++] = r
                }
            }
        }
        return dist
    }

    private fun neighbours(state: IntArray): List<IntArray> = buildList {
        for (line in 0 until N) for (k in 1 until N) {
            add(state.copyOf().also { s -> for (c in 0 until N) s[line * N + (c + k) % N] = state[line * N + c] })
            add(state.copyOf().also { s -> for (r in 0 until N) s[((r + k) % N) * N + line] = state[r * N + line] })
        }
    }

    private val FACTORIALS = IntArray(SIZE + 1).also { f -> f[0] = 1; for (i in 1..SIZE) f[i] = f[i - 1] * i }

    private fun rank(state: IntArray): Int {
        var r = 0
        for (i in 0 until SIZE) {
            val smallerLater = (i + 1 until SIZE).count { state[it] < state[i] }
            r += smallerLater * FACTORIALS[SIZE - 1 - i]
        }
        return r
    }

    private fun unrank(rank: Int): IntArray {
        val remaining = (0 until SIZE).toMutableList()
        var r = rank
        return IntArray(SIZE) { i ->
            val f = FACTORIALS[SIZE - 1 - i]
            remaining.removeAt(r / f).also { r %= f }
        }
    }
}
