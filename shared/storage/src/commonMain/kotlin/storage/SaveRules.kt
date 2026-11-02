package storage

/** The checks every save format shares (GH-237): what makes an arrangement, a solution and a ruled out length valid. */
internal object SaveRules {

    fun isArrangement(values: IntArray, size: Int) = values.size == size && values.toSet() == (0 until size).toSet()

    /** Why [solution] can't be [mordie]'s on a [side]x[side] board, or `null` when it can. */
    fun solutionProblem(solution: List<SavedMove>, mordie: SavedMordie, side: Int): String? = when {
        !mordie.found -> "Mordie's solution on a save where it is still searching"
        solution.any { it.index !in 0 until side || it.amount !in 1 until side } ->
            "Mordie's solution steps must name a line 0-${side - 1} and rotate it 1-${side - 1} cells"
        solution.size != mordie.moves -> "Mordie's solution has ${solution.size} steps, not ${mordie.moves}"
        !solves(solution, mordie.original, side) -> "Mordie's solution doesn't solve the original arrangement"
        else -> null
    }

    /** Why [ruledOut] can't go with an unproven answer of [moves], or `null` when it can. */
    fun ruledOutProblem(found: Boolean, moves: Int, ruledOut: Int): String? = when {
        !found -> "Mordie's unproven answer on a save where it is still searching"
        ruledOut !in 0..moves - 2 -> "Mordie's ruled out length must be 0 to ${moves - 2}"
        else -> null
    }

    private fun solves(solution: List<SavedMove>, original: IntArray, side: Int): Boolean {
        val tiles = original.copyOf()
        val line = IntArray(side)
        for (move in solution) {
            val at = { j: Int -> if (move.isRow) move.index * side + j else j * side + move.index }
            for (j in 0 until side) line[(j + move.amount) % side] = tiles[at(j)]
            for (j in 0 until side) tiles[at(j)] = line[j]
        }
        return tiles.indices.all { tiles[it] == it }
    }
}
