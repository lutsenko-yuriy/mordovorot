package web

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import storage.BoardLinkFormat

class ShareLinkTest {

    private val base = "https://example.org/mordovorot/"
    private val shifted = intArrayOf(3, 0, 1, 2) + IntArray(12) { it + 4 }

    @Test
    fun `a link is the address, a hash and the board`() {
        assertEquals("${base}#b=${BoardLinkFormat.encode(4, shifted)}", ShareLink.of(base, 4, shifted))
    }

    @Test
    fun `a link reads back as the board`() {
        val link = ShareLink.of(base, 4, shifted)

        val read = assertIs<LinkBoard.Playable>(ShareLink.read(link.substringAfter(base)))
        assertContentEquals(shifted, read.tiles)
    }

    @Test
    fun `the hash is optional`() {
        val fragment = "b=" + BoardLinkFormat.encode(4, shifted)

        assertIs<LinkBoard.Playable>(ShareLink.read(fragment))
        assertIs<LinkBoard.Playable>(ShareLink.read("#$fragment"))
    }

    @Test
    fun `other parts of the fragment are ignored`() {
        val board = BoardLinkFormat.encode(4, shifted)

        assertIs<LinkBoard.Playable>(ShareLink.read("#m=abc&b=$board&x=1"))
    }

    @Test
    fun `a fragment without a board is not a share link`() {
        assertNull(ShareLink.read(""))
        assertNull(ShareLink.read("#"))
        assertNull(ShareLink.read("#section"))
        assertNull(ShareLink.read("#m=abc"))
    }

    @Test
    fun `a board that does not decode is invalid`() {
        assertEquals(LinkBoard.Rejected("invalid"), ShareLink.read("#b="))
        assertEquals(LinkBoard.Rejected("invalid"), ShareLink.read("#b=!!!"))
        assertEquals(LinkBoard.Rejected("invalid"), ShareLink.read("#b=AAAA"))
    }

    @Test
    fun `a side the page does not play is unsupported`() {
        assertEquals(LinkBoard.Rejected("unsupported_size", side = 2), ShareLink.read("#b=" + BoardLinkFormat.encode(2, intArrayOf(0, 1, 2, 3))))
        assertEquals(LinkBoard.Rejected("unsupported_size", side = 10), ShareLink.read("#b=" + BoardLinkFormat.encode(10, IntArray(100) { it })))
    }

    @Test
    fun `every playable side from 3 to 9 reads`() {
        for (side in 3..9) assertIs<LinkBoard.Playable>(ShareLink.read("#b=" + BoardLinkFormat.encode(side, IntArray(side * side) { it })), "side $side")
    }

    @Test
    fun `an unsolvable board is rejected`() {
        val swapped = intArrayOf(1, 0) + IntArray(7) { it + 2 } // a 3x3 with two tiles swapped

        assertEquals(LinkBoard.Rejected("unsolvable"), ShareLink.read("#b=" + BoardLinkFormat.encode(3, swapped)))
    }

    @Test
    fun `an even side never makes a board unsolvable`() {
        val swapped = intArrayOf(1, 0) + IntArray(14) { it + 2 }

        assertIs<LinkBoard.Playable>(ShareLink.read("#b=" + BoardLinkFormat.encode(4, swapped)))
    }
}
