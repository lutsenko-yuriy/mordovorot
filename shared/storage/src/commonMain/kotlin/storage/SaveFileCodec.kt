package storage

/** The save codec, the one public seam over the save formats (GH-237): [encode] checks and writes
 *  a save, [decode] reads any format (today the text format, see [TextSaveFormat]). */
object SaveFileCodec {

    /** Throws [IllegalArgumentException] for a state, move count, Mordie data or labels [SaveRepository.save] rejects. */
    fun encode(squareSide: Int, state: IntArray, moveCount: Int, mordie: SavedMordie?, labels: String? = null): String {
        require(squareSide > 0) { "Square side must be positive" }
        require(state.size == squareSide * squareSide) { "Expected ${squareSide * squareSide} values, got ${state.size}" }
        require(SaveRules.isArrangement(state, state.size)) { "State must contain each of ${state.size} tile values exactly once" }
        require(moveCount >= 0) { "Move count must not be negative" }
        if (mordie != null) {
            require(SaveRules.isArrangement(mordie.original, state.size)) { "Mordie's original must be a valid ${state.size}-tile arrangement" }
            require(mordie.moves >= 0) { "Mordie's moves must not be negative" }
            mordie.solution?.let { solution -> SaveRules.solutionProblem(solution, mordie, squareSide)?.let { throw IllegalArgumentException(it) } }
            require(!mordie.solutionScreen || !mordie.solution.isNullOrEmpty()) { "A solution-screen save needs Mordie's solution" }
            require(mordie.provenShortest || SaveRules.ruledOutProblem(mordie.found, mordie.moves, mordie.ruledOut) == null) {
                "An unproven answer needs a found save and a ruled out length of 0 to N-2"
            }
        }
        require(labels == null || (labels.isNotEmpty() && labels.none { it.isWhitespace() })) { "Labels must be one word" }
        return TextSaveFormat.encode(squareSide, state, moveCount, mordie, labels)
    }

    /** Throws [SaveFileFormatException], naming the save [name], when [text] isn't a valid save. */
    fun decode(name: String, text: String): SavedBoard = TextSaveFormat.decode(name, text)
}
