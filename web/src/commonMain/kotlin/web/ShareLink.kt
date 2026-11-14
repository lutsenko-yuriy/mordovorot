package web

import board_model.BoardSize
import board_model.Solvability
import kotlin.math.sqrt
import storage.BoardLinkFormat
import storage.BoardLinkFormatException
import viewmodel.TileLabels

/** The tile labels a share link carries (GH-267): [None] without an `l=` part, [Unreadable] when it names
 *  none the page knows for the board, which the page ignores without a word. */
sealed interface LinkLabels {
    data object None : LinkLabels
    data object Unreadable : LinkLabels
    data class Scheme(val labels: TileLabels) : LinkLabels
}

/** A board named by a share link's fragment (GH-243). */
sealed interface LinkBoard {
    /** A board the page can start, with the [labels] its link carries. */
    class Playable(val tiles: IntArray, val labels: LinkLabels = LinkLabels.None) : LinkBoard {
        val side: Int get() = sqrt(tiles.size.toDouble()).toInt()
    }

    /** A board it can't, for [reason]: `invalid`, `unsupported_size` or `unsolvable` (the `shared_board_opened`
     *  values); [side] is the board's, for `unsupported_size`. */
    data class Rejected(val reason: String, val side: Int? = null) : LinkBoard
}

/** The share link (GH-243, ADR-0005): the page's address and `#b=` and the board; the fragment, so the server
 *  never sees it. `&l=` adds the tile labels' wire name (GH-267). Other `&key=value` parts are ignored, which
 *  leaves room for more later. */
object ShareLink {
    private const val BOARD_KEY = "b="
    private const val LABELS_KEY = "l="

    /** The link to [arrangement] on a [side] x [side] board, from the page address [base] (origin and path),
     *  carrying [labels] when given. */
    fun of(base: String, side: Int, arrangement: IntArray, labels: TileLabels? = null): String =
        "$base#$BOARD_KEY${BoardLinkFormat.encode(side, arrangement)}" + (labels?.let { "&$LABELS_KEY${it.wireName}" } ?: "")

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
            else -> LinkBoard.Playable(tiles, labelsOf(parts.firstOrNull { it.startsWith(LABELS_KEY) }?.removePrefix(LABELS_KEY), side))
        }
    }

    private fun labelsOf(name: String?, side: Int): LinkLabels {
        if (name == null) return LinkLabels.None
        val labels = TileLabels.fromWireName(name)
        return if (labels != null && labels.fits(side)) LinkLabels.Scheme(labels) else LinkLabels.Unreadable
    }
}
