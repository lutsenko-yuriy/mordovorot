package storage

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.text.CharacterCodingException

/** The packed save format (GH-237, ADR-0004): [MARKER], then base64url without padding of a bit stream.
 *
 *  Header: the side (4 bits), the labels (3 bits: 0 none, 1-6 a known name, 7 an escape followed by
 *  the name), the status (2 bits: no Mordie, searching, found proven, found unproven), the
 *  solution-screen flag and the has-solution flag. Then the move count (varint), the current board
 *  (Lehmer code), and with Mordie its original (Lehmer code), N (varint), R when unproven (varint)
 *  and last N solution steps, each one number for the axis, line and shift. Widths that depend on
 *  the side come after the side, and the long solution is last. */
@OptIn(ExperimentalEncodingApi::class)
internal object PackedSaveFormat {

    const val MARKER = '~'

    /** Frozen: a save stores the position in this list. `viewmodel.TileLabels` can't be imported here. */
    private val KNOWN_LABELS = listOf("numbers", "numbers_from_0", "pairs", "pairs_from_0", "letters", "letter_pairs")
    private const val NO_LABELS = 0
    private const val ESCAPED_LABEL = 7
    private const val NO_MORDIE = 0
    private const val SEARCHING = 1
    private const val PROVEN = 2
    private const val UNPROVEN = 3
    const val MIN_SIDE = 2
    const val MAX_SIDE = 15
    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    /** Assumes [SaveFileCodec.encode]'s checks have passed. An empty solution is written as none. */
    fun encode(squareSide: Int, state: IntArray, moveCount: Int, mordie: SavedMordie?, labels: String?): String {
        val out = BitWriter()
        val solution = mordie?.solution?.takeIf { it.isNotEmpty() }
        val status = when {
            mordie == null -> NO_MORDIE
            !mordie.found -> SEARCHING
            mordie.provenShortest -> PROVEN
            else -> UNPROVEN
        }
        out.put(squareSide, 4)
        out.put(labelCode(labels), 3)
        out.put(status, 2)
        out.put(if (mordie?.solutionScreen == true) 1 else 0, 1)
        out.put(if (solution != null) 1 else 0, 1)
        if (labels != null && labels !in KNOWN_LABELS) {
            val name = labels.encodeToByteArray()
            out.putVarint(name.size)
            name.forEach { out.put(it.toInt() and 0xFF, 8) }
        }
        out.putVarint(moveCount)
        out.putPermutation(state)
        if (mordie != null) {
            out.putPermutation(mordie.original)
            out.putVarint(mordie.moves)
            if (status == UNPROVEN) out.putVarint(mordie.ruledOut)
            solution?.forEach { out.put(stepCode(it, squareSide), bitsFor(stepCodes(squareSide))) }
        }
        return "$MARKER${base64.encode(out.toByteArray())}"
    }

    /** Throws [SaveFileFormatException], naming the save [name], when [text] isn't a valid packed save. */
    fun decode(name: String, text: String): SavedBoard =
        try {
            read(name, text)
        } catch (e: BitFormatException) {
            throw SaveFileFormatException(name, e.message.orEmpty())
        }

    private fun read(name: String, text: String): SavedBoard {
        val bytes = try {
            base64.decode(text.substring(1))
        } catch (e: IllegalArgumentException) {
            throw SaveFileFormatException(name, "the packed data is not valid base64url")
        }
        val reader = BitReader(bytes)
        val side = reader.get(4)
        if (side !in MIN_SIDE..MAX_SIDE) throw SaveFileFormatException(name, "square side must be $MIN_SIDE-$MAX_SIDE, not $side")
        val labelCode = reader.get(3)
        val status = reader.get(2)
        val screen = reader.get(1) == 1
        val hasSolution = reader.get(1) == 1
        flagProblem(status, screen, hasSolution)?.let { throw SaveFileFormatException(name, it) }
        val labels = readLabels(name, reader, labelCode)
        val moveCount = reader.getVarint()
        val state = reader.getPermutation(side * side, "the board")
        val mordie = if (status == NO_MORDIE) null else readMordie(name, reader, side, status, screen, hasSolution)
        if (!reader.atEnd()) {
            val reason = if (reader.bitsLeft() >= 8) "the packed data has extra bytes at the end" else "the packed data's padding bits are not zero"
            throw SaveFileFormatException(name, reason)
        }
        return SavedBoard(side, state, moveCount, mordie, labels)
    }

    private fun flagProblem(status: Int, screen: Boolean, hasSolution: Boolean): String? = when {
        status == NO_MORDIE && (screen || hasSolution) -> "Mordie's flags without Mordie data"
        status == SEARCHING && hasSolution -> "Mordie's solution on a save where it is still searching"
        screen && !hasSolution -> "the solution-screen flag needs Mordie's solution"
        else -> null
    }

    private fun labelCode(labels: String?) = when {
        labels == null -> NO_LABELS
        labels in KNOWN_LABELS -> KNOWN_LABELS.indexOf(labels) + 1
        else -> ESCAPED_LABEL
    }

    private fun readLabels(name: String, reader: BitReader, code: Int): String? {
        if (code == NO_LABELS) return null
        if (code != ESCAPED_LABEL) return KNOWN_LABELS[code - 1]
        val length = reader.getVarint()
        if (length > reader.bitsLeft() / 8) throw BitFormatException("the packed data ends early")
        val label = try {
            ByteArray(length) { reader.get(8).toByte() }.decodeToString(throwOnInvalidSequence = true)
        } catch (e: CharacterCodingException) {
            throw SaveFileFormatException(name, "the label is not valid UTF-8")
        }
        if (label.isEmpty() || label.any { it.isWhitespace() }) throw SaveFileFormatException(name, "the label must be one word")
        return label
    }

    private fun readMordie(name: String, reader: BitReader, side: Int, status: Int, screen: Boolean, hasSolution: Boolean): SavedMordie {
        val original = reader.getPermutation(side * side, "Mordie's original")
        val moves = reader.getVarint()
        val found = status != SEARCHING
        val provenShortest = status != UNPROVEN
        val ruledOut = if (provenShortest) 0 else reader.getVarint()
        if (!provenShortest) SaveRules.ruledOutProblem(found, moves, ruledOut)?.let { throw SaveFileFormatException(name, it) }
        if (hasSolution && moves == 0) throw SaveFileFormatException(name, "the has-solution flag needs at least one step")
        // Each step is at least 2 bits, so a count the data cannot hold is cut off before a list is made for it.
        if (hasSolution && moves > reader.bitsLeft() / bitsFor(stepCodes(side))) throw BitFormatException("the packed data ends early")
        val solution = if (hasSolution) List(moves) { readStep(name, reader, side) } else null
        val mordie = SavedMordie(original, moves, found, solution, screen, provenShortest, ruledOut)
        solution?.let { SaveRules.solutionProblem(it, mordie, side) }?.let { throw SaveFileFormatException(name, it) }
        return mordie
    }

    private fun readStep(name: String, reader: BitReader, side: Int): SavedMove {
        val code = reader.get(bitsFor(stepCodes(side)))
        if (code >= stepCodes(side)) throw SaveFileFormatException(name, "a solution step is out of range")
        val perAxis = side * (side - 1)
        val inAxis = code % perAxis
        return SavedMove(isRow = code < perAxis, index = inAxis / (side - 1), amount = inAxis % (side - 1) + 1)
    }

    /** The axis, line and shift of a step as one number: rows first, then columns, 0 until 2 * side * (side - 1). */
    private fun stepCode(step: SavedMove, side: Int) =
        (if (step.isRow) 0 else side * (side - 1)) + step.index * (side - 1) + step.amount - 1

    private fun stepCodes(side: Int) = 2 * side * (side - 1)
}
