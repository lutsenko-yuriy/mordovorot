package storage

/** The save text format of the terminal days and GH-110 to GH-224, kept in the browser's storage
 *  (GH-110): square side, then the values, then the move count, then optional Mordie data, then
 *  optional tile labels on line 8 (GH-139), then `unproven <R>` on line 9 (GH-224), one per line. */
internal object TextSaveFormat {

    private const val FOUND = "found"
    private const val SEARCHING = "searching"
    private const val SOLUTION_SCREEN = "solution"
    private const val UNPROVEN = "unproven"
    private val MOVE_TOKEN = Regex("([rc])(\\d+):(\\d+)")

    /** Lines 4-7 are blank where they don't apply, so the labels always land on line 8 and line 9 after it. */
    fun encode(squareSide: Int, state: IntArray, moveCount: Int, mordie: SavedMordie?, labels: String?): String {
        val lines = (mordie?.let(::mordieLines) ?: List(4) { "" }) + listOf(labels.orEmpty(), unprovenLine(mordie))
        return (listOf("$squareSide", state.joinToString(" "), "$moveCount") + lines.dropLastWhile { it.isEmpty() })
            .joinToString("") { "$it\n" }
    }

    /** Lines 4-7; an empty solution is written as none, so line 6 is never blank before line 7. */
    private fun mordieLines(mordie: SavedMordie): List<String> {
        val solution = mordie.solution?.takeIf { it.isNotEmpty() }
            ?.joinToString(" ") { "${if (it.isRow) 'r' else 'c'}${it.index}:${it.amount}" }
        return listOf(
            mordie.original.joinToString(" "),
            "${mordie.moves} ${if (mordie.found) FOUND else SEARCHING}",
            solution.orEmpty(),
            SOLUTION_SCREEN.takeIf { mordie.solutionScreen }.orEmpty(),
        )
    }

    private fun unprovenLine(mordie: SavedMordie?) =
        if (mordie != null && mordie.found && !mordie.provenShortest) "$UNPROVEN ${mordie.ruledOut}" else ""

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
        val labels = lines.getOrNull(7)?.trim()?.split(Regex("\\s+"))?.first()?.takeIf { it.isNotEmpty() }
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
        val ruledOut = parseUnproven(name, lines.getOrNull(8), found, moves)
        return SavedMordie(original, moves, found, solution, solutionScreen = screenLine != null, provenShortest = ruledOut == null, ruledOut = ruledOut ?: 0)
    }

    /** Line 9 (GH-224): `unproven <R>`, R the largest length ruled out; `null` when the answer is proven.
     *  Another first word is ignored, so a later build's data loads with a proven answer. */
    private fun parseUnproven(name: String, line: String?, found: Boolean, moves: Int): Int? {
        val words = line?.trim()?.split(Regex("\\s+"))?.takeIf { it.first() == UNPROVEN } ?: return null
        val ruledOut = words.getOrNull(1)?.toIntOrNull()
        if (words.size != 2 || ruledOut == null) throw SaveFileFormatException(name, "line 9 must be '$UNPROVEN <N>'")
        SaveRules.ruledOutProblem(found, moves, ruledOut)?.let { throw SaveFileFormatException(name, it) }
        return ruledOut
    }

    /** Line 6: space-separated `r<i>:<k>` / `c<i>:<k>` - rotate row i right, or column i down, k cells. */
    private fun parseSolution(name: String, line: String, mordie: SavedMordie, side: Int): List<SavedMove> {
        val solution = line.trim().split(Regex("\\s+")).map { token ->
            val match = MOVE_TOKEN.matchEntire(token)
                ?: throw SaveFileFormatException(name, "Mordie's solution step '$token' is not r<line>:<k> or c<line>:<k>")
            val (axis, index, amount) = match.destructured
            SavedMove(axis == "r", index.toIntOrNull() ?: -1, amount.toIntOrNull() ?: -1)
        }
        SaveRules.solutionProblem(solution, mordie, side)?.let { throw SaveFileFormatException(name, it) }
        return solution
    }

    private fun parseArrangement(name: String, line: String, expectedSize: Int, what: String): IntArray {
        val tokens = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val values = IntArray(tokens.size) { i ->
            tokens[i].toIntOrNull() ?: throw SaveFileFormatException(name, "$what value '${tokens[i]}' is not an integer")
        }
        if (values.size != expectedSize) {
            throw SaveFileFormatException(name, "expected $expectedSize $what values, found ${values.size}")
        }
        if (!SaveRules.isArrangement(values, expectedSize)) {
            throw SaveFileFormatException(name, "$what values are not a valid arrangement (each tile must appear exactly once)")
        }
        return values
    }
}
