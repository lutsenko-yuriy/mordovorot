package web

import board_model.BoardSize
import board_model.Solvability
import kotlin.math.sqrt
import storage.BoardLinkFormat
import storage.BoardLinkFormatException

/** A board named by a share link's fragment (GH-243). */
sealed interface LinkBoard {
    /** A board the page can start. */
    class Playable(val tiles: IntArray) : LinkBoard

    /** A board it can't, for [reason]: `invalid`, `unsupported_size` or `unsolvable` (the `shared_board_opened` values). */
    data class Rejected(val reason: String) : LinkBoard
}

/** The share link (GH-243, ADR-0005): the page's address and `#b=` and the board; the fragment, so the server
 *  never sees it. Other `&key=value` parts are ignored, which leaves room for more later. */
object ShareLink {
    private const val BOARD_KEY = "b="

    /** The link to [arrangement] on a [side] x [side] board, from the page address [base] (origin and path). */
    fun of(base: String, side: Int, arrangement: IntArray): String = "$base#$BOARD_KEY${BoardLinkFormat.encode(side, arrangement)}"

    /** What [fragment] (with or without its `#`) names: `null` when it names no board, so it isn't a share link. */
    fun read(fragment: String): LinkBoard? {
        val text = fragment.removePrefix("#").split('&').firstOrNull { it.startsWith(BOARD_KEY) }?.removePrefix(BOARD_KEY) ?: return null
        val tiles = try {
            BoardLinkFormat.decode(text)
        } catch (e: BoardLinkFormatException) {
            return LinkBoard.Rejected("invalid")
        }
        val side = sqrt(tiles.size.toDouble()).toInt()
        return when {
            side !in BoardSize.MIN..BoardSize.VERY_LARGE.last -> LinkBoard.Rejected("unsupported_size")
            !Solvability.isSolvable(tiles) -> LinkBoard.Rejected("unsolvable")
            else -> LinkBoard.Playable(tiles)
        }
    }
}
