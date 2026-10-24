package storage

/** The save text format, shared by the terminal's files and the browser's storage (GH-110):
 *  square side, then the values, then the move count, then optional Mordie data, then optional
 *  tile labels on line 8 (GH-139), one per line. */
object SaveFileCodec {

    private const val FOUND = "found"
    private const val SEARCHING = "searching"
    private const val SOLUTION_SCREEN = "solution"
    private const val MORDIE_LINES = 4
    private val MOVE_TOKEN = Regex("([rc])(\\d+):(\\d+)")

    /** Throws [IllegalArgumentException] for a state, move count, Mordie data or labels [SaveRepository.save] rejects. */
    fun encode(squareSide: Int, state: IntArray, moveCount: Int, mordie: SavedMordie?, labels: String? = null): String {
        require(squareSide > 0) { "Square side must be positive" }
        require(state.size == squareSide * squareSide) { "Expected ${squareSide * squareSide} values, got ${state.size}" }
        require(isArrangement(state, state.size)) { "State must contain each of ${state.size} tile values exactly once" }
        require(moveCount >= 0) { "Move count must not be negative" }
        if (mordie != null) {
            require(isArrangement(mordie.original, state.size)) { "Mordie's original must be a valid ${state.size}-tile arrangement" }
            require(mordie.moves >= 0) { "Mordie's moves must not be negative" }
            mordie.solution?.let { solution -> solutionProblem(solution, mordie, squareSide)?.let { throw IllegalArgumentException(it) } }
            require(!mordie.solutionScreen || !mordie.solution.isNullOrEmpty()) { "A solution-screen save needs Mordie's solution" }
        }
        require(labels == null || (labels.isNotEmpty() && labels.none { it.isWhitespace() })) { "Labels must be one word" }
        val optional = mordie?.let(::mordieLines).orEmpty()
        // Lines 4-7 are blank where they don't apply, so the labels always land on line 8.
        val tail = if (labels == null) optional else optional + List(MORDIE_LINES - optional.size) { "" } + labels
        return (listOf("$squareSide", state.joinToString(" "), "$moveCount") + tail).joinToString("") { "$it\n" }
    }

    /** Lines 4-7; an empty solution is written as none, so line 6 is never blank before line 7. */
    private fun mordieLines(mordie: SavedMordie): List<String> {
        val solution = mordie.solution?.takeIf { it.isNotEmpty() }
            ?.joinToString(" ") { "${if (it.isRow) 'r' else 'c'}${it.index}:${it.amount}" }
        return listOfNotNull(
            mordie.original.joinToString(" "),
            "${mordie.moves} ${if (mordie.found) FOUND else SEARCHING}",
            solution,
            SOLUTION_SCREEN.takeIf { mordie.solutionScreen },
        )
    }

    /** Throws [SaveFileFormatException], naming the save [name], when [text] isn't a valid save. */
    fun decode(name: String, text: String): SavedBoard {
        // As a file's lines: a final line break ends the last line rather than starting an empty one.
        val lines = text.lines().let { if (it.last().isEmpty()) it.dropLast(1) else it }
        val squareSideLine = lines.getOrNull(0) ?: throw SaveFileFormatException(name, "missing square side")
        val valuesLine = lines.getOrNull(1) ?: throw SaveFileFormatException(name, "missing board values")

        val squareSide = squareSideLine.trim().toIntOrNull()
            ?: throw SaveFileFormatException(name, "square side is not an integer")
        if (squareSide <= 0) throw SaveFileFormatException(name, "square side must be positive")

        val expectedSize = squareSide * squareSide
        val state = parseArrangement(name, valuesLine, expectedSize, "board")

        // Saves from before GH-62 have no third line - they load with no moves made.
        val moveCountLine = lines.getOrNull(2)?.trim().orEmpty()
        val moveCount = if (moveCountLine.isEmpty()) 0 else moveCountLine.toIntOrNull()
            ?: throw SaveFileFormatException(name, "move count is not an integer")
        if (moveCount < 0) throw SaveFileFormatException(name, "move count must not be negative")

        // Line 8 (GH-139): any one word, so a later build's scheme loads with the preference instead.
        val labels = lines.getOrNull(7)?.trim()?.takeIf { it.isNotEmpty() }
        return SavedBoard(squareSide, state, moveCount, parseMordie(name, lines, squareSide), labels)
    }

    /** Lines 4-5 (GH-83): the original arrangement, then `<N> found` or `<N> searching`.
     *  A missing or blank line 4 means no Mordie data. Lines 6-7 (GH-126): Mordie's solution on a
     *  found save, then `solution` on a solution-screen save; missing or blank means neither. */
    private fun parseMordie(name: String, lines: List<String>, side: Int): SavedMordie? {
        val solutionLine = lines.getOrNull(5)?.takeIf { it.isNotBlank() }
        val screenLine = lines.getOrNull(6)?.takeIf { it.isNotBlank() }
        val originalLine = lines.getOrNull(3)?.takeIf { it.isNotBlank() }
        if (originalLine == null) {
            if (solutionLine != null || screenLine != null) throw SaveFileFormatException(name, "Mordie's solution without Mordie data")
            return null
        }
        val original = parseArrangement(name, originalLine, side * side, "Mordie's original")
        val status = lines.getOrNull(4)?.trim()?.split(Regex("\\s+"))
            ?: throw SaveFileFormatException(name, "missing Mordie's status")
        val moves = status.getOrNull(0)?.toIntOrNull()
        val keyword = status.getOrNull(1)
        if (status.size != 2 || moves == null || moves < 0 || keyword !in setOf(FOUND, SEARCHING)) {
            throw SaveFileFormatException(name, "Mordie's status must be '<N> $FOUND' or '<N> $SEARCHING'")
        }
        val found = keyword == FOUND
        val solution = solutionLine?.let { parseSolution(name, it, SavedMordie(original, moves, found), side) }
        if (screenLine != null && (solution == null || screenLine.trim() != SOLUTION_SCREEN)) {
            throw SaveFileFormatException(name, "line 7 must be '$SOLUTION_SCREEN', after Mordie's solution")
        }
        return SavedMordie(original, moves, found, solution, solutionScreen = screenLine != null)
    }

    /** Line 6: space-separated `r<i>:<k>` / `c<i>:<k>` - rotate row i right, or column i down, k cells. */
    private fun parseSolution(name: String, line: String, mordie: SavedMordie, side: Int): List<SavedMove> {
        val solution = line.trim().split(Regex("\\s+")).map { token ->
            val match = MOVE_TOKEN.matchEntire(token)
                ?: throw SaveFileFormatException(name, "Mordie's solution step '$token' is not r<line>:<k> or c<line>:<k>")
            val (axis, index, amount) = match.destructured
            SavedMove(axis == "r", index.toIntOrNull() ?: -1, amount.toIntOrNull() ?: -1)
        }
        solutionProblem(solution, mordie, side)?.let { throw SaveFileFormatException(name, it) }
        return solution
    }

    /** Why [solution] can't be [mordie]'s on a [side]x[side] board, or `null` when it can. */
    private fun solutionProblem(solution: List<SavedMove>, mordie: SavedMordie, side: Int): String? = when {
        !mordie.found -> "Mordie's solution on a save where it is still searching"
        solution.any { it.index !in 0 until side || it.amount !in 1 until side } ->
            "Mordie's solution steps must name a line 0-${side - 1} and rotate it 1-${side - 1} cells"
        solution.size != mordie.moves -> "Mordie's solution has ${solution.size} steps, not ${mordie.moves}"
        !solves(solution, mordie.original, side) -> "Mordie's solution doesn't solve the original arrangement"
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

    private fun parseArrangement(name: String, line: String, expectedSize: Int, what: String): IntArray {
        val tokens = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val values = IntArray(tokens.size) { i ->
            tokens[i].toIntOrNull() ?: throw SaveFileFormatException(name, "$what value '${tokens[i]}' is not an integer")
        }
        if (values.size != expectedSize) {
            throw SaveFileFormatException(name, "expected $expectedSize $what values, found ${values.size}")
        }
        if (!isArrangement(values, expectedSize)) {
            throw SaveFileFormatException(name, "$what values are not a valid arrangement (each tile must appear exactly once)")
        }
        return values
    }

    private fun isArrangement(values: IntArray, size: Int) = values.size == size && values.toSet() == (0 until size).toSet()
}
