package storage

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** What a share link's board can't be read as (GH-243). */
class BoardLinkFormatException(message: String) : RuntimeException(message)

/** A board in a share link (GH-243, ADR-0005): base64url without padding of the side in 4 bits, then the
 *  arrangement as a Lehmer code (the packed save's board code, GH-237). Links outlive builds, so this layout
 *  is frozen apart from the save formats. It knows nothing of which sizes can be played. */
@OptIn(ExperimentalEncodingApi::class)
object BoardLinkFormat {

    private const val SIDE_BITS = 4
    private const val MIN_SIDE = 2
    private val base64 = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    /** [arrangement] of a [side] x [side] board, as the text after `b=`. */
    fun encode(side: Int, arrangement: IntArray): String {
        require(side in MIN_SIDE until (1 shl SIDE_BITS)) { "a link carries sides 2 to 15, got $side" }
        require(arrangement.size == side * side) { "a $side x $side board has ${side * side} tiles, got ${arrangement.size}" }
        val out = BitWriter()
        out.put(side, SIDE_BITS)
        out.putPermutation(arrangement)
        return base64.encode(out.toByteArray())
    }

    /** The arrangement [text] carries; its side is the square root of the size. */
    fun decode(text: String): IntArray {
        val bytes = try {
            base64.decode(text)
        } catch (e: IllegalArgumentException) {
            throw BoardLinkFormatException("the link's board is not valid base64url")
        }
        val bits = BitReader(bytes)
        try {
            val side = bits.get(SIDE_BITS)
            if (side < MIN_SIDE) throw BoardLinkFormatException("the link's board has a side of $side")
            val arrangement = bits.getPermutation(side * side, "the link's board")
            if (!bits.atEnd()) throw BoardLinkFormatException("the link's board has data after it")
            // A decoder that skips the padding bits would let many texts name one board.
            if (base64.encode(bytes) != text) throw BoardLinkFormatException("the link's board is not in its canonical form")
            return arrangement
        } catch (e: BitFormatException) {
            throw BoardLinkFormatException(e.message ?: "the link's board is not valid")
        }
    }
}
