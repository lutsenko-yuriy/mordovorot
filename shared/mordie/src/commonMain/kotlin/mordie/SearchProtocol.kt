package mordie

/**
 * The messages between the browser page and Mordie's Web Worker (GH-218), as plain strings so
 * they cross `postMessage` untouched. A kind letter leads each message, `|` separates fields.
 */
object SearchProtocol {

    enum class Kind(val tag: Char) { START('S'), PROGRESS('P') }

    class Start(val original: IntArray, val ruledOut: Int, val best: List<SolutionMove>? = null)

    fun kindOf(message: String): Kind =
        Kind.entries.firstOrNull { message.startsWith("${it.tag}|") } ?: throw IllegalArgumentException("Unknown message: $message")

    fun encodeStart(original: IntArray, ruledOut: Int, best: List<SolutionMove>? = null): String =
        "S|$ruledOut|${original.joinToString(",")}|${encodeMoves(best)}"

    fun decodeStart(message: String): Start {
        // A page and a worker from different deploys may send three fields, with no kept solution.
        val fields = fields(message, Kind.START, 3..4)
        return Start(fields[2].split(",").map { it.toInt() }.toIntArray(), fields[1].toInt(), fields.getOrNull(3)?.let(::decodeMoves))
    }

    fun encodeProgress(progress: SearchProgress): String {
        val proven = if (progress.provenShortest) 1 else 0
        val source = when (progress.source) {
            SolutionSource.FAST_SOLVER -> "f"
            SolutionSource.EXACT_SEARCH -> "e"
            null -> "~"
        }
        return "P|${progress.moves}|${if (progress.found) 1 else 0}|${progress.ruledOut}|$proven|$source|${progress.elapsedMs}|${encodeMoves(progress.solution)}"
    }

    fun decodeProgress(message: String): SearchProgress {
        val fields = fields(message, Kind.PROGRESS, 8)
        val source = when (fields[5]) {
            "f" -> SolutionSource.FAST_SOLVER
            "e" -> SolutionSource.EXACT_SEARCH
            "~" -> null
            else -> throw IllegalArgumentException("Malformed message: $message")
        }
        return SearchProgress(
            moves = fields[1].toInt(),
            found = fields[2] == "1",
            elapsedMs = fields[6].toLong(),
            solution = decodeMoves(fields[7]),
            ruledOut = fields[3].toInt(),
            provenShortest = fields[4] == "1",
            source = source,
        )
    }

    /** The moves as `r<i>:<k>`/`c<i>:<k>` joined by commas; `~` for none. */
    private fun encodeMoves(moves: List<SolutionMove>?): String =
        moves?.joinToString(",") { "${if (it.isRow) 'r' else 'c'}${it.index}:${it.amount}" } ?: "~"

    private fun decodeMoves(text: String): List<SolutionMove>? =
        text.takeIf { it != "~" }?.let { if (it.isEmpty()) emptyList() else it.split(",").map(::decodeMove) }

    private fun decodeMove(text: String): SolutionMove {
        val (index, amount) = text.drop(1).split(":").map { it.toInt() }
        return SolutionMove(isRow = text.first() == 'r', index, amount)
    }

    private fun fields(message: String, kind: Kind, count: Int): List<String> = fields(message, kind, count..count)

    private fun fields(message: String, kind: Kind, counts: IntRange): List<String> {
        require(message.startsWith("${kind.tag}|")) { "Not a ${kind.name.lowercase()} message: $message" }
        return message.split("|").also { require(it.size in counts) { "Malformed message: $message" } }
    }
}
