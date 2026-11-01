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
        return "P|${progress.moves}|${if (progress.found) 1 else 0}|${progress.elapsedMs}|$solution"
    }

    fun decodeProgress(message: String): SearchProgress {
        val fields = fields(message, Kind.PROGRESS, 5)
        val solution = fields[4].takeIf { it != "~" }?.let { moves ->
            if (moves.isEmpty()) emptyList() else moves.split(",").map(::decodeMove)
        }
        return SearchProgress(fields[1].toInt(), fields[2] == "1", fields[3].toLong(), solution)
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
