package view.tui

/** Greedy word-wrap: packs whole words onto a line up to [maxWidth], breaking to a new line
 *  rather than truncating - shared by dialog messages and the Mordie line (GH-83), either of
 *  which can be long enough that cutting it off would hide half a sentence. A single word
 *  longer than [maxWidth] is hard-broken across as many lines as it needs (not truncated - the
 *  same "don't hide content" reasoning applies to one long word, e.g. a save name in the
 *  overwrite warning, as to the message overall). */
internal fun wordWrap(text: String, maxWidth: Int): List<String> {
    val lines = mutableListOf<String>()
    var current = StringBuilder()
    for (word in text.split(" ")) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        when {
            candidate.length <= maxWidth -> current = StringBuilder(candidate)
            word.length > maxWidth -> {
                if (current.isNotEmpty()) lines += current.toString()
                lines += word.chunked(maxWidth)
                current = StringBuilder()
            }
            else -> {
                lines += current.toString()
                current = StringBuilder(word)
            }
        }
    }
    if (current.isNotEmpty() || lines.isEmpty()) lines += current.toString()
    return lines
}
