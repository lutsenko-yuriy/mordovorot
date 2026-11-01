package mordie

/**
 * The messages between the browser page and Mordie's Web Worker (GH-218), as plain strings so
 * they cross `postMessage` untouched. A kind letter leads each message, `|` separates fields.
 */
object SearchProtocol {

    enum class Kind(val tag: Char) { START('S'), PROGRESS('P') }

    class Start(val original: IntArray, val ruledOut: Int)

    fun kindOf(message: String): Kind =
        Kind.entries.firstOrNull { message.startsWith("${it.tag}|") } ?: throw IllegalArgumentException("Unknown message: $message")

    fun encodeStart(original: IntArray, ruledOut: Int): String = "S|$ruledOut|${original.joinToString(",")}"

    fun decodeStart(message: String): Start {
        val fields = fields(message, Kind.START, 3)
        return Start(fields[2].split(",").map { it.toInt() }.toIntArray(), fields[1].toInt())
    }

    fun encodeProgress(progress: SearchProgress): String {
        val solution = progress.solution?.joinToString(",") { "${if (it.isRow) 'r' else 'c'}${it.index}:${it.amount}" } ?: "~"
        val proven = if (progress.provenShortest) 1 else 0
        val source = when (progress.source) {
            SolutionSource.FAST_SOLVER -> "f"
            SolutionSource.EXACT_SEARCH -> "e"
            null -> "~"
        }
        return "P|${progress.moves}|${if (progress.found) 1 else 0}|${progress.ruledOut}|$proven|$source|${progress.elapsedMs}|$solution"
    }

    fun decodeProgress(message: String): SearchProgress {
        val fields = fields(message, Kind.PROGRESS, 8)
        val solution = fields[7].takeIf { it != "~" }?.let { moves ->
            if (moves.isEmpty()) emptyList() else moves.split(",").map(::decodeMove)
        }
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
            solution = solution,
            ruledOut = fields[3].toInt(),
            provenShortest = fields[4] == "1",
            source = source,
        )
    }

    private fun decodeMove(text: String): SolutionMove {
        val (index, amount) = text.drop(1).split(":").map { it.toInt() }
        return SolutionMove(isRow = text.first() == 'r', index, amount)
    }

    private fun fields(message: String, kind: Kind, count: Int): List<String> {
        require(message.startsWith("${kind.tag}|")) { "Not a ${kind.name.lowercase()} message: $message" }
        return message.split("|").also { require(it.size == count) { "Malformed message: $message" } }
    }
}
