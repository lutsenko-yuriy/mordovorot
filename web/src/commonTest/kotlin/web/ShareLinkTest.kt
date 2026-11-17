package web

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import storage.BoardLinkFormat
import viewmodel.SharedProgress
import viewmodel.TileLabels

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

    private val board = BoardLinkFormat.encode(4, shifted)

    private fun labelsOf(fragment: String) = assertIs<LinkBoard.Playable>(ShareLink.read(fragment)).labels

    @Test
    fun `a link with labels adds their wire name after the board (GH-267)`() {
        assertEquals("${base}#b=$board&l=pairs_from_0", ShareLink.of(base, 4, shifted, TileLabels.PAIRS_FROM_0))
        assertEquals("${base}#b=$board", ShareLink.of(base, 4, shifted, null))
    }

    @Test
    fun `every scheme reads back from its link`() {
        for (scheme in TileLabels.entries) {
            val side = minOf(4, scheme.maxSide)
            val link = ShareLink.of(base, side, IntArray(side * side) { it }, scheme)

            assertEquals(LinkLabels.Scheme(scheme), labelsOf(link.substringAfter(base)), scheme.name)
        }
    }

    @Test
    fun `a link without labels reads as none`() {
        assertEquals(LinkLabels.None, labelsOf("#b=$board"))
    }

    @Test
    fun `labels may come before the board, and the first labels part wins`() {
        assertEquals(LinkLabels.Scheme(TileLabels.PAIRS), labelsOf("#l=pairs&b=$board"))
        assertEquals(LinkLabels.Scheme(TileLabels.PAIRS), labelsOf("#b=$board&l=pairs&l=letters"))
    }

    @Test
    fun `labels that can't be read are unreadable, never an error`() {
        for (value in listOf("", "colours", "PAIRS", "Pairs", " pairs", "pairs ")) {
            assertEquals(LinkLabels.Unreadable, labelsOf("#b=$board&l=$value"), "l=$value")
        }
    }

    @Test
    fun `a scheme too small for the board is unreadable`() {
        val six = "#b=" + BoardLinkFormat.encode(6, IntArray(36) { it })

        assertEquals(LinkLabels.Unreadable, labelsOf("$six&l=letters"))
        assertEquals(LinkLabels.Scheme(TileLabels.LETTER_PAIRS), labelsOf("$six&l=letter_pairs"))
        assertEquals(LinkLabels.Unreadable, labelsOf("$six&l=cyrillic_letters"))
        assertEquals(LinkLabels.Scheme(TileLabels.CYRILLIC_LETTER_PAIRS), labelsOf("$six&l=cyrillic_letter_pairs"))
        assertEquals(LinkLabels.Unreadable, labelsOf("$six&l=greek_letters"))
        assertEquals(LinkLabels.Scheme(TileLabels.GREEK_LETTER_PAIRS), labelsOf("$six&l=greek_letter_pairs"))
    }

    @Test
    fun `the longest link, a 9x9 board with Cyrillic pairs and progress, still fits a QR code (GH-313)`() {
        val link = ShareLink.of(base, 9, IntArray(81) { 80 - it }, TileLabels.CYRILLIC_LETTER_PAIRS, SharedProgress.Solving(9999999))

        assertTrue(link.contains("&l=cyrillic_letter_pairs"), link)
        web.qr.QrCode.encode(link)
    }

    @Test
    fun `labels never change a board's rejection`() {
        assertEquals(LinkBoard.Rejected("invalid"), ShareLink.read("#b=!!!&l=pairs"))
        assertEquals(LinkBoard.Rejected("unsolvable"), ShareLink.read("#b=" + BoardLinkFormat.encode(3, intArrayOf(1, 0) + IntArray(7) { it + 2 }) + "&l=pairs"))
    }

    @Test
    fun `a labelled link still names its board to a reader of the board alone`() {
        // What an older build does with it: it takes the b= part and ignores the rest (ADR-0005).
        val fragment = ShareLink.of(base, 4, shifted, TileLabels.PAIRS).substringAfter('#')
        val boardOnly = fragment.split('&').first { it.startsWith("b=") }.removePrefix("b=")

        assertContentEquals(shifted, BoardLinkFormat.decode(boardOnly))
    }

    @Test
    fun `a link of labels alone is not a share link`() {
        assertNull(ShareLink.read("#l=pairs"))
    }

    // --- the sender's progress (GH-272) ---

    private fun progressOf(fragment: String) = assertIs<LinkBoard.Playable>(ShareLink.read(fragment)).progress

    @Test
    fun `a link with progress adds a letter and the count after the board`() {
        assertEquals("${base}#b=$board&p=n12", ShareLink.of(base, 4, shifted, progress = SharedProgress.Solving(12)))
        assertEquals("${base}#b=$board&p=s12", ShareLink.of(base, 4, shifted, progress = SharedProgress.Solved(12)))
        assertEquals("${base}#b=$board&p=g12", ShareLink.of(base, 4, shifted, progress = SharedProgress.GaveUp(12)))
        assertEquals("${base}#b=$board&p=g", ShareLink.of(base, 4, shifted, progress = SharedProgress.GaveUp(null)))
        assertEquals("${base}#b=$board&l=pairs&p=n3", ShareLink.of(base, 4, shifted, TileLabels.PAIRS, SharedProgress.Solving(3)))
    }

    @Test
    fun `each state reads back from its link`() {
        for (sent in listOf(SharedProgress.Solving(12), SharedProgress.Solved(1), SharedProgress.GaveUp(9999999), SharedProgress.GaveUp(null))) {
            val link = ShareLink.of(base, 4, shifted, progress = sent)

            assertEquals(LinkProgress.Sent(sent), progressOf(link.substringAfter(base)), "$sent")
        }
    }

    @Test
    fun `a link without progress reads as none`() {
        assertEquals(LinkProgress.None, progressOf("#b=$board"))
    }

    @Test
    fun `progress that can't be read is unreadable, never an error`() {
        for (value in listOf("", "x", "n", "s", "n0", "n012", "n00", "n10000000", "N12", "g-1", "n1.5", "n 1", "12", "gg", "g0")) {
            assertEquals(LinkProgress.Unreadable, progressOf("#b=$board&p=$value"), "p=$value")
        }
    }

    @Test
    fun `progress may come before the board, and the first progress part wins`() {
        assertEquals(LinkProgress.Sent(SharedProgress.Solved(5)), progressOf("#p=s5&b=$board"))
        assertEquals(LinkProgress.Sent(SharedProgress.Solved(5)), progressOf("#b=$board&p=s5&p=n2"))
        assertEquals(LinkProgress.Unreadable, progressOf("#b=$board&p=x&p=n2"))
    }

    @Test
    fun `labels and progress read together`() {
        val read = assertIs<LinkBoard.Playable>(ShareLink.read("#b=$board&l=pairs&p=g3"))

        assertEquals(LinkLabels.Scheme(TileLabels.PAIRS), read.labels)
        assertEquals(LinkProgress.Sent(SharedProgress.GaveUp(3)), read.progress)
    }

    @Test
    fun `progress never changes a board's rejection`() {
        assertEquals(LinkBoard.Rejected("invalid"), ShareLink.read("#b=!!!&p=n3"))
        assertEquals(LinkBoard.Rejected("unsolvable"), ShareLink.read("#b=" + BoardLinkFormat.encode(3, intArrayOf(1, 0) + IntArray(7) { it + 2 }) + "&p=n3"))
    }

    @Test
    fun `a link of progress alone is not a share link`() {
        assertNull(ShareLink.read("#p=n3"))
    }

    @Test
    fun `progress round-trips at both ends of the range`() {
        for (progress in listOf(SharedProgress.Solving(1), SharedProgress.Solved(9999999), SharedProgress.GaveUp(1), SharedProgress.GaveUp(null))) {
            val link = ShareLink.of("https://x.test/", 3, IntArray(9) { it }, progress = progress)

            assertEquals(LinkProgress.Sent(progress), assertIs<LinkBoard.Playable>(ShareLink.read(link.substringAfter('#'))).progress)
        }
    }

    @Test
    fun `a count the link can't carry is left out`() {
        for (progress in listOf(SharedProgress.Solving(0), SharedProgress.Solved(10000000), SharedProgress.GaveUp(0))) {
            assertEquals(ShareLink.of("https://x.test/", 3, IntArray(9) { it }), ShareLink.of("https://x.test/", 3, IntArray(9) { it }, progress = progress))
        }
    }
}
