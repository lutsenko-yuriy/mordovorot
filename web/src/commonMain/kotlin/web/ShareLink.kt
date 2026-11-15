package web

import board_model.BoardSize
import board_model.Solvability
import kotlin.math.sqrt
import storage.BoardLinkFormat
import storage.BoardLinkFormatException
import viewmodel.SharedProgress
import viewmodel.TileLabels

/** The tile labels a share link carries (GH-267): [None] without an `l=` part, [Unreadable] when it names
 *  none the page knows for the board, which the page ignores without a word. */
sealed interface LinkLabels {
    data object None : LinkLabels
    data object Unreadable : LinkLabels
    data class Scheme(val labels: TileLabels) : LinkLabels
}

/** The sender's progress a share link carries (GH-272): [None] without a `p=` part, [Unreadable] when it isn't one
 *  the page knows, which the page ignores without a word. */
sealed interface LinkProgress {
    data object None : LinkProgress
    data object Unreadable : LinkProgress
    data class Sent(val progress: SharedProgress) : LinkProgress
}

/** A board named by a share link's fragment (GH-243). */
sealed interface LinkBoard {
    /** A board the page can start, with the [labels] and [progress] its link carries. */
    class Playable(val tiles: IntArray, val labels: LinkLabels = LinkLabels.None, val progress: LinkProgress = LinkProgress.None) : LinkBoard {
        val side: Int get() = sqrt(tiles.size.toDouble()).toInt()
    }

    /** A board it can't, for [reason]: `invalid`, `unsupported_size` or `unsolvable` (the `shared_board_opened`
     *  values); [side] is the board's, for `unsupported_size`. */
    data class Rejected(val reason: String, val side: Int? = null) : LinkBoard
}

/** The share link (GH-243, ADR-0005): the page's address and `#b=` and the board; the fragment, so the server
 *  never sees it. `&l=` adds the tile labels' wire name (GH-267), `&p=` the sender's progress: a letter (`n` not
 *  solved yet, `s` solved, `g` gave up) and the move count, 1 to 9999999; `g` alone is a count not known (GH-272).
 *  Other `&key=value` parts are ignored, which leaves room for more later. */
object ShareLink {
    private const val BOARD_KEY = "b="
    private const val LABELS_KEY = "l="
    private const val PROGRESS_KEY = "p="
    private const val MAX_MOVES = 9999999

    /** The link to [arrangement] on a [side] x [side] board, from the page address [base] (origin and path),
     *  carrying [labels] and [progress] when given; a count outside 1 to 9999999 is left out, as a link can't carry it. */
    fun of(base: String, side: Int, arrangement: IntArray, labels: TileLabels? = null, progress: SharedProgress? = null): String =
        "$base#$BOARD_KEY${BoardLinkFormat.encode(side, arrangement)}" +
            (labels?.let { "&$LABELS_KEY${it.wireName}" } ?: "") +
            (progress?.takeIf(::carries)?.let { "&$PROGRESS_KEY${progressText(it)}" } ?: "")

    /** Whether a link can carry [progress]: its count, when known, is 1 to 9999999. */
    fun carries(progress: SharedProgress): Boolean = progress.moves == null || progress.moves in 1..MAX_MOVES

    /** What [fragment] (with or without its `#`) names: `null` when it names no board, so it isn't a share link. */
    fun read(fragment: String): LinkBoard? {
        val parts = fragment.removePrefix("#").split('&')
        val text = parts.firstOrNull { it.startsWith(BOARD_KEY) }?.removePrefix(BOARD_KEY) ?: return null
        val tiles = try {
            BoardLinkFormat.decode(text)
        } catch (e: BoardLinkFormatException) {
            return LinkBoard.Rejected("invalid")
        }
        val side = sqrt(tiles.size.toDouble()).toInt()
        return when {
            side !in BoardSize.MIN..BoardSize.VERY_LARGE.last -> LinkBoard.Rejected("unsupported_size", side)
            !Solvability.isSolvable(tiles) -> LinkBoard.Rejected("unsolvable")
            else -> LinkBoard.Playable(
                tiles,
                labelsOf(parts.firstOrNull { it.startsWith(LABELS_KEY) }?.removePrefix(LABELS_KEY), side),
                progressOf(parts.firstOrNull { it.startsWith(PROGRESS_KEY) }?.removePrefix(PROGRESS_KEY)),
            )
        }
    }

    private fun progressText(progress: SharedProgress) = when (progress) {
        is SharedProgress.Solving -> "n${progress.moves}"
        is SharedProgress.Solved -> "s${progress.moves}"
        is SharedProgress.GaveUp -> "g${progress.moves ?: ""}"
    }

    private fun progressOf(text: String?): LinkProgress {
        if (text == null) return LinkProgress.None
        if (text == "g") return LinkProgress.Sent(SharedProgress.GaveUp(null))
        val digits = text.drop(1)
        val moves = digits.takeIf { it.length in 1..7 && it.all { c -> c in '0'..'9' } && it[0] != '0' }?.toInt()
        if (moves == null || moves > MAX_MOVES) return LinkProgress.Unreadable
        return when (text[0]) {
            'n' -> LinkProgress.Sent(SharedProgress.Solving(moves))
            's' -> LinkProgress.Sent(SharedProgress.Solved(moves))
            'g' -> LinkProgress.Sent(SharedProgress.GaveUp(moves))
            else -> LinkProgress.Unreadable
        }
    }

    private fun labelsOf(name: String?, side: Int): LinkLabels {
        if (name == null) return LinkLabels.None
        val labels = TileLabels.fromWireName(name)
        return if (labels != null && labels.fits(side)) LinkLabels.Scheme(labels) else LinkLabels.Unreadable
    }
}
